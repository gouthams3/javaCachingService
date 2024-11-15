package com.example.javaCachingService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.javaCachingService.entity.CachedEntity;
import com.example.javaCachingService.repo.CachedEntityRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Thread-safe caching service implementing an LFU (Least Frequently Used) eviction policy.
 * This service manages in-memory caching for entities and persists data to the database upon eviction.
 * Designed for concurrent access, it uses synchronized blocks and concurrent collections to ensure thread safety.
 */
@Service // Spring-managed singleton
public class CachingService {

    private static final Logger log = LoggerFactory.getLogger(CachingService.class);

    // Repository for database operations on CachedEntity
    private final CachedEntityRepository repository;
    // Maximum cache size before eviction occurs
    private final int maxCacheSize;

    // Thread-safe collections to store cache entries and their access frequencies
    private final ConcurrentHashMap<Long, CachedEntity> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> frequencyMap = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Long> leastUsedQueue;

    /**
     * Constructor initializes the repository, maximum cache size, and thread-safe priority queue for LFU.
     *
     * @param repository   The repository handling database operations.
     * @param maxCacheSize The maximum number of entities allowed in the cache.
     */
    public CachingService(CachedEntityRepository repository, @Value("${cache.max.size:5}") int maxCacheSize) {
        this.repository = repository;
        this.maxCacheSize = maxCacheSize;
        this.leastUsedQueue = new PriorityBlockingQueue<>(maxCacheSize, (a, b) -> frequencyMap.get(a) - frequencyMap.get(b));
    }

    /**
     * Adds an entity to the cache, evicting the least frequently used entity if the cache size exceeds the limit.
     * Uses synchronized blocks for thread safety in concurrent scenarios.
     *
     * @param entity The entity to be cached.
     * @return The entity after being saved in the database.
     */
    public CachedEntity add(CachedEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot add a null entity to the cache");
        }
        try {
            synchronized (this) {
                // Ensure entity has an ID by saving it if it's new
                if (entity.getId() == null) {
                    entity = repository.save(entity);
                    log.info("Generated ID {} for new entity", entity.getId());
                }

                // Check if eviction is required based on cache size
                if (cache.size() >= maxCacheSize) {
                    evictLeastUsed();
                }

                // Add entity to cache and update its frequency for LFU tracking
                cache.put(entity.getId(), entity);
                frequencyMap.put(entity.getId(), frequencyMap.getOrDefault(entity.getId(), 0) + 1);
                leastUsedQueue.offer(entity.getId());
                log.info("Added entity with ID: {} to cache", entity.getId());

                // Persist the entity to the database to maintain access consistency
                return repository.save(entity);
            }
        } catch (Exception e) {
            log.error("Failed to add entity with ID: {} to cache or database", entity.getId(), e);
            throw new RuntimeException("An error occurred while adding the entity to cache and database", e);
        }
    }

    /**
     * Retrieves an entity by its ID from the cache or loads it from the database if not cached.
     * Updates the entity's frequency for LFU ordering.
     *
     * @param id The ID of the entity to retrieve.
     * @return The cached or database-loaded entity, or null if not found.
     */
    public CachedEntity get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        try {
            if (cache.containsKey(id)) {
                synchronized (leastUsedQueue) {
                    // Increment access frequency and re-order in LFU queue
                    frequencyMap.put(id, frequencyMap.get(id) + 1);
                    leastUsedQueue.remove(id);
                    leastUsedQueue.offer(id);
                }
                log.info("Entity with ID: {} found in cache", id);
                return cache.get(id);
            } else {
                log.info("Entity with ID: {} not found in cache. Attempting to load from database", id);
                CachedEntity entityFromDb = repository.findById(id).orElse(null);
                if (entityFromDb == null) {
                    log.warn("Entity with ID: {} not found in database either", id);
                    return null;
                }

                // Cache the loaded entity with an initial frequency count
                synchronized (this) {
                    cache.put(id, entityFromDb);
                    frequencyMap.put(id, 1);
                    leastUsedQueue.offer(id);
                }
                log.info("Entity with ID: {} loaded from database and cached", id);
                return entityFromDb;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve entity with ID: {} from cache or database", id, e);
            throw new RuntimeException("An error occurred while retrieving the entity", e);
        }
    }

    /**
     * Removes an entity from both the cache and the database based on its ID.
     * Ensures safe removal from shared resources using synchronization.
     *
     * @param id The ID of the entity to remove.
     */
    public void remove(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        try {
            if (!cache.containsKey(id) && !repository.existsById(id)) {
                log.warn("Entity with ID: {} not found in cache or database. Nothing to remove.", id);
                return;
            }
            synchronized (leastUsedQueue) {
                cache.remove(id);
                frequencyMap.remove(id);
                leastUsedQueue.remove(id);
            }
            repository.deleteById(id);
            log.info("Removed entity with ID: {} from cache and database", id);
        } catch (Exception e) {
            log.error("Failed to remove entity with ID: {} from cache or database", id, e);
            throw new RuntimeException("An error occurred while removing the entity", e);
        }
    }

    /**
     * Removes all entities from the cache and the database.
     * This method clears both the in-memory cache and persistent storage.
     */
    public void removeAll() {
        try {
            synchronized (leastUsedQueue) {
                cache.clear();
                frequencyMap.clear();
                leastUsedQueue.clear();
            }
            repository.deleteAll();
            log.info("Removed all entities from cache and database");
        } catch (Exception e) {
            log.error("Failed to remove all entities from cache or database", e);
            throw new RuntimeException("An error occurred while removing all entities", e);
        }
    }

    /**
     * Clears only the in-memory cache without impacting the database records.
     * Useful for resetting cache state while maintaining persistence.
     */
    public void clearCache() {
        try {
            synchronized (leastUsedQueue) {
                cache.clear();
                frequencyMap.clear();
                leastUsedQueue.clear();
            }
            log.info("Cleared the cache");
        } catch (Exception e) {
            log.error("Failed to clear the cache", e);
            throw new RuntimeException("An error occurred while clearing the cache", e);
        }
    }

    /**
     * Evicts the least frequently used entity from the cache to maintain the maximum cache size.
     * Persists the evicted entity to the database to avoid data loss.
     */
    private void evictLeastUsed() {
        try {
            synchronized (leastUsedQueue) {
                Long leastUsedId = leastUsedQueue.poll();
                if (leastUsedId != null) {
                    CachedEntity entityToEvict = cache.remove(leastUsedId);
                    frequencyMap.remove(leastUsedId);
                    log.info("Evicting entity with ID: {} from cache to database", leastUsedId);
                    repository.save(entityToEvict); // Persist evicted entity
                }
            }
        } catch (Exception e) {
            log.error("Failed to evict least frequently used entity from cache", e);
            throw new RuntimeException("An error occurred while evicting the least frequently used entity", e);
        }
    }

    /**
     * Retrieves the current number of entities in the cache.
     * This method is thread-safe and used for monitoring cache usage.
     *
     * @return The count of entities in the cache.
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
