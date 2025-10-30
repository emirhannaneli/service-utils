package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

    companion object {
        val VISITED: ThreadLocal<MutableSet<String>> = ThreadLocal()

        inline fun <R> mapSession(block: () -> R): R {
            try {
                VISITED.set(mutableSetOf())
                return block()
            } finally {
                VISITED.remove()
            }
        }

        fun <T : BaseModel, D : BaseDocumented<T>> from(
            source: T?,
            mapper: (T) -> D
        ): D? {
            source ?: return null
            val visited = VISITED.get() ?: mutableSetOf<String>().also { VISITED.set(it) }
            val id = source.getId()

            if (!visited.add(id)) return null

            val documented = mapper(source)
            documented.apply(source, documented)
            documented.mapping(source)
            return documented
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["ref"])
interface BaseDocumentedSchema<T : BaseModel> : Serializable {
    var ref: T?
    fun mapping(source: T): BaseDocumented<T>
}
