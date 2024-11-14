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

## Method Descriptions

### CachingService Class Methods

1. **`add(CachedEntity entity)`**  
   - Adds an entity to the cache. If the cache exceeds the maximum allowed size (`maxCacheSize`), it evicts the least frequently used (LFU) entity to make space. If the entity’s `ID` is null, it is first saved to the database to generate an auto-assigned ID. The entity is then stored in the cache, its access frequency is updated, and it is persisted in the database.

2. **`get(Long id)`**  
   - Retrieves an entity by `ID` from the cache if available. If the entity is not found in the cache, it attempts to load it from the database. Once retrieved from the database, the entity is added to the cache, with an initial frequency count of 1. If the entity is not found in both the cache and database, it returns `null`.

3. **`remove(Long id)`**  
   - Removes an entity by `ID` from both the cache and the database. It first checks if the entity exists in the cache or database. If found, it removes the entity from the cache structures (including frequency and priority tracking) and then deletes it from the database. If the entity doesn’t exist, a warning is logged.

4. **`removeAll()`**  
   - Clears all entries from both the cache and the database. It removes all entities from the in-memory cache structures and deletes all records from the database, effectively resetting the cache and database contents.

5. **`clearCache()`**  
   - Clears only the in-memory cache without impacting the database. It removes all entries from the cache, frequency map, and priority queue, but the entities remain in the database.

6. **`evictLeastUsed()`**  
   - Helper method that removes the least frequently used entity from the cache to maintain cache size. It uses a priority queue to identify the least frequently used entity, removes it from the cache, and saves it to the database if it’s not already persisted.

These methods ensure efficient cache management, keeping frequently accessed entities in memory and moving the least-used entities to the database for optimal performance.


### Setup Instructions

1. **Clone the Repository**:
   git clone https://github.com/yourusername/java-caching-service.git
   cd java-caching-service
2. **Configure PostgreSQL Database**:  
   Create a PostgreSQL database named `entity_data` for the application. Open the `src/main/resources/application.properties` file and update it with your PostgreSQL credentials. Set the following properties:
   - `spring.datasource.url=jdbc:postgresql://localhost:5432/entity_data`
   - `spring.datasource.username=your_username`
   - `spring.datasource.password=your_password`
   - `spring.jpa.hibernate.ddl-auto=update`

   Replace `your_username` and `your_password` with your actual PostgreSQL credentials.

3. **Build the Project**:  
   Use Maven to clean and build the project by running the command:
   mvn clean install

4. **Run Application**:  
   Start the application by running:
   mvn spring-boot:run