package net.lubble.util.spec

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import java.util.*

class BaseSpec {
    /**
     * Abstract base class for specifications.
     *
     * @param T the type of the JPA model
     * @param param the parameters for the specification
     * @param fields the fields to projection
     */
    abstract class JPA<T : BaseModel>(param: ParameterModel, val fields: Collection<String>? = null) :
        SpecTool(param),
        SpecTool.JPAModel<T> {
        fun toKey(): String {
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
    abstract class Mongo<T : BaseModel>(param: ParameterModel, val fields: Collection<String>? = null) :
        SpecTool(param),
        SpecTool.MongoModel<T> {
        fun toKey(): String {
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
