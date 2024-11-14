package com.example.javaCachingService.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Entity class representing cached data.
 * Each entity has a unique ID and data content.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the entity

    private String data; // Content of the entity
    
    // getter and setter methods
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}

