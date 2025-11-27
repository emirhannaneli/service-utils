package net.lubble.util

import net.lubble.util.dto.RBase

object ModelRegistryHolder {
    private val contexts = ThreadLocal.withInitial { ArrayDeque<ModelContext>() }

    fun hasContext(): Boolean = contexts.get().isNotEmpty()

    fun current(): ModelContext? = contexts.get().lastOrNull()

    fun createContext(): ModelContext {
        val ctx = ModelContext()
        contexts.get().addLast(ctx)
        return ctx
    }

    fun closeContext() {
        contexts.get().removeLastOrNull()
    }
}

class ModelContext {
    val registry = mutableMapOf<String, RBase>()
    val visiting = mutableSetOf<String>()
}

