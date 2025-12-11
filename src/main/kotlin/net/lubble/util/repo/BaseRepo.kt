package net.lubble.util.repo

import net.lubble.util.model.BaseModel
import net.lubble.util.projection.LElasticNativeProjection
import net.lubble.util.projection.LElasticProjection
import net.lubble.util.projection.LJPAProjection
import net.lubble.util.projection.LMongoProjection
import org.bson.types.ObjectId
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
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
    interface JPA<T : BaseModel> : JpaRepository<T, String>, JpaSpecificationExecutor<T>, LJPAProjection<T>

    @NoRepositoryBean
    interface Elastic<T : BaseModel> : ElasticsearchRepository<T, String>, LElasticProjection<T>, LElasticNativeProjection<T>
}