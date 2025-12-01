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
import jakarta.persistence.Entity
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
                    is TypeVariable<*> -> {
                        // TypeVariable durumunda, class hierarchy'sini tarayarak gerçek entity sınıfını bul
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
            // Class hierarchy'sini tarayarak gerçek entity sınıfını bul
            var currentClass: Class<*>? = this::class.java
            
            while (currentClass != null) {
                // Generic superclass'ı kontrol et
                val genericSuperclass = currentClass.genericSuperclass
                if (genericSuperclass is ParameterizedType) {
                    val typeArgs = genericSuperclass.actualTypeArguments
                    if (typeArgs.isNotEmpty()) {
                        val firstTypeArg = typeArgs[0]
                        when (firstTypeArg) {
                            is Class<*> -> {
                                // Entity annotation'ı kontrol et
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
                                // TypeVariable durumunda, upper bound'ları kontrol et
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
                
                // Ayrıca, mevcut class'ın generic type parameter'larını da kontrol et
                // (örneğin ProductSpec<T : Product> durumunda)
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
                
                // Superclass'a geç
                currentClass = currentClass.superclass
                
                // BaseSpec.JPA'ya ulaştıysak dur
                if (currentClass?.name == "net.lubble.util.spec.BaseSpec\$JPA") {
                    break
                }
            }
            
            return null
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
