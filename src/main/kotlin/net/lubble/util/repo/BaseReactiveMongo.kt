package net.lubble.util.repo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * BaseReactiveMongo is an interface that extends ReactiveMongoRepository.
 * It is annotated with @NoRepositoryBean to indicate that it is not a repository itself.
 * Instead, it is used as a base interface for actual repository interfaces.
 *
 * @param T The type of the entity the repository manages.
 *
 * @property ReactiveMongoRepository<T, ObjectId> This is a repository interface for generic CRUD operations on a repository for a specific type.
 * It provides reactive support for MongoDB operations.
 */
@NoRepositoryBean
interface BaseReactiveMongo<T> : ReactiveMongoRepository<T, ObjectId> {

}