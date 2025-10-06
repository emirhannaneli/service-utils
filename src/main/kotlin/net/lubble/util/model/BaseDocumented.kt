package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

abstract class BaseDocumented<T : BaseModel>(
    override var source: T?
) : BaseModel(), BaseDocumentedSchema<T> {

    init {
        val id = id(source)
        val reg = registry.get()
        val stack = visiting.get()

        if (id != null && !reg.containsKey(id)) {
            stack.add(id)
            reg[id] = this

            // mapping + apply
            source?.let { src ->
                @Suppress("UNCHECKED_CAST")
                val typedNode = this as BaseDocumented<BaseModel>
                typedNode.apply(src, typedNode)
                typedNode.mapping(src)
            }

            stack.remove(id)
        }
        this.source = null
    }

    private fun id(source: BaseModel?): String? =
        source?.let { "${it::class.simpleName}-${it.pk}-${it.sk}" }

    fun map(source: T): BaseDocumented<T> {
        apply(source, this)
        return mapping(source)
    }

    fun <D : BaseDocumented<T>> apply(source: BaseModel, target: D) {
        target.setId(source.getId())
        target.pk = source.pk
        target.sk = source.sk
        target.archived = source.archived
        target.deleted = source.deleted
        target.createdAt = source.createdAt
        target.updatedAt = source.updatedAt
    }

    companion object {
        val registry = ThreadLocal.withInitial {
            mutableMapOf<String, BaseDocumented<*>>() // id → node
        }
        val visiting = ThreadLocal.withInitial {
            mutableSetOf<String>() // aktif stack (cycle kontrolü için)
        }

        fun id(source: BaseModel?): String? =
            source?.let { "${it::class.simpleName}-${it.pk}-${it.sk}" }

        inline fun <reified T : BaseModel, D : BaseDocumented<T>> from(
            source: T?,
            factory: (T) -> D
        ): D? {
            if (source == null) return null
            val id = id(source) ?: return null
            val reg = registry.get()
            val stack = visiting.get()

            if (id in stack) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            val existing = reg[id] as? D
            if (existing != null) return existing

            stack.add(id)
            val newObj = factory(source)
            reg[id] = newObj
            stack.remove(id)

            return newObj
        }

        fun clear() {
            registry.get().clear()
            visiting.get().clear()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["source"])
interface BaseDocumentedSchema<T : BaseModel> : Serializable {
    var source: T?
    fun mapping(source: T): BaseDocumented<T>
}
