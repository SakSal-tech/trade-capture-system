# docker-Explanations.md

## Docker and Compose: What I built, why I built it that way, and how it fixes the real issues

## 1) Objectives

I set out to:

1. I built the **backend** (Spring Boot, Java 21) and **frontend** (Vite/React) in containers in a reproducible way.
2. The frontend with **nginx** in production mode, keeping the image small and fast.
3. Use **Docker Compose** to run both services in development with sensible health checks, restart policies and clear ports.
4. I had 2 issues concrete build failures I hit:
   - The container build failed because host `node_modules` collided with the container’s filesystem.
   - The frontend build script invoked `pnpm` but the image only had `npm`.

---

## 2) The real problems I hit (and how I proved the root causes)

### Problem A: Docker build failed due to host `node_modules` being copied into the image

**Symptom:** During `docker compose up --build` I saw errors complaining about files under `node_modules/@eslint/eslintrc` while running the container build.  
**Cause:** My Dockerfile did `COPY . .`. Because I hadn’t set a proper `.dockerignore`, Docker sent my entire local working directory (including `node_modules`, `dist` and other artifacts) into the build context. Those files conflicted with the image’s own install and caused confusing errors.

**Fix:** I added **.dockerignore** files at the root of the frontend and backend build contexts so `COPY . .` only brings in source that should actually go into the image. This keeps the build context small and prevents file clashes.

### Problem B: Frontend build failed with `sh: pnpm: not found`

**Symptom:** The Docker build printed:

```
> frontend@0.0.0 build
> pnpm lint && vite build
sh: pnpm: not found
```

**Cause:** The project’s scripts use `pnpm`, but my `node:18-alpine` image only had `npm`. In containers, the package manager must match what the scripts call.  
**Fix:** I **enabled Corepack** inside the build image and used it to activate `pnpm` at the version pinned by the lockfile. I then switched the Dockerfile to run `pnpm install --frozen-lockfile` and `pnpm build`. That gives **deterministic installs** and avoids the “pnpm not found” error.

---

## 3) The final file set (compose, Dockerfiles, .dockerignore, and optional nginx.conf)

### 3.1 docker-compose.yml

I orchestrated the two services for local dev. The backend exposes `8080`, the frontend is served by nginx on `3000`. I added a healthcheck to the backend so Compose can observe readiness, and I set restart policies for a smoother dev loop.

```yaml
# Note: Docker Compose V2 files may omit the top-level `version` key. The
# repository's `docker-compose.yml` omits it; the example below matches the
# repository and therefore starts directly with `services:`.
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

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
    restart: unless-stopped
```

**Why I did it this way:**

- The **backend** healthcheck calls `/actuator/health` which is the conventional Spring Boot readiness endpoint. If Actuator is disabled, I can switch the URL to a public endpoint.
- `restart: unless-stopped` makes dev smoother if a service crashes.
- I kept **ports explicit** and stable so developers know where to browse and which API URL to hit.
- I deliberately avoided volume mounts here to keep it simple and reproducible; for live‑reload workflows we can add volumes later.

---

### 3.2 Backend Dockerfile (multi‑stage, Java 21, non‑root, healthcheck ready)

I chose a classic Maven build stage and a slim Java 21 runtime stage. I also install `curl` in the runtime image so the container can run an internal healthcheck against the app port.

```dockerfile
# ============================
# BUILD STAGE (Java 21)
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Cache dependencies first for faster rebuilds
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests


# ============================
# RUNTIME STAGE (Java 21, non-root)
# ============================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Install curl for healthchecks and create a non-root user
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --create-home --shell /bin/bash appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Drop privileges
RUN chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Optional container-level healthcheck (works with the curl above)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Why this works well:**

- **Same Java version** in build and runtime avoids “it works on my laptop but not in the container”.
- `dependency:go-offline` makes rebuilds faster by caching artifacts in a layer.
- I **run as a non‑root user** which is a good default for container security.
- The healthcheck is optional but useful in Compose and in container platforms.

---

### 3.3 Frontend Dockerfile (Corepack + pnpm in build stage, nginx in runtime)

This switches the build to `pnpm` via Corepack and serves the static app from nginx.

```dockerfile
# ============================
# BUILD STAGE
# ============================
FROM node:18-alpine AS build
WORKDIR /app

# Enable Corepack and prepare pnpm so scripts like "pnpm lint" will work
RUN corepack enable && corepack prepare pnpm@latest --activate

# Copy only manifests first to leverage caching
COPY package.json pnpm-lock.yaml* ./

# Deterministic install based on the lockfile
RUN pnpm install --frozen-lockfile

# Copy source and build
COPY . .
RUN pnpm build


# ============================
# PRODUCTION STAGE (nginx)
# ============================
FROM nginx:alpine

# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from the previous stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", " -g", "daemon off;"]
```

**Why I did it this way:**

- I **match the project’s package manager** to the image by enabling Corepack and activating `pnpm`. That removes the `pnpm: not found` error and uses the **lockfile** for reproducible builds.
- I copy `package.json` and `pnpm-lock.yaml` first so dependency install layers can be cached.
- Vite outputs to `dist`, which nginx serves from `/usr/share/nginx/html`.
- If we need SPA fallback routing, I can provide a small custom `nginx.conf` later.

**Optional nginx.conf for a SPA:**

```nginx
server {
  listen 80;
  server_name _;

  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri /index.html;
  }

  # If the frontend proxies API calls during local dev, configure here:
  # location /api/ {
  #   proxy_pass http://backend:8080;
  # }
}
```

---

### 3.4 .dockerignore files

These stop Docker from sending huge, unnecessary directories into the build context, which prevents collisions and speeds up `docker build` significantly.

**Root or frontend/.dockerignore** (for the frontend build context):

```
node_modules
dist
build
.cache
.vite
npm-debug.log
pnpm-lock.yaml.*.tmp
.git
.gitignore
Dockerfile
docker-compose.yml
```

**backend/.dockerignore** (for the backend build context):

```
target
.idea
*.iml
.git
.gitignore
Dockerfile
docker-compose.yml
```

**Why this matters:**

- Keeps build contexts small, faster and safer.
- Prevents the `COPY . .` pattern from dragging host `node_modules` into the image.
- Removes confusing file clashes like the ESLint config incident I saw earlier.

---

## 4) How this design meets the business and engineering goals

- **Deterministic frontend builds** via `pnpm` and its lockfile. Reproducibility is key for CI and cross‑machine parity.
- **Secured backend runtime** by dropping root in the container. That is a best practice for production‑grade setups.
- **Faster rebuilds** from targeted caching steps (`dependency:go-offline`, copying only manifests before source).
- **Health visibility** with the Actuator healthcheck, which allows Compose or an orchestrator to confirm the app is ready.
- **Simple local UX**: `http://localhost:3000` for the UI, `http://localhost:8080` for the API. No surprises.

---

## 5) Day‑to‑day commands I actually run

### Build everything clean

```bash
docker compose build --no-cache
```

### Build and run

```bash
docker compose up --build
```

### Tear down

```bash
docker compose down
```

### Inspect logs

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

### Shell into a container (for debugging)

```bash
docker compose exec backend bash
docker compose exec frontend sh
```

---

## 6) Troubleshooting notes I wrote for future me

1. **Frontend 404 on deep links**: add the SPA `try_files` rule in nginx.conf so client‑side routes resolve to `index.html`.
2. **Backend healthcheck fails**: ensure `spring-boot-starter-actuator` is on the classpath and `/actuator/health` is exposed for the `local` profile.
3. **CORS issues in dev**: consider enabling dev CORS in Spring or proxy `/api` from nginx to the backend when running behind Compose.
4. **Slow builds**: check that the `.dockerignore` files are in place, and leverage `dependency:go-offline`.
5. **pnpm version drift**: I use `corepack prepare pnpm@latest --activate`. To pin a specific version, change that line to the required version and commit the lockfile with it.

---

## 7) CI/CD readiness tips

If I wire this into CI:

- Use the **same Dockerfiles** so CI builds match local builds exactly.
- Pass profile flags via `SPRING_PROFILES_ACTIVE`.
- Cache Maven and pnpm layers by keeping the **manifest copy steps** early in Dockerfiles.
- Push multi‑arch images if needed by your platform using Buildx.

---

## 8) Summary of key techniques (and why I chose them)

- **Multi‑stage Dockerfiles**: small production images, clean separation of build vs runtime.
- **Corepack + pnpm in container**: the build environment matches package manager used by the project, and the lockfile ensures identical dependency resolution.
- **.dockerignore hygiene**: prevents host artifacts from poisoning the container build context.
- **Non‑root runtime**: safer defaults for production.
- **Healthchecks**: observable containers that play nicely with Compose and orchestrators.
- **Explicit ports and restart policies**: clear local DX without guesswork.

I tested both backend and frontend and they both up and running http://localhost:8080/actuator/health and http://localhost:3000/signin

### Looks good I tried bo

saksa@Work-Sak MINGW64 ~/cbfacademy/trade-capture-system (main)
$ docker ps
CONTAINER ID IMAGE COMMAND CREATED STATUS PORTS NAMES
439947ab7e4d trade-capture-system-frontend "/docker-entrypoint.…" 5 minutes ago Up 5 minutes 0.0.0.0:3000->80/tcp, [::]:3000->80/tcp trade-capture-system-frontend-1
2d02442ad319 trade-capture-system-backend "java -jar /app/app.…" 5 minutes ago Up 5 minutes (healthy) 0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp trade-capture-system-backend-1

ackend-1 | 2025-11-08T00:57:36.785Z DEBUG 1 --- [nio-8080-exec-5] o.s.w.s.m.m.a.HttpEntityMethodProcessor : Using 'application/vnd.spring-boot.actuator.v3+json', given [*/*] and supported [application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json]
backend-1 | 2025-11-08T00:57:36.786Z DEBUG 1 --- [nio-8080-exec-5] o.s.w.s.m.m.a.HttpEntityMethodProcessor : Writing [org.springframework.boot.actuate.health.SystemHealth@16874f58]
backend-1 | 2025-11-08T00:57:36.789Z DEBUG 1 --- [nio-8080-exec-5] o.s.web.servlet.DispatcherServlet : Completed 200 OK
