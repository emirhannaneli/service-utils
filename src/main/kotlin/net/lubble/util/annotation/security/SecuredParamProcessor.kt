package net.lubble.util.annotation.security

import net.lubble.util.PageResponse
import net.lubble.util.exception.AccessDenied
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component
import java.lang.reflect.Field

@Aspect
@Component
@ConditionalOnClass(SecurityFilterChain::class)
class SecuredParamProcessor {

    /**
     * Pre-authorize parameters before the execution of the target method.
     *
     * @param point the join point representing the method execution
     * @throws AccessDenied if the user does not have the required permissions
     */
    @Before("target(net.lubble.util.controller.BaseController)")
    fun preAuthorizeParam(point: JoinPoint) {
        val auth = SecurityContextHolder.getContext().authentication
        val context = StandardEvaluationContext(auth)
        val parser = SpelExpressionParser()

        context.setVariable("auth", auth)
        context.setVariable("authentication", auth)

        point.args.forEach { arg ->
            arg::class.java.declaredFields.forEach { field ->
                val securedParam = field.getAnnotation(PreAuthorizeParam::class.java) ?: return@forEach

                field.isAccessible = true

                val value = field.get(arg) ?: return@forEach
                context.setVariable(field.name, value)

                if (auth !is AnonymousAuthenticationToken && auth.isAuthenticated)
                    parser.parseExpression(securedParam.value).getValue(context, Boolean::class.java)
                        ?.let { hasPermission ->
                            if (!hasPermission) throw AccessDenied()
                        }
                else throw AccessDenied()
            }
        }
    }

    /**
     * Post-authorize parameters after the execution of the target method.
     *
     * @param result the result of the method execution
     */
    @AfterReturning(pointcut = "target(net.lubble.util.controller.BaseController)", returning = "result")
    fun postAuthorizeParam(result: Any) {
        val auth = SecurityContextHolder.getContext().authentication
        val context = StandardEvaluationContext(auth)
        val parser = SpelExpressionParser()

        context.setVariable("auth", auth)
        context.setVariable("authentication", auth)

        if (result is ResponseEntity<*>) {
            result.body?.let { body ->
                if (body is PageResponse)
                    body.items.forEach { item ->
                        item?.let { sanitizeFields(it, context, parser) }
                    }
                else sanitizeFields(body, context, parser)
            }
        } else {
            sanitizeFields(result, context, parser)
        }
    }

    /**
     * Sanitize fields of the given object based on security annotations.
     *
     * @param obj the object to sanitize
     * @param context the evaluation context
     * @param parser the Spring-EL expression parser
     */
    private fun sanitizeFields(obj: Any, context: StandardEvaluationContext, parser: SpelExpressionParser) {
        val fields = obj::class.java.declaredFields

        fields.forEach { field ->
            runCatching { field.isAccessible = true }.onFailure { return@forEach }

            val value = field.get(obj) ?: return@forEach

            if (!field.type.isPrimitive && !field.type.isEnum && field.type != String::class.java)
                sanitizeFields(value, context, parser)

            val securedParam = field.getAnnotation(PostAuthorizeParam::class.java) ?: return@forEach
            val expression = securedParam.value

            val auth = context.lookupVariable("auth") as Authentication

            if (auth !is AnonymousAuthenticationToken && auth.isAuthenticated) {
                val hasPermission = parser.parseExpression(expression).getValue(context, Boolean::class.java) == true

                if (!hasPermission) censorField(field, obj)
            } else censorField(field, obj)
        }
    }

    /**
     * Censor the value of the given field in the object.
     *
     * @param field the field to censor
     * @param obj the object containing the field
     */
    private fun censorField(field: Field, obj: Any) {
        field.isAccessible = true
        val value = field.get(obj) ?: return
        when (field.type) {
            String::class.java -> {
                val str = value as String
                var censored = str.replace(Regex("[a-zA-Z0-9]"), "*")
                if (str.length > 3) censored = str.substring(0, 3) + censored.substring(3)
                field.set(obj, censored)
            }

            List::class.java -> field.set(obj, emptyList<Any>())
            Map::class.java -> field.set(obj, emptyMap<Any, Any>())
            else -> field.set(obj, null)
        }
    }
}