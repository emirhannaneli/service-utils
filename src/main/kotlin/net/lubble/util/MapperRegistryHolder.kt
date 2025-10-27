package net.lubble.util

import net.lubble.util.dto.RBase
import java.util.*

/**
 * MapperRegistryHolder caches already mapped entities and DTOs
 * to avoid circular references or duplicate work.
 */
class MapperRegistryHolder private constructor() {
    private val registry = ThreadLocal.withInitial { WeakHashMap<Any, RBase>() }

    companion object {
        private val instance = MapperRegistryHolder()

        /**
         * Get cached DTO for given key (BaseModel or BaseDocumented).
         */
        @Suppress("UNCHECKED_CAST")
        fun <R : RBase> get(key: Any): R? {
            return instance.registry.get()[key] as? R
        }

        /**
         * Put DTO in cache for given key (BaseModel or BaseDocumented).
         */
        fun <R : RBase> put(key: Any, value: R) {
            instance.registry.get()[key] = value
        }

        /**
         * Clear cache for current thread (important for thread pools).
         */
        fun clear() {
            instance.registry.get().clear()
        }
    }
}