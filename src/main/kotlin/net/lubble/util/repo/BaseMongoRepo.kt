package net.lubble.util.repo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseMongoRepo<T> : MongoRepository<T, ObjectId> {
}