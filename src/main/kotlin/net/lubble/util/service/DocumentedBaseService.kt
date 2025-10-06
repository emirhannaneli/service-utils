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
interface DocumentedBaseService<T : BaseModel, D : BaseDocumented<T>, S> {

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
    fun search(spec: S): Page<D>

    /**
     * Deletes the given DTO.
     *
     * @param doc The DTO to delete.
     */
    fun delete(doc: D)
}