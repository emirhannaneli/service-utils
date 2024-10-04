package net.lubble.util.controller.reactive

import jakarta.validation.Valid
import net.lubble.util.PageResponse
import net.lubble.util.Response
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.net.URI

/**
 * This interface defines the base operations for a reactive controller.
 * It includes CRUD operations, as well as additional operations for archiving, unarchiving, and managing a recycle bin.
 *
 * @param C The type of the object used in the create operation.
 * @param U The type of the object used in the update operation.
 * @param R The type of the object returned in the response.
 * @param P The type of the object used as parameters in the find operations.
 */
interface ReactiveBaseController<C, U, R, P> {
    /**
     * Create a new entity.
     *
     * @param create The object containing the data to create the new entity.
     * @return A Mono that emits the ResponseEntity of the created entity.
     */
    @PostMapping
    fun create(@RequestBody @Valid create: C): Mono<ResponseEntity<R>>

    /**
     * Find an entity by its ID.
     *
     * @param id The ID of the entity to find.
     * @return A Mono that emits the ResponseEntity of the found entity.
     */
    @GetMapping("{id}")
    fun findById(@PathVariable id: String): Mono<ResponseEntity<R>>

    /**
     * Find all entities.
     *
     * @param params The parameters to use in the find operation.
     * @return A Mono that emits the ResponseEntity of the PageResponse containing all found entities.
     */
    @GetMapping
    fun findAll(@Valid params: P): Mono<ResponseEntity<PageResponse>>

    /**
     * Update an existing entity.
     *
     * @param id The ID of the entity to update.
     * @param update The object containing the data to update the entity.
     * @return A Mono that emits the ResponseEntity of the updated entity.
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: String, @RequestBody @Valid update: U): Mono<ResponseEntity<R>>

    /**
     * Delete an entity.
     *
     * @param id The ID of the entity to delete.
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     */
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String): Mono<ResponseEntity<Response>>

    // Additional operations for archiving, unarchiving, and managing a recycle bin are defined below.
    // These operations throw UnsupportedOperationException by default, and should be overridden in the implementing class if needed.

    /**
     * Find all archived entities.
     *
     * @param params The parameters to use in the find operation.
     * @return A Mono that emits the ResponseEntity of the PageResponse containing all found entities.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @GetMapping("archive")
    fun findAllArchived(@Valid params: P): Mono<ResponseEntity<PageResponse>> {
        throw UnsupportedOperationException()
    }

    /**
     * Archive an entity.
     *
     * @param id The ID of the entity to archive.
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @PutMapping("{id}/archive")
    fun archive(@PathVariable id: String): Mono<ResponseEntity<Response>> {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchive an entity.
     *
     * @param id The ID of the entity to unarchive.
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @PutMapping("{id}/unarchive")
    fun unarchive(@PathVariable id: String): Mono<ResponseEntity<Response>> {
        throw UnsupportedOperationException()
    }

    /**
     * Get the recycle bin.
     *
     * @param params The parameters to use in the find operation.
     * @return A Mono that emits the ResponseEntity of the PageResponse containing all found entities.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @GetMapping("recycle-bin")
    fun recycleBin(@Valid params: P): Mono<ResponseEntity<PageResponse>> {
        throw UnsupportedOperationException()
    }

    /**
     * Clear the recycle bin.
     *
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @DeleteMapping("recycle-bin/clear")
    fun clearRecycleBin(): Mono<ResponseEntity<Response>> {
        throw UnsupportedOperationException()
    }

    /**
     * Restore an entity from the recycle bin.
     *
     * @param id The ID of the entity to restore.
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @PutMapping("{id}/restore")
    fun restore(@PathVariable id: String): Mono<ResponseEntity<Response>> {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently delete an entity.
     *
     * @param id The ID of the entity to delete.
     * @return A Mono that emits the ResponseEntity of the Response indicating the result of the operation.
     * @throws UnsupportedOperationException If the method is not overridden in the implementing class.
     */
    @DeleteMapping("{id}/permanently")
    fun deletePermanently(@PathVariable id: String): Mono<ResponseEntity<Response>> {
        throw UnsupportedOperationException()
    }

    /**
     * Create a Mono that emits a ResponseEntity with a CREATED status and the URI of the created entity.
     *
     * @param read The object to include in the response.
     * @param uri The URI of the created entity.
     * */
    fun created(read: R, uri: String): Mono<ResponseEntity<R>> = Mono.fromCallable { ResponseEntity.created(URI(uri)).body(read) }

    /**
     * Create a Mono that emits a ResponseEntity with an OK status and the object.
     *
     * @param read The object to include in the response.
     * */
    fun ok(read: R): Mono<ResponseEntity<R>> = Mono.fromCallable { ResponseEntity.ok(read) }

    /**
     * Create a Mono that emits a ResponseEntity with an OK status and the object.
     *
     * @param status The status to include in the response.
     * */
    fun ok(status: Boolean): Mono<ResponseEntity<Boolean>> = Mono.fromCallable { ResponseEntity.ok(status) }

    /**
     * Create a Mono that emits a ResponseEntity with a 200 OK status and a fun message.
     *
     * @param message The message to include in the response.
     * */
    fun message(message: String): Mono<ResponseEntity<Response>> = Mono.fromCallable { Response(message).build() }

    /**
     * Create a Mono that emits a PageResponse with the given Page and data.
     *
     * @param page The Page to include in the response.
     * @param data The data to include in the response.
     * */
    fun paged(page: Page<*>, data: Collection<*>): Mono<ResponseEntity<PageResponse>> = Mono.fromCallable { Response.of(page, data) }
}