package com.example.javaCachingService;

import com.example.javaCachingService.entity.CachedEntity;
import com.example.javaCachingService.repo.CachedEntityRepository;
import com.example.javaCachingService.service.CachingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CachingService class.
 * Validates adding, retrieving, and removing cached entities.
 */
@SpringBootTest
public class JavaCachingServiceApplicationTests {

    @Autowired
    private CachingService cachingService;

    @Autowired
    private CachedEntityRepository repository;

    @BeforeEach
    public void setup() {
        // Clear cache and database before each test
        try {
            cachingService.clearCache();
            cachingService.removeAll();
        } catch (Exception e) {
            fail("Exception during setup: " + e.getMessage());
        }
    }

    /**
     * Test adding an entity to the cache.
     */
    @Test
    public void testAddEntityToCache() {
        try {
            CachedEntity entity = new CachedEntity();
            entity.setData("Test data");
            CachedEntity addedEntity = cachingService.add(entity);

            assertNotNull(addedEntity.getId(), "Entity ID should not be null after adding");
            assertEquals("Test data", addedEntity.getData(), "Entity data should match expected");

        } catch (Exception e) {
            fail("Exception in testAddEntityToCache: " + e.getMessage());
        }
    }
    
    /**
     * Test retrieving an entity from the cache.
     */
    @Test
    public void testRetrieveEntityFromCache() {
        try {
            CachedEntity entity = new CachedEntity();
            entity.setData("Retrieve test data");
            CachedEntity addedEntity = cachingService.add(entity);

            CachedEntity retrievedEntity = cachingService.get(addedEntity.getId());
            assertNotNull(retrievedEntity, "Entity should be retrievable from cache");
            assertEquals(addedEntity.getId(), retrievedEntity.getId(), "Retrieved entity ID should match");

        } catch (Exception e) {
            fail("Exception in testRetrieveEntityFromCache: " + e.getMessage());
        }
    }

    /**
     * Test retrieving an entity not present in cache but in database.
     */
    @Test
    public void testRetrieveEntityFromDatabase() {
        try {
            CachedEntity entity = new CachedEntity();
            entity.setData("DB test data");
            CachedEntity savedEntity = repository.save(entity);

            // Ensure cache is clear to test retrieval from database
            cachingService.clearCache();

            CachedEntity retrievedEntity = cachingService.get(savedEntity.getId());
            assertNotNull(retrievedEntity, "Entity should be retrievable from database when not in cache");
            assertEquals(savedEntity.getId(), retrievedEntity.getId(), "Retrieved entity ID should match saved entity ID");

        } catch (Exception e) {
            fail("Exception in testRetrieveEntityFromDatabase: " + e.getMessage());
        }
    }

    /**
     * Test removing an entity from the cache and database.
     */
    @Test
    public void testRemoveEntity() {
        try {
            CachedEntity entity = new CachedEntity();
            entity.setData("Remove test data");
            CachedEntity addedEntity = cachingService.add(entity);

            cachingService.remove(addedEntity.getId());
            assertNull(cachingService.get(addedEntity.getId()), "Entity should not be retrievable after removal");
            assertFalse(repository.existsById(addedEntity.getId()), "Entity should not exist in database after removal");

        } catch (Exception e) {
            fail("Exception in testRemoveEntity: " + e.getMessage());
        }
    }


    /**
     * Test removing all entities from both cache and database.
     */
    @Test
    public void testRemoveAll() {
        try {
            CachedEntity entity1 = new CachedEntity();
            entity1.setData("Remove all test data 1");
            cachingService.add(entity1);

            CachedEntity entity2 = new CachedEntity();
            entity2.setData("Remove all test data 2");
            cachingService.add(entity2);

            cachingService.removeAll();

            assertEquals(0, cachingService.getCacheSize(), "Cache should be empty after removing all entities");
            assertTrue(repository.findAll().isEmpty(), "Database should be empty after removing all entities");

        } catch (Exception e) {
            fail("Exception in testRemoveAll: " + e.getMessage());
        }
    }
}