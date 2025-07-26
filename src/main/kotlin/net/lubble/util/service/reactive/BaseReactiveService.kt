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
    fun find(spec: S): Mono<T?>

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
}