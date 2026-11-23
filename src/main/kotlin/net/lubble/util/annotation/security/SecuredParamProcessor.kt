package net.lubble.util.annotation.security

import net.lubble.util.exception.AccessDenied
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Enforces [PreAuthorizeParam] on controller arguments and [PostAuthorizeParam] on controller results.
 *
 * Both checks fail closed: a missing or anonymous authentication, or an expression that does not
 * evaluate to `true`, denies the request (pre) or censors the field (post).
 */
@Aspect
@Component
@ConditionalOnClass(SecurityFilterChain::class)
class SecuredParamProcessor {

    /** Reflection data of one class, resolved once. Fields of superclasses are included. */
    private class Secured(
        val pre: List<Pair<Field, String>>,
        val post: List<Pair<Field, String>>,
        /** Fields whose values may themselves hold annotated fields. */
        val nested: List<Field>,
    )

    private val parser = SpelExpressionParser()
    private val expressions = ConcurrentHashMap<String, Expression>()

    private val metadata = object : ClassValue<Secured>() {
        override fun computeValue(type: Class<*>): Secured {
            if (isOpaque(type)) return EMPTY

            val pre = mutableListOf<Pair<Field, String>>()
            val post = mutableListOf<Pair<Field, String>>()
            val nested = mutableListOf<Field>()

            var current: Class<*>? = type
            while (current != null && !isOpaque(current)) {
                for (field in current.declaredFields) {
                    if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue
                    if (!field.trySetAccessible()) continue

                    field.getAnnotation(PreAuthorizeParam::class.java)?.let { pre += field to it.value }
                    field.getAnnotation(PostAuthorizeParam::class.java)?.let { post += field to it.value }

                    val fieldType = field.type
                    if (!fieldType.isPrimitive && !fieldType.isEnum && fieldType != String::class.java) nested += field
                }
                current = current.superclass
            }
            return if (pre.isEmpty() && post.isEmpty() && nested.isEmpty()) EMPTY else Secured(pre, post, nested)
        }
    }

    /**
     * This method is executed before the execution of any method in a class that extends BaseController.
     * It checks if the authenticated user has the necessary permissions to access the fields annotated with @PreAuthorizeParam.
     * If the user is not authenticated or does not have the necessary permissions, it throws an AccessDenied exception.
     * @param point The join point representing the method execution.
     */
    @Before("target(net.lubble.util.controller.BaseController)")
    fun preAuthorizeParam(point: JoinPoint) {
        var evaluation: Evaluation? = null

        for (arg in point.args) {
            if (arg == null) continue
            for ((field, expression) in metadata.get(arg.javaClass).pre) {
                val value = field.get(arg) ?: continue
                val current = evaluation ?: Evaluation().also { evaluation = it }
                current.context.setVariable(field.name, value)
                if (!current.permits(expression)) throw AccessDenied()
            }
        }
    }

    /**
     * This method is executed after the execution of any method in a class that extends BaseController.
     * It censors the fields annotated with @PostAuthorizeParam if the authenticated user does not have the necessary permissions.
     * @param result The result of the method execution.
     */
    @AfterReturning(pointcut = "target(net.lubble.util.controller.BaseController)", returning = "result")
    fun postAuthorizeParam(result: Any) {
        val body = (if (result is ResponseEntity<*>) result.body else result) ?: return
        sanitize(body, Sanitization())
    }

    private fun sanitize(obj: Any, state: Sanitization) {
        val type = obj.javaClass
        when {
            // Only plain JDK/Kotlin containers are walked. Anything else (a Hibernate PersistentBag,
            // a lazy proxy) could hit the database or fail outside of a session when iterated.
            obj is Array<*> -> if (state.visited.add(obj)) obj.forEach { item -> item?.let { sanitize(it, state) } }

            isOpaque(type) -> when (obj) {
                is Iterable<*> if isStandardContainer(type) ->
                    if (state.visited.add(obj)) obj.forEach { item -> item?.let { sanitize(it, state) } }

                is Map<*, *> if isStandardContainer(type) ->
                    if (state.visited.add(obj)) obj.values.forEach { item -> item?.let { sanitize(it, state) } }
            }

            else -> {
                val secured = metadata.get(type)
                if (secured === EMPTY || !state.visited.add(obj)) return

                for (field in secured.nested) {
                    field.get(obj)?.let { sanitize(it, state) }
                }
                for ((field, expression) in secured.post) {
                    if (field.get(obj) == null) continue
                    if (!state.evaluation.permits(expression)) censorField(field, obj)
                }
            }
        }
    }

    /**
     * This method censors a field of an object.
     * If the field is a String, it replaces every letter and digit after the third character with '*'.
     * If the field is a collection or map, it replaces it with an empty one.
     * Otherwise it resets the field to null (or the zero value for primitives).
     * @param field The field to censor.
     * @param obj The object that contains the field.
     */
    private fun censorField(field: Field, obj: Any) {
        val value = field.get(obj) ?: return
        val type = field.type
        val censored: Any? = when {
            type == String::class.java -> {
                val str = value as String
                val masked = str.replace(CENSORED_CHARS, "*")
                if (str.length > 3) str.substring(0, 3) + masked.substring(3) else masked
            }

            type == java.lang.Boolean.TYPE -> false
            type == Character.TYPE -> '\u0000'
            type == java.lang.Byte.TYPE -> 0.toByte()
            type == java.lang.Short.TYPE -> 0.toShort()
            type == Integer.TYPE -> 0
            type == java.lang.Long.TYPE -> 0L
            type == java.lang.Float.TYPE -> 0f
            type == java.lang.Double.TYPE -> 0.0
            type.isInstance(EMPTY_LIST) -> EMPTY_LIST
            type.isInstance(EMPTY_SET) -> EMPTY_SET
            type.isInstance(EMPTY_MAP) -> EMPTY_MAP
            else -> null
        }
        field.set(obj, censored)
    }

    /** The authentication of the current request plus the SpEL context built from it. */
    private inner class Evaluation {
        private val auth: Authentication? = SecurityContextHolder.getContext().authentication
        private val authenticated = auth != null && auth !is AnonymousAuthenticationToken && auth.isAuthenticated

        val context: StandardEvaluationContext by lazy {
            StandardEvaluationContext(auth).apply {
                setVariable("auth", auth)
                setVariable("authentication", auth)
            }
        }

        fun permits(expression: String): Boolean {
            if (!authenticated) return false
            val parsed = expressions.computeIfAbsent(expression, parser::parseExpression)
            return parsed.getValue(context, Boolean::class.javaObjectType) == true
        }
    }

    private inner class Sanitization {
        val visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
        val evaluation: Evaluation by lazy { Evaluation() }
    }

    private companion object {
        val EMPTY = Secured(emptyList(), emptyList(), emptyList())

        val EMPTY_LIST = emptyList<Any>()
        val EMPTY_SET = emptySet<Any>()
        val EMPTY_MAP = emptyMap<Any, Any>()

        /** Every letter and digit of any script, so non-ASCII text (ç, ş, ğ, ...) is masked too. */
        val CENSORED_CHARS = Regex("[\\p{L}\\p{N}]")

        /** Packages whose classes are never reflected into: they cannot carry the annotations. */
        val OPAQUE_PACKAGES = arrayOf(
            "java.", "javax.", "jdk.", "sun.", "kotlin.", "kotlinx.", "jakarta.",
            "org.hibernate.", "org.springframework.", "tools.jackson.", "com.fasterxml.",
        )

        fun isOpaque(type: Class<*>): Boolean =
            type.isPrimitive || type.isEnum || OPAQUE_PACKAGES.any { type.name.startsWith(it) }

        fun isStandardContainer(type: Class<*>): Boolean =
            type.name.startsWith("java.") || type.name.startsWith("kotlin.")
    }
}
