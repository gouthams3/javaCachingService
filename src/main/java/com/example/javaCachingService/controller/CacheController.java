package com.example.javaCachingService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.example.javaCachingService.entity.CachedEntity;
import com.example.javaCachingService.service.CachingService;

/**
 * REST Controller for managing cache operations.
 * Provides endpoints to add, retrieve, remove, and clear cache entries.
 * Each method includes error handling and validation for robust and reliable API interactions.
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache Controller", description = "APIs for managing cache")
public class CacheController {

    private final CachingService cachingService;

    @Autowired
    public CacheController(CachingService cachingService) {
        this.cachingService = cachingService;
    }

    /**
     * Adds a new entity to the cache. 
     * If the entity does not contain valid data, a 400 Bad Request is returned.
     * @param entity - the entity to be added to the cache
     * @return ResponseEntity containing the added entity or an error message
     */
    @PostMapping("/add")
    @Operation(summary = "Add an entity to cache")
    public ResponseEntity<?> addEntity(@RequestBody CachedEntity entity) {
        if (entity == null) {
            // Check for null entity to avoid NullPointerException
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Entity cannot be null.");
        }
        if (!StringUtils.hasText(entity.getData())) {
            // Ensure the 'data' field is not empty or null
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Entity data cannot be empty.");
        }
        try {
            // Attempt to add the entity to the cache
            CachedEntity addedEntity = cachingService.add(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(addedEntity);
        } catch (IllegalArgumentException e) {
            // Catch specific exceptions for targeted error responses
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Handle any unexpected server errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add entity to cache.");
        }
    }

    /**
     * Retrieves an entity from the cache by ID.
     * If the entity is not found, a 404 Not Found response is returned.
     * @param id - the ID of the entity to retrieve
     * @return ResponseEntity containing the found entity or an error message
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "Get an entity from cache or database")
    public ResponseEntity<?> getEntity(@PathVariable Long id) {
        if (id == null) {
            // ID validation to prevent null input
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Entity ID cannot be null.");
        }
        try {
            // Attempt to retrieve the entity from cache or database
            CachedEntity entity = cachingService.get(id);
            if (entity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entity with ID " + id + " not found.");
            }
            return ResponseEntity.ok(entity);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve entity with ID " + id);
        }
    }

    /**
     * Removes an entity from both the cache and database by ID.
     * If the entity ID does not exist, a 404 Not Found response is returned.
     * @param id - the ID of the entity to remove
     * @return ResponseEntity confirming the removal or an error message
     */
    @DeleteMapping("/remove/{id}")
    @Operation(summary = "Remove an entity from cache and database")
    public ResponseEntity<?> removeEntity(@PathVariable Long id) {
        if (id == null) {
            // Ensure ID is not null before proceeding
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Entity ID cannot be null.");
        }
        try {
            // Attempt to remove the entity
            cachingService.remove(id);
            return ResponseEntity.ok("Entity with ID " + id + " removed from cache and database.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove entity with ID " + id);
        }
    }

    /**
     * Removes all entities from both the cache and database.
     * Useful for bulk deletion or resetting the entire cache.
     * @return ResponseEntity confirming the bulk removal or an error message
     */
    @DeleteMapping("/removeAll")
    @Operation(summary = "Remove all entities from cache and database")
    public ResponseEntity<?> removeAllEntities() {
        try {
            // Attempt to clear all entries from both cache and database
            cachingService.removeAll();
            return ResponseEntity.ok("All entities removed from cache and database.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove all entities from cache and database.");
        }
    }

    /**
     * Clears all entities from the cache only, without affecting the database.
     * Useful for refreshing cache while preserving data persistence.
     * @return ResponseEntity confirming the cache clearance or an error message
     */
    @PostMapping("/clearCache")
    @Operation(summary = "Clear all entities from cache")
    public ResponseEntity<?> clearCache() {
        try {
            // Attempt to clear the cache only
            cachingService.clearCache();
            return ResponseEntity.ok("Cache cleared successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clear the cache.");
        }
    }
}
