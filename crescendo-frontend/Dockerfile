# Stage 1: Build static assets
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies with clean cache
COPY package.json package-lock.json ./
RUN npm ci

# Copy application source code and build
COPY . .
RUN npm run build

# Stage 2: Serve with lightweight Nginx (<25MB RAM consumption)
FROM nginx:alpine

# Copy custom Nginx SPA routing configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
