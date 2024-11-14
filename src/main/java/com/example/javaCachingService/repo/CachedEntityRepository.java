package com.example.javaCachingService.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.javaCachingService.entity.CachedEntity;

/**
 * Repository interface for managing CachedEntity data persistence.
 * Extends JpaRepository for CRUD operations.
 */
@Repository
public interface CachedEntityRepository extends JpaRepository<CachedEntity, Long> {
}

