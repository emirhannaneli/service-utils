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

        fun clear() {
            instance.registry.get().clear()
        }
    }
}
