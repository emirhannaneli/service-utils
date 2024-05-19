package net.lubble.util.service.reactive

import org.springframework.data.domain.Page
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * This interface defines the basic CRUD operations for a reactive service.
 * It uses generic types for the entity (T), create (C), update (U), and specification (S).
 * @param T The entity type.
 * @param C The create type.
 * @param U The update type.
 * @param S The specification type.
 * @see Transactional
 */
interface BaseReactiveService<T, C, U, S> {

    /**
     * Create a new entity.
     * @param create The entity to create.
     * @return The created entity wrapped in a Mono.
     */
    fun create(create: C): Mono<T>

    /**
     * Save an entity.
     * @param base The entity to save.
     * @return The saved entity wrapped in a Mono.
     */
    fun save(base: T): Mono<T>

    /**
     * Find an entity by its specification.
     * @param spec The specification to use when finding the entity.
     * @return The found entity wrapped in a Mono.
     */
    fun find(spec: S): Mono<T>

    /**
     * Check if an entity exists by its specification.
     * @param spec The specification to use when checking if the entity exists.
     * @return A boolean indicating if the entity exists wrapped in a Mono.
     */
    fun exists(spec: S): Mono<Boolean>

    /**
     * Find an entity by its ID.
     * @param id The ID of the entity to find.
     * @return The found entity wrapped in a Mono.
     */
    fun findById(id: String): Mono<T>

    /**
     * Find all entities by their specification.
     * @param spec The specification to use when finding the entities.
     * @return A page of found entities wrapped in a Mono.
     */
    fun findAll(spec: S): Mono<Page<T>>

    /**
     * Update an entity.
     * @param base The entity to update.
     * @param update The updated entity.
     * @return A void wrapped in a Mono.
     */
    fun update(base: T, update: U): Mono<Void>

    /**
     * Delete an entity.
     * @param base The entity to delete.
     * @return A void wrapped in a Mono.
     */
    fun delete(base: T): Mono<Void>

    /**
     * Find all archived entities by their specification.
     * @param spec The specification to use when finding the archived entities.
     * @return A page of found archived entities wrapped in a Mono.
     */
    fun findAllArchived(spec: S): Mono<Page<T>> {
        throw UnsupportedOperationException()
    }

    /**
     * Archive an entity.
     * @param id The ID of the entity to archive.
     * @return A void wrapped in a Mono.
     */
    fun archive(id: String): Mono<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchive an entity.
     * @param id The ID of the entity to unarchive.
     * @return A void wrapped in a Mono.
     */
    fun unarchive(id: String): Mono<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Find all entities in the recycle bin by their specification.
     * @param spec The specification to use when finding the entities in the recycle bin.
     * @return A page of found entities in the recycle bin wrapped in a Mono.
     */
    fun recycleBin(spec: S): Mono<Page<T>> {
        throw UnsupportedOperationException()
    }

    /**
     * Clear the recycle bin.
     * @return A void wrapped in a Mono.
     */
    fun clearRecycleBin(): Mono<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Restore an entity from the recycle bin.
     * @param id The ID of the entity to restore.
     * @return A void wrapped in a Mono.
     */
    fun restore(id: String): Mono<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently delete an entity.
     * @param id The ID of the entity to delete.
     * @return A void wrapped in a Mono.
     */
    fun deletePermanently(id: String): Mono<Void> {
        throw UnsupportedOperationException()
    }
}