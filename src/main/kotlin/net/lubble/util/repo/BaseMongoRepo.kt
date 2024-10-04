package net.lubble.util.repo

import net.lubble.util.LMongoProjection
import net.lubble.util.model.BaseMongoModel
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * BaseMongoRepo is an interface that extends MongoRepository.
 * It is annotated with @NoRepositoryBean to indicate that it is not a repository itself.
 * Instead, it is used as a base interface for actual repository interfaces.
 *
 * @param T The type of the entity the repository manages.
 *
 * @property MongoRepository<T, ObjectId> This is a repository interface for generic CRUD operations on a repository for a specific type.
 */
@NoRepositoryBean
interface BaseMongoRepo<T : BaseMongoModel> : MongoRepository<T, ObjectId>, LMongoProjection<T>