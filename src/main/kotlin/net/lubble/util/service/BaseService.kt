package net.lubble.util.service

import org.springframework.data.domain.Page

/**
 * Base service interface
 * @param T Entity
 * @param C Create DTO
 * @param U Update DTO
 * @param S Specification
 * @param ID Entity ID type
 * @property create Create Entity
 * @property save Save Entity
 * @property find Find Entity by Specification
 * @property exists Check if Entity exists by Specification
 * @property findById Find Entity by ID
 * @property findAll Find all Entities by Specification
 * @property update Update Entity
 * @property delete Delete Entity (soft delete)
 * @property recycleBin Find all deleted Entities by Specification
 * @property restore Restore Entity from recycle bin
 * @property deletePermanently Delete Entity permanently
 */
interface BaseService<T, C, U, S> {
    fun create(create: C): T

    fun save(base: T): T

    fun find(spec: S): T

    fun exists(spec: S): Boolean

    fun findById(id: String): T

    fun findAll(spec: S): Page<T>

    fun update(base: T, update: U)

    fun delete(base: T)

    fun recycleBin(spec: S): Page<T> {
        throw UnsupportedOperationException()
    }

    fun restore(base: T) {
        throw UnsupportedOperationException()
    }

    fun deletePermanently(base: T) {
        throw UnsupportedOperationException()
    }
}