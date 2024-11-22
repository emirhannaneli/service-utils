package net.lubble.util.annotation.security

import net.lubble.util.exception.AccessDenied
import net.lubble.util.model.BaseJPAModel
import net.lubble.util.model.BaseMongoModel
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class SecuredParamProcessor() {

    @Before("target(net.lubble.util.controller.BaseController)")
    fun preAuthorizeParam(point: JoinPoint) {
        val auth = SecurityContextHolder.getContext().authentication
        val context = StandardEvaluationContext(auth)
        val parser = SpelExpressionParser()

        context.setVariable("auth", auth)
        context.setVariable("authentication", auth)

        val args = point.args.filter { arg ->
            arg is BaseJPAModel.SearchParams || arg is BaseMongoModel.SearchParams
        }

        args.forEach { arg ->
            arg::class.java.declaredFields.forEach { field ->
                field.isAccessible = true

                val value = field.get(arg) ?: return@forEach
                context.setVariable(field.name, value)

                val securedParam = field.getAnnotation(PreAuthorizeParam::class.java)
                val expression = securedParam?.value ?: return@forEach

                if (auth !is AnonymousAuthenticationToken && auth.isAuthenticated)
                    parser.parseExpression(expression).getValue(context, Boolean::class.java)?.let { hasPermission ->
                        if (!hasPermission) throw AccessDenied()
                    }
                else throw AccessDenied()
            }
        }
    }

    @After("target(net.lubble.util.controller.BaseController)")
    fun postAuthorizeParam(point: JoinPoint) {
        val auth = SecurityContextHolder.getContext().authentication
        val context = StandardEvaluationContext(auth)
        val parser = SpelExpressionParser()

        context.setVariable("auth", auth)
        context.setVariable("authentication", auth)

        val args = point.args.filter { arg ->
            arg is BaseJPAModel.SearchParams || arg is BaseMongoModel.SearchParams
        }

        args.forEach { arg ->
            arg::class.java.declaredFields.forEach { field ->
                field.isAccessible = true

                val value = field.get(arg) ?: return@forEach
                context.setVariable(field.name, value)

                val securedParam = field.getAnnotation(PostAuthorizeParam::class.java)
                val expression = securedParam?.value ?: return@forEach

                if (auth !is AnonymousAuthenticationToken && auth.isAuthenticated)
                    parser.parseExpression(expression).getValue(context, Boolean::class.java)?.let { hasPermission ->
                        if (!hasPermission) throw AccessDenied()
                    }
                else throw AccessDenied()
            }
        }
    }
}