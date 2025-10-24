package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Transient
import java.io.Serializable

abstract class BaseDocumented<T : BaseModel>(
    @Transient
    override var ref: T? = null
) : BaseModel(), BaseDocumentedSchema<T> {

    init {
        val id = id(ref)
        val reg = registry.get()
        val stack = visiting.get()

        if (id != null && !reg.containsKey(id)) {
            stack.add(id)
            reg[id] = this

            ref?.let { src ->
                @Suppress("UNCHECKED_CAST")
                val typedNode = this as BaseDocumented<BaseModel>
                typedNode.apply(src, typedNode)
                typedNode.mapping(src)
            }

            stack.remove(id)
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

        inline fun <reified T : BaseModel, D : BaseDocumented<T>> from(
            source: T?,
            factory: (T) -> D
        ): D? {
            if (source == null) return null
            val id = id(source) ?: return null
            val reg = registry.get()
            val stack = visiting.get()

            if (id in stack) return null

            @Suppress("UNCHECKED_CAST")
            val existing = reg[id] as? D
            if (existing != null) return existing

            stack.add(id)
            val newObj = factory(source)
            reg[id] = newObj
            stack.remove(id)

            return newObj
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["ref"])
interface BaseDocumentedSchema<T : BaseModel> : Serializable {
    var ref: T?
    fun mapping(source: T): BaseDocumented<T>
}
