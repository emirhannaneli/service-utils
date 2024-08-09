package net.lubble.util.controller

import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.Valid
import net.lubble.util.GraphPageResponse
import net.lubble.util.GraphResponse
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping

/**
 * Base interface for GraphQL controllers providing common CRUD operations.
 *
 * @param C The type of the create input.
 * @param U The type of the update input.
 * @param R The type of the response.
 * @param P The type of the pagination options.
 */
interface BaseGraphController<C, U, R, P> {
    /**
     * Creates a new entity.
     *
     * @param input The input data for creating the entity.
     * @return The created entity.
     */
    @MutationMapping
    fun create(@Argument @Valid input: C): R

    /**
     * Finds an entity by its ID.
     *
     * @param id The ID of the entity to find.
     * @param env The data fetching environment.
     * @return The found entity.
     */
    @QueryMapping
    fun findById(@Argument id: String, env: DataFetchingEnvironment): R

    /**
     * Finds all entities with pagination options.
     *
     * @param options The pagination options.
     * @param env The data fetching environment.
     * @return A paginated response containing the entities.
     */
    @QueryMapping
    fun findAll(@Argument @Valid options: P, env: DataFetchingEnvironment): GraphPageResponse

    /**
     * Updates an existing entity.
     *
     * @param id The ID of the entity to update.
     * @param input The input data for updating the entity.
     * @return The updated entity.
     */
    @MutationMapping
    fun update(@Argument id: String, @Argument @Valid input: U): R

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete.
     * @return A response indicating the result of the deletion.
     */
    @MutationMapping
    fun delete(@Argument id: String): GraphResponse

    /**
     * Finds all archived entities with pagination options.
     *
     * @param options The pagination options.
     * @param env The data fetching environment.
     * @return A paginated response containing the archived entities.
     */
    @QueryMapping
    fun findAllArchived(@Argument @Valid options: P, env: DataFetchingEnvironment): GraphPageResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Archives an entity by its ID.
     *
     * @param id The ID of the entity to archive.
     * @return A response indicating the result of the archiving.
     */
    @MutationMapping
    fun archive(@Argument id: String): GraphResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchives an entity by its ID.
     *
     * @param id The ID of the entity to unarchive.
     * @return A response indicating the result of the unarchiving.
     */
    @MutationMapping
    fun unarchive(@Argument id: String): GraphResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Finds all entities in the recycle bin with pagination options.
     *
     * @param options The pagination options.
     * @param env The data fetching environment.
     * @return A paginated response containing the entities in the recycle bin.
     */
    @QueryMapping
    fun recycleBin(@Argument @Valid options: P, env: DataFetchingEnvironment): GraphPageResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Clears the recycle bin.
     *
     * @return A response indicating the result of clearing the recycle bin.
     */
    @MutationMapping
    fun clearRecycleBin(): GraphResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Restores an entity from the recycle bin by its ID.
     *
     * @param id The ID of the entity to restore.
     * @return A response indicating the result of the restoration.
     */
    @MutationMapping
    fun restore(@Argument id: String): GraphResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently deletes an entity by its ID.
     *
     * @param id The ID of the entity to permanently delete.
     * @return A response indicating the result of the permanent deletion.
     */
    @MutationMapping
    fun deletePermanently(@Argument id: String): GraphResponse {
        throw UnsupportedOperationException()
    }

    /**
     * Creates a response message.
     *
     * @param message The message to include in the response.
     * @return A response containing the message.
     */
    fun message(message: String): GraphResponse = GraphResponse(message)

    /**
     * Creates a paginated response.
     *
     * @param page The page information.
     * @param data The data to include in the response.
     * @return A paginated response containing the data.
     */
    fun paged(page: Page<*>, data: Collection<*>): GraphPageResponse = GraphResponse.of(page, data)

    /**
     * Retrieves the fields for a pagination query from the data fetching environment.
     *
     * @param env The data fetching environment.
     * @return A list of field names for the pagination query.
     */
    fun pQueryFields(env: DataFetchingEnvironment): List<String> {
        return env.field.selectionSet.selections.find {
            val field = it as Field
            field.name == "data"
        }?.let {
            val items = it as Field
            items.selectionSet.selections.map { field -> (field as Field).name }
        } ?: emptyList()
    }


    /**
     * Retrieves the fields for a read query from the data fetching environment.
     *
     * @param env The data fetching environment.
     * @return A list of field names for the read query.
     */
    fun rQueryFields(env: DataFetchingEnvironment): List<String> {
        return env.field.selectionSet.selections.map {
            val field = it as Field
            field.name
        }
    }
}