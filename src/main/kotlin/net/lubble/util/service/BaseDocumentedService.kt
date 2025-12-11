package net.lubble.util.service

import net.lubble.util.model.BaseDocumented
import net.lubble.util.model.BaseModel
import org.springframework.data.domain.Page

/**
 * Base interface for a generic service layer.
 *
 * @param T The type of the entity.
 * @param D The type of the DTO (Data Transfer Object).
 * @param S The type of the specification or criteria used for querying.
 */
interface BaseDocumentedService<T : BaseModel, D : BaseDocumented<in T>, S> {

    /**
     * Saves the given DTO.
     *
     * @param doc The DTO to save.
     * @return The saved DTO.
     */
    fun save(doc: D): D

    /**
     * Finds a single DTO based on the given specification.
     *
     * @param spec The specification or criteria to find the DTO.
     * @return The found DTO, or null if no match is found.
     */
    fun find(spec: S): D?

    /**
     * Checks if a DTO exists based on the given specification.
     *
     * @param spec The specification or criteria to check existence.
     * @return True if a matching DTO exists, false otherwise.
     */
    fun exists(spec: S): Boolean


    /**
     * Finds all DTOs matching the given specification.
     *
     * @param spec The specification or criteria to find DTOs.
     * @return A page of matching DTOs.
     */
    fun searchPaged(spec: S): Page<D> = throw NotImplementedError("Paged search not implemented")

    /**
     * Finds all DTOs matching the given specification.
     *
     * @param spec The specification or criteria to find DTOs.
     * @return A list of matching DTOs.
     */
    fun search(spec: S): List<D>

    /**
     * Counts the number of DTOs matching the given specification.
     *
     * @param spec The specification or criteria to count DTOs.
     * @return The count of matching DTOs.
     */
    fun count(spec: S): Long

    /**
     * Deletes the given DTO.
     *
     * @param doc The DTO to delete.
     */
    fun delete(doc: D)
}