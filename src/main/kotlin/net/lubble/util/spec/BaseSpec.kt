package net.lubble.util.spec

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.spec.tool.ElasticTool
import net.lubble.util.spec.tool.JPATool
import net.lubble.util.spec.tool.MongoTool
import net.lubble.util.spec.tool.SpecTool
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.util.*

class BaseSpec {
    /**
     * Abstract base class for specifications.
     *
     * @param T the type of the JPA model
     * @param param the parameters for the specification
     * @param fields the fields to projection
     */
    abstract class JPA<T : BaseModel>(
        param: ParameterModel,
        var fields: Collection<String>? = null,
    ) : SpecTool(param), JPATool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() {
                val type = (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0]

                return when (type) {
                    is Class<*> -> type as Class<T>
                    is ParameterizedType -> type.rawType as Class<T>
                    is TypeVariable<*> -> BaseModel::class.java as Class<T>
                    else -> throw IllegalStateException("Cannot resolve generic type for ${this::class}")
                }
            }

        fun toCacheKey(): String {
            val mapper = AppContextUtil.bean(ObjectMapper::class.java).apply {
                setAnnotationIntrospector(object : JacksonAnnotationIntrospector() {
                    override fun findPropertyIgnoralByName(
                        config: MapperConfig<*>?,
                        a: Annotated?
                    ): JsonIgnoreProperties.Value {
                        return JsonIgnoreProperties.Value.empty()
                    }
                })
            }
            val json = mapper.writeValueAsString(this)
            return Base64.getEncoder().encodeToString(json.toByteArray())
        }
    }

    /**
     * Abstract base class for specifications.
     *
     * @param T the type of the Mongo model
     * @param param the parameters for the specification
     * @param fields the fields to projection
     */
    abstract class Mongo<T : BaseModel>(
        param: ParameterModel,
        var fields: Collection<String>? = null,
    ) : SpecTool(param), MongoTool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() {
                val type = (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0]

                return when (type) {
                    is Class<*> -> type as Class<T>
                    is ParameterizedType -> type.rawType as Class<T>
                    is TypeVariable<*> -> BaseModel::class.java as Class<T>
                    else -> throw IllegalStateException("Cannot resolve generic type for ${this::class}")
                }
            }

        fun toCacheKey(): String {
            val mapper = AppContextUtil.bean(ObjectMapper::class.java).apply {
                setAnnotationIntrospector(object : JacksonAnnotationIntrospector() {
                    override fun findPropertyIgnoralByName(
                        config: MapperConfig<*>?,
                        a: Annotated?
                    ): JsonIgnoreProperties.Value {
                        return JsonIgnoreProperties.Value.empty()
                    }
                })
            }
            val json = mapper.writeValueAsString(this)
            return Base64.getEncoder().encodeToString(json.toByteArray())
        }
    }

    abstract class Elastic<T : BaseModel>(
        param: ParameterModel,
        var fields: Collection<String>? = null
    ) : SpecTool(param), ElasticTool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() {
                val type = (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0]

                return when (type) {
                    is Class<*> -> type as Class<T>
                    is ParameterizedType -> type.rawType as Class<T>
                    is TypeVariable<*> -> BaseModel::class.java as Class<T>
                    else -> throw IllegalStateException("Cannot resolve generic type for ${this::class}")
                }
            }

        fun toCacheKey(): String {
            val mapper = AppContextUtil.bean(ObjectMapper::class.java).apply {
                setAnnotationIntrospector(object : JacksonAnnotationIntrospector() {
                    override fun findPropertyIgnoralByName(
                        config: MapperConfig<*>?,
                        a: Annotated?
                    ): JsonIgnoreProperties.Value {
                        return JsonIgnoreProperties.Value.empty()
                    }
                })
            }
            val json = mapper.writeValueAsString(this)
            return Base64.getEncoder().encodeToString(json.toByteArray())
        }
    }
}
