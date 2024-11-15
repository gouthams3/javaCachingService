package com.example.javaCachingService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.javaCachingService.entity.CachedEntity;
import com.example.javaCachingService.repo.CachedEntityRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Service class implementing a caching mechanism with LFU (Least Frequently Used) eviction policy.
 * Manages an in-memory cache and provides persistence to the database upon eviction or removal.
 * Designed to ensure efficient cache management, avoiding memory overflow with a limited cache size.
 */
@Service
public class CachingService {

    private static final Logger log = LoggerFactory.getLogger(CachingService.class);
    private final CachedEntityRepository repository;
    private final int maxCacheSize;
    private final Map<Long, CachedEntity> cache = new HashMap<>();
    private final Map<Long, Integer> frequencyMap = new HashMap<>();
    private final PriorityQueue<Long> leastUsedQueue = new PriorityQueue<>((a, b) -> frequencyMap.get(a) - frequencyMap.get(b));

    /**
     * Constructor for CachingService.
     *
     * @param repository   The repository to manage database operations for cached entities.
     * @param maxCacheSize The maximum number of entities that the cache can hold before eviction.
     */
    public CachingService(CachedEntityRepository repository, @Value("${cache.max.size:5}") int maxCacheSize) {
        this.repository = repository;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Adds an entity to the cache and persists it to the database.
     * If the cache exceeds the maximum size, it evicts the least frequently used entity to free up space.
     *
     * @param entity The entity to add to the cache.
     * @return The saved entity.
     */
    public CachedEntity add(CachedEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot add a null entity to the cache");
        }
        try {
            // Persist new entities without ID to generate a unique ID
            if (entity.getId() == null) {
                entity = repository.save(entity);
                log.info("Generated ID {} for new entity", entity.getId());
            }

            // Evict least-used entity if cache size limit is reached
            if (cache.size() >= maxCacheSize) {
                evictLeastUsed();
            }

            // Add the entity to the cache with updated frequency
            cache.put(entity.getId(), entity);
            frequencyMap.put(entity.getId(), frequencyMap.getOrDefault(entity.getId(), 0) + 1);
            leastUsedQueue.offer(entity.getId());
            log.info("Added entity with ID: {} to cache", entity.getId());

            // Save entity to the database to persist its current state
            return repository.save(entity);

        } catch (IllegalArgumentException e) {
            log.error("Entity cannot be null", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to add entity with ID: {} to cache or database", entity.getId(), e);
            throw new RuntimeException("An error occurred while adding the entity to cache and database", e);
        }
    }

    /**
     * Retrieves an entity from the cache, or loads it from the database if not found in cache.
     * Updates the entity's frequency count in the cache.
     *
     * @param id The ID of the entity to retrieve.
     * @return The retrieved entity, or null if not found in the database.
     */
    public CachedEntity get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        try {
            if (cache.containsKey(id)) {
                // Update access frequency for LFU policy
                frequencyMap.put(id, frequencyMap.get(id) + 1);
                leastUsedQueue.remove(id);
                leastUsedQueue.offer(id);
                log.info("Entity with ID: {} found in cache", id);
                return cache.get(id);
            } else {
                // Load entity from database if not in cache
                log.info("Entity with ID: {} not found in cache. Attempting to load from database", id);
                CachedEntity entityFromDb = repository.findById(id).orElse(null);
                if (entityFromDb == null) {
                    log.warn("Entity with ID: {} not found in database either", id);
                    return null;
                }

                // Add loaded entity to cache with frequency count initialized
                cache.put(id, entityFromDb);
                frequencyMap.put(id, 1);
                leastUsedQueue.offer(id);
                log.info("Entity with ID: {} loaded from database and cached", id);
                return entityFromDb;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve entity with ID: {} from cache or database", id, e);
            throw new RuntimeException("An error occurred while retrieving the entity", e);
        }
    }

    /**
     * Removes an entity from both the cache and the database by ID.
     *
     * @param id The ID of the entity to remove.
     */
    public void remove(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        try {
            // Check if entity exists in cache or database before removal
            if (!cache.containsKey(id) && !repository.existsById(id)) {
                log.warn("Entity with ID: {} not found in cache or database. Nothing to remove.", id);
                return;
            }

            // Remove entity from cache
            cache.remove(id);
            frequencyMap.remove(id);
            leastUsedQueue.remove(id);

            // Remove entity from database
            repository.deleteById(id);
            log.info("Removed entity with ID: {} from cache and database", id);
        } catch (Exception e) {
            log.error("Failed to remove entity with ID: {} from cache or database", id, e);
            throw new RuntimeException("An error occurred while removing the entity", e);
        }
    }

    /**
     * Removes all entities from both the cache and the database.
     * This clears the entire cache and permanently deletes all records in the database.
     */
    public void removeAll() {
        try {
            // Clear all entries in the cache and frequency map
            cache.clear();
            frequencyMap.clear();
            leastUsedQueue.clear();

            // Delete all records from the database
            repository.deleteAll();
            log.info("Removed all entities from cache and database");
        } catch (Exception e) {
            log.error("Failed to remove all entities from cache or database", e);
            throw new RuntimeException("An error occurred while removing all entities", e);
        }
    }

    /**
     * Clears all entities from the cache without affecting the database.
     * Useful for resetting in-memory cache while preserving data persistence.
     */
    public void clearCache() {
        try {
            cache.clear();
            frequencyMap.clear();
            leastUsedQueue.clear();
            log.info("Cleared the cache");
        } catch (Exception e) {
            log.error("Failed to clear the cache", e);
            throw new RuntimeException("An error occurred while clearing the cache", e);
        }
    }

    /**
     * Evicts the least frequently used entity from the cache to maintain cache size.
     * Persists the evicted entity back to the database to ensure data retention.
     */
    private void evictLeastUsed() {
        try {
            Long leastUsedId = leastUsedQueue.poll();
            if (leastUsedId != null) {
                // Remove least-used entity from cache and persist to database
                CachedEntity entityToEvict = cache.remove(leastUsedId);
                frequencyMap.remove(leastUsedId);
                log.info("Evicting entity with ID: {} from cache to database", leastUsedId);
                repository.save(entityToEvict); // Persist to database upon eviction
            }
        } catch (Exception e) {
            log.error("Failed to evict least frequently used entity from cache", e);
            throw new RuntimeException("An error occurred while evicting the least frequently used entity", e);
        }
    }

    /**
     * Retrieves the current size of the cache.
     * Used to monitor the number of entities currently stored in cache.
     *
     * @return The number of entities currently stored in the cache.
     */
    public int getCacheSize() {
        try {
            return cache.size();
        } catch (Exception e) {
            log.error("Failed to retrieve cache size", e);
            throw new RuntimeException("An error occurred while retrieving the cache size", e);
        }
    }
}
