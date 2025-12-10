package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.lubble.util.DocumentRegistryHolder
import org.springframework.data.annotation.Transient
import java.io.Serializable

abstract class BaseDocumented<T : BaseModel>(
    @Transient
    override var ref: T? = null
) : BaseModel(), BaseDocumentedSchema<T> {
    init {
        ref?.let { from(it, ::map) }
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseDocumented<*>

        return ref == other.ref
    }

    override fun hashCode(): Int {
        return ref?.hashCode() ?: 0
    }

    companion object {
        
        inline fun <R> mapSession(block: () -> R): R {
            try {
                DocumentRegistryHolder.createContext()
                return block()
            } finally {
                DocumentRegistryHolder.closeContext()
            }
        }

        fun <T : BaseModel, D : BaseDocumented<T>> from(
            source: T?,
            mapper: (T) -> D
        ): D? {
            source ?: return null

            val context = DocumentRegistryHolder.current() ?: return null
            val id = source.getId()

            @Suppress("UNCHECKED_CAST")
            val existing = context.registry[id] as? D
            if (existing != null) {
                return existing
            }

            if (!context.visiting.add(id)) {
                return null
            }

            try {
                val documented = mapper(source)
                documented.apply(source, documented)
                documented.mapping(source)
                
                documented.ref = null
                
                context.registry[id] = documented
                
                return documented
            } finally {
                context.visiting.remove(id)
            }
        }
    }


}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["ref"])
interface BaseDocumentedSchema<T : BaseModel> : Serializable {
    var ref: T?
    fun mapping(source: T): BaseDocumented<T>
}
