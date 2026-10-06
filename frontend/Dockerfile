FROM node:22-alpine AS build

WORKDIR /workspace/frontend
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Unprivileged variant: runs as uid 101, no root needed to bind (port 8080).
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 8080
