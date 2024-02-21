package net.lubble.util.service

import org.springframework.data.domain.Page

/**
 * This interface defines the basic CRUD operations for a service.
 * It uses generic types for the entity (T), create (C), update (U), and specification (S).
 */
interface BaseService<T, C, U, S> {

    /**
     * Create a new entity.
     * @param create The entity to create.
     * @return The created entity.
     */
    fun create(create: C): T

    /**
     * Save an entity.
     * @param base The entity to save.
     * @return The saved entity.
     */
    fun save(base: T): T

    /**
     * Find an entity by its specification.
     * @param spec The specification to use when finding the entity.
     * @return The found entity.
     */
    fun find(spec: S): T

    /**
     * Check if an entity exists by its specification.
     * @param spec The specification to use when checking if the entity exists.
     * @return A boolean indicating if the entity exists.
     */
    fun exists(spec: S): Boolean

    /**
     * Find an entity by its ID.
     * @param id The ID of the entity to find.
     * @return The found entity.
     */
    fun findById(id: String): T

    /**
     * Find all entities by their specification.
     * @param spec The specification to use when finding the entities.
     * @return A page of found entities.
     */
    fun findAll(spec: S): Page<T>

    /**
     * Update an entity.
     * @param base The entity to update.
     * @param update The updated entity.
     */
    fun update(base: T, update: U)

    /**
     * Delete an entity.
     * @param base The entity to delete.
     */
    fun delete(base: T)

    /**
     * Find all archived entities by their specification.
     * @param spec The specification to use when finding the archived entities.
     * @return A page of found archived entities.
     */
    fun findAllArchived(spec: S): Page<T> {
        throw UnsupportedOperationException()
    }

    /**
     * Archive an entity.
     * @param base The entity to archive.
     */
    fun archive(base: T) {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchive an entity.
     * @param base The entity to unarchive.
     */
    fun unarchive(id: String) {
        throw UnsupportedOperationException()
    }

    /**
     * Find all entities in the recycle bin by their specification.
     * @param spec The specification to use when finding the entities in the recycle bin.
     * @return A page of found entities in the recycle bin.
     */
    fun recycleBin(spec: S): Page<T> {
        throw UnsupportedOperationException()
    }

    /**
     * Clear the recycle bin.
     */
    fun clearRecycleBin() {
        throw UnsupportedOperationException()
    }

    /**
     * Restore an entity from the recycle bin.
     * @param id The ID of the entity to restore.
     */
    fun restore(id: String) {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently delete an entity.
     * @param id The ID of the entity to delete.
     */
    fun deletePermanently(id: String) {
        throw UnsupportedOperationException()
    }
}