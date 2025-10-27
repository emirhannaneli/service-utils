package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.lubble.util.DocumentRegistryHolder
import org.springframework.data.annotation.Transient
import java.io.Serializable

abstract class BaseDocumented<T : BaseModel>(
    @Transient
    override var ref: T? = null
) : BaseModel(), BaseDocumentedSchema<T> {

    @Transient
    private var rootContextOwner: Boolean = false

    init {
        val ctx = if (!DocumentRegistryHolder.hasContext()) {
            rootContextOwner = true
            DocumentRegistryHolder.createContext()
        } else {
            DocumentRegistryHolder.current()!!
        }

        val id = id(ref)
        val reg = ctx.registry
        val stack = ctx.visiting

        if (id != null && !reg.containsKey(id)) {
            stack.add(id)
            try {
                reg[id] = this
                ref?.let { src ->
                    @Suppress("UNCHECKED_CAST")
                    val typedNode = this as BaseDocumented<BaseModel>
                    typedNode.apply(src, typedNode)
                    typedNode.mapping(src)
                }
            } finally {
                stack.remove(id)
            }
        }

        if (rootContextOwner) {
            DocumentRegistryHolder.closeContext()
        }
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
            mutableMapOf<String, BaseDocumented<*>>()
        }
        val visiting = ThreadLocal.withInitial {
            mutableSetOf<String>()
        }

        fun id(source: BaseModel?): String? =
            source?.let { "${it::class.simpleName}-${it.pk}-${it.sk}" }

        @Suppress("UNCHECKED_CAST")
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

            val existing = reg[id] as? D
            if (existing != null) return existing

            stack.add(id)
            return try {
                val newObj = factory(source)
                reg[id] = newObj
                newObj
            } finally {
                stack.remove(id)
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["ref", "rootContextOwner"])
interface BaseDocumentedSchema<T : BaseModel> : Serializable {
    var ref: T?
    fun mapping(source: T): BaseDocumented<T>
}
