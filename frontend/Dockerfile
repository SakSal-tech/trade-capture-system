# -----------------------------------------------------------------------------
# Frontend Dockerfile
# For Vite/React frontend
# - Build stage: build static assets with Node (produces /app/dist)
# - Production stage: serve static assets with nginx
# Notes: repository contains pnpm lockfile; consider using pnpm (Corepack) for deterministic installs
# and add a .dockerignore to exclude node_modules, dist, target to speed builds.
# -----------------------------------------------------------------------------
# ============================
# BUILD STAGE
# ============================
FROM node:18-alpine AS build
WORKDIR /app

# Copy package manifest and lockfile for deterministic installs
COPY package.json pnpm-lock.yaml ./

# Enable Corepack and install pnpm, then install deps using pnpm (deterministic)
RUN corepack enable && corepack prepare pnpm@latest --activate
RUN pnpm install --frozen-lockfile

# Copy source and build using pnpm
COPY . .
RUN pnpm build


# ============================
# PRODUCTION STAGE
# ============================
FROM nginx:alpine

# Copy built frontend into nginx HTML directory
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
