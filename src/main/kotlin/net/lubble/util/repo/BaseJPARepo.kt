package net.lubble.util.repo

import net.lubble.util.LJPAProjection
import net.lubble.util.model.BaseJPAModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import java.util.*

/**
 * BaseJPARepo is an interface that extends JpaRepository and JpaSpecificationExecutor.
 * It is annotated with @NoRepositoryBean to indicate that it is not a repository itself.
 * Instead, it is used as a base interface for actual repository interfaces.
 *
 * @param T The type of the entity the repository manages. It must be a class that is annotated with @Entity.
 *
 * @property JpaRepository<T, UUID> This is a repository interface for generic CRUD operations on a repository for a specific type.
 * @property JpaSpecificationExecutor<T> Interface to allow execution of Specifications based on the JPA criteria API.
 */
@NoRepositoryBean
interface BaseJPARepo<T : BaseJPAModel> : JpaRepository<T, UUID>, JpaSpecificationExecutor<T>, LJPAProjection<T>