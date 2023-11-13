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
 * @property findById Find Entity by ID
 * @property findAll Find all Entities by Specification
 * @property update Update Entity
 * @property delete Delete Entity
 */
interface BaseService<T, C, U, S, ID> {
    fun create(create: C): T

    fun save(base: T): T

    fun find(spec: S): T

    fun findById(id: ID): T

    fun findAll(spec: S): Page<T>

    fun update(base: T, update: U)

    fun delete(base: T)
}