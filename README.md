# Java Caching Service

## Description

The Java Caching Service is a Spring Boot-based API that implements an in-memory caching mechanism with LFU (Least Frequently Used) eviction policy. When the cache reaches its maximum size, the least-used entity is evicted from memory and persisted in a PostgreSQL database. The service provides endpoints to add, retrieve, remove, and clear cached entities. It also includes API documentation via Swagger.

## Features

- **Configurable Cache Size**: Set the maximum number of elements in memory.
- **LFU Eviction**: Evicts the least-used entity when the cache reaches its maximum size.
- **Database Persistence**: Automatically saves evicted entities to the database.
- **RESTful APIs**: Endpoints for adding, retrieving, removing, and clearing cached entities.
- **Swagger Documentation**: API documentation is available at `/swagger-ui.html`.

## Technologies Used

- **Java 17**
- **Spring Boot**
- **PostgreSQL**
- **Swagger (OpenAPI)** for API documentation

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL installed and running
- Maven for project build and dependencies

### Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/yourusername/java-caching-service.git
   cd java-caching-service
