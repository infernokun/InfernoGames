# InfernoGames REST API

[![Docker Image](https://img.shields.io/docker/v/infernokun/inferno-games-rest?label=Docker%20Image)](https://hub.docker.com/r/infernokun/inferno-games-rest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/infernokun/inferno-games-rest/ci.yml?label=CI%20Build)](https://github.com/infernokun/inferno-games-rest/actions)

A robust REST API backend built with **Spring Boot 3.x** and **Java 25** to manage and track video game backlogs and collections.

> **Java Version**: 25
> **Spring Boot Version**: 3.x
> **Base URL**: `/api`

---

## 🔥 Features

- **Game Library Management**
    - Track titles, play status, and completion time
    - Platform-specific library organization
    - Metadata synchronization with IGDB/RAWG
    - Support for DLCs and expansions

- **Integration & Sync**
    - External API integration for game covers and metadata
    - Nextcloud integration for asset synchronization (box art, manual scans)
    - Batch processing for library imports

- **Real-time Communication**
    - **WebSocket** support for live library updates
    - **Server-Sent Events (SSE)** for long-running import progress tracking
    - Session and Auth management

- **Performance & Scalability**
    - **Redis** caching layer for high-frequency game lookups
    - Asynchronous processing for image recognition and metadata fetching
    - Database optimization for complex filtering

---

## 🏗️ Architecture

The application follows a layered architecture:

- **Controller Layer**: REST endpoints and API handlers
- **Service Layer**: Core business logic and backlog orchestration
- **Repository Layer**: Data access via Spring Data JPA
- **Integration Layer**: Clients for IGDB, Nextcloud, and AI APIs
- **Utility Layer**: Helper classes for image processing and logging

---

## 🛣️ API Endpoints

### Core Resources
- **Games**: Manage individual game entries and statuses
- **Platforms**: Track consoles and launchers (Steam, PS5, Retro, etc.)
- **Stats**: Generate analytics on backlog progress and playtime
- **Progress**: Track live processing of library imports
- **Recognition**: Image recognition for game covers and screenshots

### Management Endpoints
- Cache eviction and management
- System health and performance metrics
- Configuration management
- Version and build information

---

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.x
- **Language**: Java 25
- **Database**: PostgreSQL (via JPA/Hibernate)
- **Caching**: Redis
- **Documentation**: OpenAPI / Swagger UI
- **Messaging**: WebSockets & SSE
- **External APIs**: IGDB, Nextcloud, custom AI recognition models

---

## 🚀 Getting Started

### Prerequisites
- **Java 25+**
- **PostgreSQL** database
- **Redis** server
- **Docker** (optional, for containerized deployments)

### Setup Instructions

1. Clone the repository: `git clone https://github.com/infernokun/inferno-games-rest.git`
2. Configure database connection in `src/main/resources/application.yml`
3. Set up Redis connection details
4. Add your external API keys (IGDB Client ID/Secret)
5. Run the application: `./gradlew bootRun`

---

## 💻 Development

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application locally
./gradlew bootRun
```

## Configuration

The application uses Spring Boot configuration with:
- `application.yml` for main settings
- `application-local.yml` for local development
- Environment variables for sensitive data

---

## Project Structure

- `src/main/java/com/infernokun/infernoGames/` - Main application packages
- `controllers/` - REST endpoint controllers
- `services/` - Business logic implementations
- `repositories/` - Data access objects
- `models/` - Data transfer objects and entities
- `clients/` - External API clients
- `config/` - Application configuration classes
- `utils/` - Utility classes and helpers
- `logger/` - Custom logging implementation