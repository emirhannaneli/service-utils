package net.lubble.util.spec

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Entity
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SpecOptions
import net.lubble.util.spec.tool.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import tools.jackson.databind.json.JsonMapper
import java.util.*

open class BaseSpec {
    /**
     * Abstract base class for JPA specifications.
     */
    abstract class JPA<T : BaseModel>(
        open val param: ParameterModel,
        override var options: SpecOptions = SpecOptions(),
        var fields: Collection<String>? = null,
        var joins: Collection<String>? = null,
    ) : SpecTool(param), JPATool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() {
                val type = (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0]

                return when (type) {
                    is Class<*> -> type as Class<T>
                    is ParameterizedType -> type.rawType as Class<T>
                    is TypeVariable<*> -> {
                        resolveEntityClassFromHierarchy() ?: throw IllegalStateException(
                            "Cannot resolve entity class for ${this::class}. " +
                                    "Please ensure your specification class properly extends BaseSpec.JPA with a concrete entity type."
                        )
                    }

                    else -> throw IllegalStateException("Cannot resolve generic type for ${this::class}")
                }
            }

        @Suppress("UNCHECKED_CAST")
        private fun resolveEntityClassFromHierarchy(): Class<T>? {
            var currentClass: Class<*>? = this::class.java

            while (currentClass != null) {
                val genericSuperclass = currentClass.genericSuperclass
                if (genericSuperclass is ParameterizedType) {
                    val typeArgs = genericSuperclass.actualTypeArguments
                    if (typeArgs.isNotEmpty()) {
                        val firstTypeArg = typeArgs[0]
                        when (firstTypeArg) {
                            is Class<*> -> {
                                if (firstTypeArg.isAnnotationPresent(Entity::class.java)) {
                                    return firstTypeArg as Class<T>
                                }
                            }

                            is ParameterizedType -> {
                                val rawType = firstTypeArg.rawType
                                if (rawType is Class<*> && rawType.isAnnotationPresent(Entity::class.java)) {
                                    return rawType as Class<T>
                                }
                            }

                            is TypeVariable<*> -> {
                                val bounds = firstTypeArg.bounds
                                for (bound in bounds) {
                                    when (bound) {
                                        is Class<*> -> {
                                            if (bound.isAnnotationPresent(Entity::class.java)) {
                                                return bound as Class<T>
                                            }
                                        }

                                        is ParameterizedType -> {
                                            val rawType = bound.rawType
                                            if (rawType is Class<*> && rawType.isAnnotationPresent(Entity::class.java)) {
                                                return rawType as Class<T>
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val typeParameters = currentClass.typeParameters
                for (typeParam in typeParameters) {
                    val bounds = typeParam.bounds
                    for (bound in bounds) {
                        when (bound) {
                            is Class<*> -> {
                                if (bound.isAnnotationPresent(Entity::class.java)) {
                                    return bound as Class<T>
                                }
                            }

                            is ParameterizedType -> {
                                val rawType = bound.rawType
                                if (rawType is Class<*> && rawType.isAnnotationPresent(Entity::class.java)) {
                                    return rawType as Class<T>
                                }
                            }
                        }
                    }
                }

                currentClass = currentClass.superclass
                if (currentClass?.name == "net.lubble.util.spec.BaseSpec\$JPA") {
                    break
                }
            }

            return null
        }

        fun toCacheKey(): String = generateCacheKey(this)
    }

    /**
     * Abstract base class for Mongo specifications.
     */
    abstract class Mongo<T : BaseModel>(
        open val param: ParameterModel,
        override var options: SpecOptions = SpecOptions(),
        var fields: Collection<String>? = null,
    ) : SpecTool(param), MongoTool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() = resolveSimpleGenericType(this::class.java)

        fun toCacheKey(): String = generateCacheKey(this)
    }

    /**
     * Abstract base class for Legacy Elastic specifications (Criteria API).
     */
    abstract class Elastic<T : BaseModel>(
        open val param: ParameterModel,
        override var options: SpecOptions = SpecOptions(),
        var fields: Collection<String>? = null
    ) : SpecTool(param), ElasticTool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() = resolveSimpleGenericType(this::class.java)

        fun toCacheKey(): String = generateCacheKey(this)
    }

    /**
     * Abstract base class for Native Elastic specifications (Java Client API).
     * Use this for 'co.elastic.clients' based queries.
     */
    abstract class ElasticNative<T : BaseModel>(
        open val param: ParameterModel,
        override var options: SpecOptions = SpecOptions(),
        var fields: Collection<String>? = null
    ) : SpecTool(param), ElasticNativeTool<T> {
        @Suppress("UNCHECKED_CAST")
        override val clazz: Class<T>
            get() = resolveSimpleGenericType(this::class.java)

        fun toCacheKey(): String = generateCacheKey(this)
    }

    companion object {
        /**
         * Mapper used only for cache keys. It is owned by the library (so it never touches the
         * application's shared mapper) and ignores `@JsonIgnoreProperties`, because paging and
         * sorting parameters hidden from API payloads still have to be part of the key.
         */
        private val cacheKeyMapper: ObjectMapper by lazy {
            JsonMapper.builder()
                .annotationIntrospector(object : JacksonAnnotationIntrospector() {
                    override fun findPropertyIgnoralByName(
                        config: MapperConfig<*>,
                        a: Annotated
                    ): JsonIgnoreProperties.Value = JsonIgnoreProperties.Value.empty()
                })
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build()
        }

        private fun generateCacheKey(obj: Any): String {
            val json = cacheKeyMapper.writeValueAsString(obj)
            return Base64.getEncoder().encodeToString(json.toByteArray())
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> resolveSimpleGenericType(clazz: Class<*>): Class<T> {
            val type = (clazz.genericSuperclass as ParameterizedType).actualTypeArguments[0]
            return when (type) {
                is Class<*> -> type as Class<T>
                is ParameterizedType -> type.rawType as Class<T>
                is TypeVariable<*> -> when (val bound = type.bounds.firstOrNull()) {
                    is Class<*> -> bound as Class<T>
                    is ParameterizedType -> bound.rawType as Class<T>
                    else -> BaseModel::class.java as Class<T>
                }
                else -> throw IllegalStateException("Cannot resolve generic type for $clazz")
            }
        }
    }
}