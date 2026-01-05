# InfernoGames
[![Docker Image](https://img.shields.io/docker/v/infernokun/inferno-games-web?label=Docker%20Image)](https://hub.docker.com/r/infernokun/inferno-games-web)
[![Build Status](https://img.shields.io/github/actions/workflow/status/infernokun/inferno-games-web/ci.yml?label=CI%20Build)](https://github.com/infernokun/inferno-games-web/actions)

A modern web application built with **Angular 21** to discover, track, and conquer your gaming backlog.

> **Angular Version**: 21 
> **Base URL**: http://inferno-games-web  
> **REST API Endpoint**: /api

---

## Features
- **Backlog Tracking**: Manage your "To Play," "Playing," and "Completed" lists.
- **Game Discovery**: Search and explore a massive database of titles.
- **Data Insights**: Advanced filtering and sorting using **AG Grid**.
- **Modern UI**: Fully responsive interface built with **Angular Material**.
- **Real-time Sync**: Updates across instances via WebSocket integration.
- **Docker-Ready**: Optimized for containerized deployments.

---

## Architecture
The application is structured for scalability and performance:
- **Frontend**: Angular 21, TypeScript, and SCSS.
- **UI Components**: Angular Material components for a consistent design.
- **State Management**: Reactive data flows using RxJS Observables.
- **Deployment**: Multi-stage Docker builds with Nginx.

---

## Getting Started

### Prerequisites
- Node.js (v18+ recommended)
- pnpm (`npm install -g pnpm`)
- Angular CLI (`pnpm install -g @angular/cli`)
- Docker (optional)

### Development Setup
```bash
# Install dependencies
pnpm install

# Start the development server
ng serve

# Navigate to http://localhost:4200/
```

## Docker Compose
```yaml
services:
  inferno-games-web:
    image: infernokun/inferno-games-web:latest
    restart: always
    environment:
      - BASE_URL=http://localhost:4200
      - API_URL=http://localhost:8080/inferno-games-rest/api
    ports:
      - "4200:4200"
```

## Project Structure

- `src/app/components/` - Reusable UI components
- `src/app/models/` - Data models and interfaces
- `src/app/services/` - API service layer
- `src/app/utils/` - Utility functions and animations
- `src/assets/` - Static assets and configuration files
- `src/styles/` - Global styles and themes

---

## Build Process

The application uses Nx for build orchestration and supports:
- Development builds
- Production builds with optimization
- Testing configurations
- Continuous integration workflows

---

## Deployment

The application is configured for Docker deployment with:
- Multi-stage Docker builds
- Environment-specific configurations
- Nginx reverse proxy setup
- CORS handling for API communication