package com.example.javaCachingService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.javaCachingService.entity.CachedEntity;
import com.example.javaCachingService.service.CachingService;

/**
 * REST Controller for managing cache operations.
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

    @PostMapping("/add")
    @Operation(summary = "Add an entity to cache")
    public ResponseEntity<?> addEntity(@RequestBody CachedEntity entity) {
        try {
            CachedEntity addedEntity = cachingService.add(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(addedEntity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add entity to cache.");
        }
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Get an entity from cache or database")
    public ResponseEntity<?> getEntity(@PathVariable Long id) {
        try {
            CachedEntity entity = cachingService.get(id);
            if (entity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entity with ID " + id + " not found.");
            }
            return ResponseEntity.ok(entity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve entity with ID " + id);
        }
    }

    @DeleteMapping("/remove/{id}")
    @Operation(summary = "Remove an entity from cache and database")
    public ResponseEntity<?> removeEntity(@PathVariable Long id) {
        try {
            cachingService.remove(id);
            return ResponseEntity.ok("Entity with ID " + id + " removed from cache and database.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove entity with ID " + id);
        }
    }

    @DeleteMapping("/removeAll")
    @Operation(summary = "Remove all entities from cache and database")
    public ResponseEntity<?> removeAllEntities() {
        try {
            cachingService.removeAll();
            return ResponseEntity.ok("All entities removed from cache and database.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove all entities from cache and database.");
        }
    }

    @PostMapping("/clearCache")
    @Operation(summary = "Clear all entities from cache")
    public ResponseEntity<?> clearCache() {
        try {
            cachingService.clearCache();
            return ResponseEntity.ok("Cache cleared successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clear the cache.");
        }
    }
}
