package net.lubble.util

import net.lubble.util.model.BaseDocumented

object DocumentRegistryHolder {
    private val contexts = ThreadLocal.withInitial { ArrayDeque<DocumentContext>() }

    fun hasContext(): Boolean = contexts.get().isNotEmpty()

    fun current(): DocumentContext? = contexts.get().lastOrNull()

    fun createContext(): DocumentContext {
        val ctx = DocumentContext()
        contexts.get().addLast(ctx)
        return ctx
    }

    fun closeContext() {
        contexts.get().removeLastOrNull()
    }
}

class DocumentContext {
    val registry = mutableMapOf<String, BaseDocumented<*>>()
    val visiting = mutableSetOf<String>()
}
