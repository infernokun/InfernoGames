# syntax=docker/dockerfile:1.4.2
# Stage 1: Compile
FROM node:lts-bullseye AS compile-stage

WORKDIR /app/

# Install pnpm globally
RUN npm install -g pnpm

COPY --link package.json pnpm-lock.yaml /app/
COPY --link angular.json tsconfig.json tsconfig.app.json /app/
COPY --link src/ /app/src/

RUN --mount=type=cache,target=/root/.npm/_cacache \
  --mount=type=secret,id=npmrc,dst=/root/.npmrc \
  set -eux \
  && pnpm install --frozen-lockfile \
  && pnpm run build:app

# Stage 2: Build runtime image
FROM nginx:alpine AS runtime-stage

LABEL maintainer="infernokun@infernokun.com" \
  description="NGINX Angular Docker image" \
  source="https://github.com/infernokun/InfernoGames" \
  run="docker run -p 80:80 infernokun/inferno-games -d"

# Create nginx user with fixed UID/GID for consistency
RUN addgroup -g 101 -S nginx && \
    adduser -D -u 101 -S nginx

# Copy nginx config
COPY --link nginx/nginx.conf /etc/nginx/nginx.conf

# Set proper ownership and permissions
RUN set -eux \
  && chown -R nginx:nginx /var/cache/nginx \
  && chown -R nginx:nginx /var/log/nginx \
  && chown -R nginx:nginx /etc/nginx \
  && chown -R nginx:nginx /usr/share/nginx \
  && touch /var/run/nginx.pid \
  && chown nginx:nginx /var/run/nginx.pid \
  && rm -f /etc/nginx/conf.d/default.conf

# Copy built application
COPY --link --from=compile-stage /app/dist/app/browser /usr/share/nginx/html/

# Copy and set permissions for entrypoint script
COPY --link --chmod=755 ./scripts/docker-entrypoint.sh /docker-entrypoint.sh

VOLUME [ "/etc/nginx" ]

EXPOSE 80
HEALTHCHECK --start-period=5s CMD curl --fail http://localhost/ || exit 1

USER nginx

# Start Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]