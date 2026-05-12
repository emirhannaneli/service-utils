package net.lubble.util

import net.lubble.util.dto.RBase
import java.util.IdentityHashMap

/**
 * MapperRegistryHolder caches already mapped entities and DTOs to avoid circular
 * references and duplicate work within a single mapping operation.
 *
 * Uses [IdentityHashMap] (reference equality) — different load events of the same
 * entity (different Hibernate sessions, transactions, or refetches) produce distinct
 * instances and therefore distinct cache keys, even though [BaseModel.equals] would
 * mark them equal by `pk + sk`.
 *
 * The registry is [ThreadLocal] and Tomcat reuses request threads, so the cache MUST
 * be cleared between requests via [clear]. Lubble installs a `OncePerRequestFilter`
 * that does this automatically; async event handlers that invoke mappers must call
 * [clear] when their work completes.
 *
 * When an entity is mutated in-memory between two mapping calls (e.g. an update
 * mutation followed by re-mapping the same managed instance), the cache would
 * otherwise return the stale pre-mutation DTO. Callers that mutate an entity
 * must invoke [invalidate] for that entity before re-mapping it.
 * [net.lubble.util.mapper.BaseMapper.map] (U, T) does this automatically.
 */
class MapperRegistryHolder private constructor() {
    private val registry = ThreadLocal.withInitial { IdentityHashMap<Any, RBase>() }

    companion object {
        private val instance = MapperRegistryHolder()

        @Suppress("UNCHECKED_CAST")
        fun <R : RBase> get(key: Any): R? {
            return instance.registry.get()[key] as? R
        }

        fun <R : RBase> put(key: Any, value: R) {
            instance.registry.get()[key] = value
        }

        /**
         * Drops the cached DTO for [key] so the next [get] returns null and the
         * next [net.lubble.util.mapper.BaseMapper.map] re-runs `mapping` against
         * the current entity state. Use this whenever you mutate an entity in
         * memory and intend to re-map it.
         */
        fun invalidate(key: Any) {
            instance.registry.get().remove(key)
        }

        fun clear() {
            instance.registry.get().clear()
        }
    }
}
