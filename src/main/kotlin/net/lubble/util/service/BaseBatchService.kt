package net.lubble.util.service

import net.lubble.util.dto.CBatch
import net.lubble.util.dto.UBatch
import net.lubble.util.model.BaseModel

interface BaseBatchService<T : BaseModel, C, U> {
    fun batchCreate(batch: Collection<CBatch<C>>): Collection<T>

    fun batchUpdate(batch: Collection<UBatch<U>>): Collection<T>

    fun saveAll(entities: Collection<T>): Collection<T>
}