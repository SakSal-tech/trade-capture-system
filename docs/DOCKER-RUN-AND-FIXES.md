# Docker run notes, errors and fixes

## Commands I used

Run and build

```bash
# Build and run both services (foreground)
docker compose up --build

# Build and run in detached mode
docker compose up --build -d

# Stop and remove images, volumes, and orphans
docker compose down --rmi all --volumes --remove-orphans

# Rebuild a single service with no cache
docker compose build --no-cache frontend

docker compose build --progress=plain frontend

docker compose build frontend

- `docker compose up --build` — Builds images and starts all services defined in compose in the foreground. You will see the combined build and runtime logs. It was taking forever and I  pressed Ctrl+C while it runs, Docker will stop the containers (that's why I saw exit 130).

docker compose build backend

```

Inspect and logs

```bash
docker compose ps --all
docker compose ps

docker ps -a

docker compose logs --tail=200

docker compose logs --tail=200 frontend

docker compose logs --tail=200 backend

docker compose logs --follow frontend

docker compose logs --follow backend

docker logs <container-id-or-name>

docker inspect --format '{{.Name}} {{.State.Status}} Exit={{.State.ExitCode}} OOM={{.State.OOMKilled}}' <container-id-or-name>

docker inspect --format '{{.Name}} {{.State.Status}} Exit={{.State.ExitCode}} OOM={{.State.OOMKilled}} Error="{{.State.Error}}" FinishedAt={{.State.FinishedAt}}' $(docker compose ps -q)

docker stats

What these inspection commands do:

- `docker compose ps` / `docker compose ps --all` — Shows services defined by compose and their state (Up, Exited, etc.). Use `--all` to include stopped containers.
- `docker ps -a` — Lists all containers on the host (not just compose-managed). Useful to find exited container IDs.
- `docker compose logs --tail=200` — Shows the last 200 lines of combined logs for all services. Add `frontend` or `backend` to limit output.
- `docker compose logs --follow` — Continues streaming logs so I can watch events live.
- `docker logs <id>` — Shows logs for a single container ID.
- `docker inspect --format ...` — Shows low-level container state fields; the template prints exit code and whether the container was OOM killed.
- `docker stats` — Live resource usage (CPU/memory) per container. Useful to detect OOM or high memory usage.
- `docker volume ls` / `docker volume inspect` — Lists and inspects Docker volumes (where container data like H2 files can be kept).

docker volume ls

docker volume inspect <volume-name>
```

Why run a shell in a container:

- This opens a short-lived shell inside the backend container image so I can inspect files and permissions as the container user. It helps diagnose file ownership and missing files.
  Quick in-container checks

```bash
# Run an interactive shell (diagnostic)
docker compose run --rm backend sh -c "ls -la /app && id && whoami"
```

What these endpoint check:

- `curl http://localhost:8080/actuator/health` — Calls the Spring Boot Actuator health endpoint. If it returns 200 and JSON status UP your backend is running and ready.
- `curl http://localhost:3000` — Requests the frontend root page served by nginx. If I see HTML output, nginx is serving the built static site.
  Host-level checks (app endpoints)

```bash
curl -v http://localhost:8080/actuator/health
curl -sS http://localhost:8080/actuator/health | jq .
curl -sS http://localhost:3000 | head -n 20

```

Package manager / build commands seen inside Dockerfile or suggested

Simple notes about package managers and lockfiles:

- `npm` and `pnpm` are JavaScript package managers. `pnpm` is used in this repo (there's a `pnpm-lock.yaml`).
- `corepack` is a Node tool that can install and run `pnpm` for me inside the image; we enabled it in the Dockerfile so container builds use the same package manager as your local machine.
- A `lockfile` (like `pnpm-lock.yaml`) pins exact dependency versions so builds are reproducible.

```bash
# frontend
npm install
npm run build
pnpm install --frozen-lockfile
pnpm build
pnpm lint
```

---

## Errors encountered, root causes, solutions and impact

### Problem: COPY . . failed with "cannot replace to directory ... node_modules/@eslint/eslintrc"

- Root cause: the Docker build context included the host `node_modules` and other build artifacts. When Docker tried to COPY the project into the image the host files conflicted with container filesystem/cache mounts.

- Solution: add `.dockerignore` files to exclude `node_modules`, `dist`, `target`, and other large or host-only folders from the build context. This prevents copying host-side `node_modules` into the image and avoids the collision.

  Files added:

  - `.dockerignore` (repo root)
  - `frontend/.dockerignore`

- Impact: the frontend build step could proceed; build context size reduced significantly (faster builds) and the specific COPY error was resolved.

---

### Problem: frontend build failed with `sh: pnpm: not found` when running `npm run build` inside the image

- Root cause: the frontend's build script (lint + vite) uses `pnpm`. The base image (`node:18-alpine`) had npm but not pnpm installed, so the `npm run build` step (which invoked pnpm) failed.

- Solution: update `frontend/Dockerfile` to enable Corepack and use `pnpm` inside the image. Copy `pnpm-lock.yaml` and run `pnpm install --frozen-lockfile`, then `pnpm build`.

  Key Dockerfile changes:

  - `RUN corepack enable && corepack prepare pnpm@latest --activate`
  - `RUN pnpm install --frozen-lockfile`
  - `RUN pnpm build`

- Impact: matches local developer tooling (pnpm + lockfile), ensures deterministic installs inside the container, and fixed the pnpm-not-found error so the frontend image can be built reliably.

---

### Problem: H2 error during backend startup — "Error while creating file \"/app/data\"" / AccessDeniedException

- Root cause: the Spring Boot backend (embedded H2) attempted to create its data directory `/app/data` but the JVM process (`appuser`) did not have write permission on that path.

- Solution (multi-step):

  1. Ensured Dockerfile creates the `/app/data` directory and sets ownership to the non-root user `appuser` during image build.
     - `RUN mkdir -p /app/data && chown -R appuser:appuser /app`
  2. Used a Docker named volume in `docker-compose.yml` and mount it at `/app/data` instead of mounting a host directory. Named volumes are managed by Docker and avoid Windows/WSL host permission issues.
     - Compose addition:
       ```yaml
       volumes:
         backend-data:
       services:
         backend:
           volumes:
             - backend-data:/app/data
       ```

- Impact: the backend could create H2 storage files and complete JPA/Hibernate initialization. This removed the failing startup and allowed the application to progress to serving Actuator health endpoints.

---

### Problem: Containers exited with codes 137 / 130 during iteration

- Root cause: exit 137 indicates a SIGKILL (often an OOM kill). Exit 130 is SIGINT (interruption by Ctrl+C). Both appeared during iterations while I tested builds and stopped/restarted services.

- Solution / mitigation:

  - For presumed OOM (137): increase Docker Desktop memory (recommend >= 4GB) or reduce concurrent container memory usage; monitor with `docker stats` during startup.
  - For Ctrl+C interruptions (130): re-run with detached mode `-d` or avoid interrupting the foreground process while images are building.

- Impact: After ensuring resources and not interrupting, containers remained up. No functional change was necessary to application code.

---

## Final verification commands (what I ran to confirm success)

```bash
# Recreate volumes, images, containers from scratch
docker compose down --rmi all --volumes --remove-orphans

# Fresh build and run
docker compose up --build

# Or detached mode
docker compose up --build -d

# Verify containers and health
docker compose ps
curl -sS http://localhost:8080/actuator/health | jq .
curl -sS http://localhost:3000 | head -n 20
```

## Learned

- Using named volumes for container-managed data storage (we added `backend-data`) rather than host-mounted folders when developing on Windows/WSL to avoid permission issues.
- Adding `.dockerignore` to every build context (repo root, frontend, backend) to keep build contexts small and avoid copying host-only artifacts.
- Aligning build/runtime JDK versions (we switched to Java 21 images for build and runtime) to avoid class-file or runtime incompatibilities.
- Using `pnpm` inside the node image when the repository uses pnpm; enable Corepack in the Dockerfile for a small, consistent change.

## Simple explanations of key concepts (plain English)

- Build context

  - Term: build context
  - Simple: The set of files Docker looks at when run `docker build` or `docker compose build`.

- .dockerignore

  - Term: `.dockerignore`
  - A file that tells Docker which files and folders NOT to send into the build context.
  - Large folders like `node_modules` or build outputs can slow or break the build. `.dockerignore` keeps the build context clean and small.

- Named volume (docker volume)

  - Term: Docker named volume (e.g. `backend-data`)
  - Simple: A storage area managed by Docker that lives outside the container's writable layer and persists data between container restarts.
  - Analogy: A labeled storage box in the datastore that Docker controls, so Windows permission quirks on your host don't block the container from writing files.

- chown and non-root user

  - Term: `chown`, `USER appuser`, non-root
  - Simple: `chown` changes file ownership inside the image. Running the app as a non-root user (`appuser`) is safer and a recommended Docker practice.
  - Why it matters: If the data directory is owned by root or the wrong user, the running Java process won't be able to write H2 files and will fail with AccessDeniedException.

- Healthcheck and Actuator

  - Term: `HEALTHCHECK`, Spring Boot Actuator `/actuator/health`
  - Simple: A healthcheck is a small command Docker runs to confirm the service is healthy. Spring Boot Actuator provides a ready-made health endpoint. If the healthcheck fails, Docker marks the container unhealthy.
  - Why it matters: Healthchecks let Docker and `depends_on` logic decide if services are ready for other services to talk to them.

- The frontedn exited was it OOM? Exit 137, Exit 130

  - Term: OOM (Out Of Memory), exit codes 137 and 130
  - Simple: Exit 137 typically means the kernel killed the process because it used too much memory (OOM). Exit 130 means the process was interrupted by Ctrl+C (SIGINT).
  - How to react: Increase Docker memory limits or reduce memory usage if I see OOM, I need to avoid interrupting a foreground compose run with Ctrl+C when a build is running.

- Corepack, pnpm and lockfile

  - Term: Corepack, `pnpm`, `pnpm-lock.yaml`
  - Simple: `pnpm` is the package manager used here. `corepack` is a helper that installs/activates `pnpm` inside the image. The lockfile pins exact dependency versions so builds are repeatable. Corepack is a tool that comes with Node.js starting from Node 16+. It manages package managers like: pnpm

## Error snippets (raw lines seen during debugging)

COPY/context collision (frontend build):

```text
> ERROR [frontend build 5/6] COPY . .:
target frontend: failed to solve: cannot replace to directory /var/lib/docker/buildkit/containerd-overlayfs/cachemounts/buildkit.../app/node_modules/@eslint/eslintrc with file
```

pnpm missing during build:

```text
> [frontend build 6/6] RUN npm run build:
sh: pnpm: not found
```

H2 permission error during backend startup:

```text
org.h2.jdbc.JdbcSQLNonTransientException: Error while creating file "/app/data" [90062-214]
Caused by: java.nio.file.AccessDeniedException: /app/data
```

Container exit codes observed while iterating:

```text
CONTAINER ... Exited (137)   # SIGKILL (likely OOM)
CONTAINER ... Exited (130)   # Interrupted (SIGINT / Ctrl+C)
```

---

## File changes — before / after (high level snippets)

Below are the concise before/after snippets for files I edited:

### frontend/Dockerfile

Before (used npm, no pnpm):

```dockerfile
COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build
```

After (use Corepack + pnpm, copy lockfile):

```dockerfile
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && corepack prepare pnpm@latest --activate
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build
```

### backend/Dockerfile

Before (Java 17 images, no data dir ownership):

```dockerfile
FROM maven:3.8.6-openjdk-17 AS build
...
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

After (Java 21, non-root user, create/chown data dir, healthcheck):

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
... (dependency:go-offline, mvn package)
FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y curl ca-certificates && useradd --create-home appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### docker-compose.yml

Before (no named volume):

```yaml
version: "3.8"
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
```

After (named volume for H2, healthcheck, restart policy):

```yaml
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
    restart: unless-stopped
    healthcheck:
      test:
        ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
    volumes:
      - backend-data:/app/data
  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
    restart: unless-stopped
volumes:
  backend-data:
```

### .dockerignore (added)

```text
node_modules
dist
target
frontend/node_modules
frontend/dist
backend/target
.git
*.log
```

---
