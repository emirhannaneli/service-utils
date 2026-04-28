package net.lubble.util.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.lubble.util.MapperRegistryHolder
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Tomcat reuses request threads, and [MapperRegistryHolder] keeps DTO entries on a
 * [ThreadLocal]. Without explicit cleanup, a previous request's mapped DTOs stay
 * cached on the thread and the next request's [net.lubble.util.mapper.BaseMapper.map]
 * call may return a stale cached DTO instead of re-mapping the freshly loaded entity.
 *
 * This filter runs as early as possible and clears the registry both before and after
 * every request to guarantee a clean state regardless of how the previous request
 * exited (success, exception, async dispatch, etc.).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MapperRegistryCleanupFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        MapperRegistryHolder.clear()
        try {
            filterChain.doFilter(request, response)
        } finally {
            MapperRegistryHolder.clear()
        }
    }
}
