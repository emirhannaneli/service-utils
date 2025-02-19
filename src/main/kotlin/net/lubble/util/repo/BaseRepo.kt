package net.lubble.util.repo

import net.lubble.util.LJPAProjection
import net.lubble.util.LMongoProjection
import net.lubble.util.model.BaseModel
import org.bson.types.ObjectId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*

@NoRepositoryBean
abstract class BaseRepo {
    @NoRepositoryBean
    interface Mongo<T : BaseModel> : MongoRepository<T, ObjectId>, LMongoProjection<T>

    @NoRepositoryBean
    interface JPA<T : BaseModel> : JpaRepository<T, UUID>, JpaSpecificationExecutor<T>, LJPAProjection<T>
}