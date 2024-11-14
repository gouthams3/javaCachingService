package com.example.javaCachingService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
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
    public CachedEntity addEntity(@RequestBody CachedEntity entity) {
        return cachingService.add(entity);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Get an entity from cache or database")
    public CachedEntity getEntity(@PathVariable Long id) {
        return cachingService.get(id);
    }

    @DeleteMapping("/remove/{id}")
    @Operation(summary = "Remove an entity from cache and database")
    public void removeEntity(@PathVariable Long id) {
        cachingService.remove(id);
    }

    @DeleteMapping("/removeAll")
    @Operation(summary = "Remove all entities from cache and database")
    public void removeAllEntities() {
        cachingService.removeAll();
    }

    @PostMapping("/clearCache")
    @Operation(summary = "Clear all entities from cache")
    public void clearCache() {
        cachingService.clearCache();
    }
}
