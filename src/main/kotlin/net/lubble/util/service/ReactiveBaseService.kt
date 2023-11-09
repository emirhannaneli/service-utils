package net.lubble.util.service

import org.springframework.data.domain.Page
import reactor.core.publisher.Mono

/**
 * Reactive base service interface
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
interface ReactiveBaseService<T, C, U, S, ID> {
    fun create(create: C)

    fun save(base: T): Mono<T>

    fun find(spec: S): Mono<T>

    fun findById(id: ID): Mono<T>

    fun findAll(spec: S): Mono<Page<T>>

    fun update(base: T, update: U)

    fun delete(base: T)
}