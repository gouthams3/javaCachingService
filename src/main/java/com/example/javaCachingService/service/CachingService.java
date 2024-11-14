package com.example.javaCachingService.service;


import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class CachingService {
	private static final Logger log = LoggerFactory.getLogger(CachingService.class);
    private final CachedEntityRepository repository;
    private final int maxCacheSize; // Maximum number of items in the cache
    private final Map<Long, CachedEntity> cache = new HashMap<>(); // Cache to store entities
    private final Map<Long, Integer> frequencyMap = new HashMap<>(); // Frequency of each entity access
    private final PriorityQueue<Long> leastUsedQueue = new PriorityQueue<>((a, b) -> frequencyMap.get(a) - frequencyMap.get(b)); // LFU priority queue

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
        if (cache.size() >= maxCacheSize) {
            evictLeastUsed();
        }
        cache.put(entity.getId(), entity);
        frequencyMap.put(entity.getId(), frequencyMap.getOrDefault(entity.getId(), 0) + 1);
        leastUsedQueue.offer(entity.getId());
        log.info("Added entity with ID: {} to cache", entity.getId());
        return repository.save(entity);
    }

    /**
     * Retrieves an entity from the cache or loads it from the database if not found.
     *
     * @param the ID of the entity
     * @return the retrieved entity, or null if not found
     */
    public CachedEntity get(Long id) {
        if (cache.containsKey(id)) {
            frequencyMap.put(id, frequencyMap.get(id) + 1);
            leastUsedQueue.remove(id);
            leastUsedQueue.offer(id);
            log.info("Entity with ID: {} found in cache", id);
            return cache.get(id);
        } else {
            log.info("Entity with ID: {} not found in cache. Loading from database", id);
            return repository.findById(id).orElse(null);
        }
    }

    /**
     * Removes an entity from both the cache and the database.
     *
     * @param id the ID of the entity to remove
     */
    public void remove(Long id) {
        cache.remove(id);
        frequencyMap.remove(id);
        leastUsedQueue.remove(id);
        repository.deleteById(id);
        log.info("Removed entity with ID: {} from cache and database", id);
    }

    /**
     * Removes all entities from the cache and the database.
     */
    public void removeAll() {
        cache.clear();
        frequencyMap.clear();
        leastUsedQueue.clear();
        repository.deleteAll();
        log.info("Removed all entities from cache and database");
    }

    /**
     * Clears all entities from the cache without affecting the database.
     */
    public void clearCache() {
        cache.clear();
        frequencyMap.clear();
        leastUsedQueue.clear();
        log.info("Cleared the cache");
    }

    /**
     * Evicts the least frequently used entity from the cache to maintain cache size.
     */
    private void evictLeastUsed() {
        Long leastUsedId = leastUsedQueue.poll();
        if (leastUsedId != null) {
            CachedEntity entityToEvict = cache.remove(leastUsedId);
            frequencyMap.remove(leastUsedId);
            log.info("Evicting entity with ID: {} from cache to database", leastUsedId);
            repository.save(entityToEvict); // Persist to database on eviction
        }
    }
}
