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
 * Service class implementing the caching mechanism with LFU eviction policy.
 * Manages both in-memory cache and persistence to database when eviction occurs.
 */
@Service
public class CachingService {

    private static final Logger log = LoggerFactory.getLogger(CachingService.class);
    private final CachedEntityRepository repository;
    private final int maxCacheSize;
    private final Map<Long, CachedEntity> cache = new HashMap<>();
    private final Map<Long, Integer> frequencyMap = new HashMap<>();
    private final PriorityQueue<Long> leastUsedQueue = new PriorityQueue<>((a, b) -> frequencyMap.get(a) - frequencyMap.get(b));

    public CachingService(CachedEntityRepository repository, @Value("${cache.max.size:5}") int maxCacheSize) {
        this.repository = repository;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Adds an entity to the cache.
     * If the cache exceeds the max size, it evicts the least frequently used entity.
     *
     * @param entity the entity to add to the cache
     * @return the saved entity
     */
    public CachedEntity add(CachedEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot add a null entity to the cache");
        }
        try {
            if (entity.getId() == null) {
                entity = repository.save(entity);
                log.info("Generated ID {} for new entity", entity.getId());
            }

            if (cache.size() >= maxCacheSize) {
                evictLeastUsed();
            }

            cache.put(entity.getId(), entity);
            frequencyMap.put(entity.getId(), frequencyMap.getOrDefault(entity.getId(), 0) + 1);
            leastUsedQueue.offer(entity.getId());
            log.info("Added entity with ID: {} to cache", entity.getId());

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
     * Retrieves an entity from the cache or loads it from the database if not found.
     *
     * @param id the ID of the entity
     * @return the retrieved entity, or null if not found
     */
    public CachedEntity get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        try {
            if (cache.containsKey(id)) {
                frequencyMap.put(id, frequencyMap.get(id) + 1);
                leastUsedQueue.remove(id);
                leastUsedQueue.offer(id);
                log.info("Entity with ID: {} found in cache", id);
                return cache.get(id);
            } else {
                log.info("Entity with ID: {} not found in cache. Attempting to load from database", id);
                CachedEntity entityFromDb = repository.findById(id).orElse(null);
                if (entityFromDb == null) {
                    log.warn("Entity with ID: {} not found in database either", id);
                    return null;
                }

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
     * Removes an entity from both the cache and the database.
     *
     * @param id the ID of the entity to remove
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

            cache.remove(id);
            frequencyMap.remove(id);
            leastUsedQueue.remove(id);
            repository.deleteById(id);
            log.info("Removed entity with ID: {} from cache and database", id);
        } catch (Exception e) {
            log.error("Failed to remove entity with ID: {} from cache or database", id, e);
            throw new RuntimeException("An error occurred while removing the entity", e);
        }
    }

    /**
     * Removes all entities from the cache and the database.
     */
    public void removeAll() {
        try {
            cache.clear();
            frequencyMap.clear();
            leastUsedQueue.clear();
            repository.deleteAll();
            log.info("Removed all entities from cache and database");
        } catch (Exception e) {
            log.error("Failed to remove all entities from cache or database", e);
            throw new RuntimeException("An error occurred while removing all entities", e);
        }
    }

    /**
     * Clears all entities from the cache without affecting the database.
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
     */
    private void evictLeastUsed() {
        try {
            Long leastUsedId = leastUsedQueue.poll();
            if (leastUsedId != null) {
                CachedEntity entityToEvict = cache.remove(leastUsedId);
                frequencyMap.remove(leastUsedId);
                log.info("Evicting entity with ID: {} from cache to database", leastUsedId);
                repository.save(entityToEvict); // Persist to database on eviction
            }
        } catch (Exception e) {
            log.error("Failed to evict least frequently used entity from cache", e);
            throw new RuntimeException("An error occurred while evicting the least frequently used entity", e);
        }
    }

    /**
     * Retrieves the current size of the cache.
     *
     * @return the number of entities currently stored in the cache
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
