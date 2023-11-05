package net.lubble.util.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface BaseJPA<T>: JpaRepository<T,UUID>, JpaSpecificationExecutor<T> {
}