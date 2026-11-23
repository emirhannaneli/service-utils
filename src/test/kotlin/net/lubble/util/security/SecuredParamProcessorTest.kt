package net.lubble.util.security

import net.lubble.util.PageResponse
import net.lubble.util.Response
import net.lubble.util.annotation.security.PostAuthorizeParam
import net.lubble.util.annotation.security.PreAuthorizeParam
import net.lubble.util.annotation.security.SecuredParamProcessor
import net.lubble.util.controller.BaseController
import net.lubble.util.dto.RBase
import net.lubble.util.exception.AccessDenied
import net.lubble.util.model.ParameterModel
import org.aspectj.lang.JoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class SecuredParamProcessorTest {
    open class BaseCreate {
        @PreAuthorizeParam("name == 'admin'")
        var role: String? = null
    }

    class Create : BaseCreate() {
        var title: String? = null

        @PreAuthorizeParam("#discount <= 10 or name == 'admin'")
        var discount: Int? = null
    }

    class Broken {
        @PreAuthorizeParam("null")
        var field: String? = "x"
    }

    open class BaseRead : RBase() {
        @PostAuthorizeParam("name == 'admin'")
        var email: String? = null
    }

    class Child {
        @PostAuthorizeParam("name == 'admin'")
        var secret: String? = "child-secret"
    }

    class Read : BaseRead() {
        var title: String? = null

        @PostAuthorizeParam("name == 'admin'")
        var phone: String? = null

        @PostAuthorizeParam("name == 'admin'")
        var tags: List<String>? = null

        @PostAuthorizeParam("name == 'admin'")
        var attributes: Map<String, String>? = null

        @PostAuthorizeParam("name == 'admin'")
        var salary: Long? = null

        @PostAuthorizeParam("name == 'admin'")
        var score: Int = 0

        @PostAuthorizeParam("name == 'admin'")
        var active: Boolean = false

        var child: Child? = null
        var children: List<Child>? = null
        var childrenByKey: Map<String, Child>? = null
        var childArray: Array<Child>? = null
        var parent: Read? = null
    }

    private val processor = SecuredParamProcessor()

    private fun login(name: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(name, "n/a", listOf(SimpleGrantedAuthority("ROLE_USER")))
    }

    private fun joinPoint(vararg args: Any?): JoinPoint = mock(JoinPoint::class.java).also { `when`(it.args).thenReturn(args) }

    private fun read() = Read().apply {
        title = "Public"
        email = "çağrı.şükrü@example.com"
        phone = "5551234567"
        tags = listOf("vip")
        attributes = mapOf("k" to "v")
        salary = 9_000
        score = 7
        active = true
        child = Child()
        children = listOf(Child(), Child())
        childrenByKey = mapOf("a" to Child())
        childArray = arrayOf(Child())
    }

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    // region pre

    @Test
    fun `arguments without secured fields pass even when nobody is logged in`() {
        assertThatCode { processor.preAuthorizeParam(joinPoint("plain", 5, null, ParameterModel())) }.doesNotThrowAnyException()
    }

    @Test
    fun `secured fields that are not set are not checked`() {
        assertThatCode { processor.preAuthorizeParam(joinPoint(Create().apply { title = "x" })) }.doesNotThrowAnyException()
    }

    @Test
    fun `a secured field requires an authenticated user`() {
        val create = Create().apply { discount = 5 }

        assertThatThrownBy { processor.preAuthorizeParam(joinPoint(create)) }.isInstanceOf(AccessDenied::class.java)

        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        assertThatThrownBy { processor.preAuthorizeParam(joinPoint(create)) }.isInstanceOf(AccessDenied::class.java)
    }

    @Test
    fun `the expression decides and can read the field value`() {
        login("user")

        assertThatCode { processor.preAuthorizeParam(joinPoint(Create().apply { discount = 10 })) }.doesNotThrowAnyException()
        assertThatThrownBy { processor.preAuthorizeParam(joinPoint(Create().apply { discount = 50 })) }
            .isInstanceOf(AccessDenied::class.java)

        login("admin")
        assertThatCode { processor.preAuthorizeParam(joinPoint(Create().apply { discount = 50 })) }.doesNotThrowAnyException()
    }

    @Test
    fun `secured fields inherited from a superclass are enforced`() {
        login("user")

        assertThatThrownBy { processor.preAuthorizeParam(joinPoint(Create().apply { role = "ADMIN" })) }
            .isInstanceOf(AccessDenied::class.java)

        login("admin")
        assertThatCode { processor.preAuthorizeParam(joinPoint(Create().apply { role = "ADMIN" })) }.doesNotThrowAnyException()
    }

    @Test
    fun `an expression that does not evaluate to true denies access`() {
        login("admin")

        assertThatThrownBy { processor.preAuthorizeParam(joinPoint(Broken())) }.isInstanceOf(AccessDenied::class.java)
    }

    // endregion

    // region post

    @Test
    fun `permitted users see everything`() {
        login("admin")
        val read = read()

        processor.postAuthorizeParam(ResponseEntity.ok(read))

        assertThat(read.email).isEqualTo("çağrı.şükrü@example.com")
        assertThat(read.phone).isEqualTo("5551234567")
        assertThat(read.tags).containsExactly("vip")
        assertThat(read.salary).isEqualTo(9_000)
        assertThat(read.child!!.secret).isEqualTo("child-secret")
    }

    @Test
    fun `secured fields are censored for users without permission`() {
        login("user")
        val read = read()

        processor.postAuthorizeParam(ResponseEntity.ok(read))

        assertThat(read.title).isEqualTo("Public")
        assertThat(read.phone).isEqualTo("555*******")
        assertThat(read.tags).isEmpty()
        assertThat(read.attributes).isEmpty()
        assertThat(read.salary).isNull()
        assertThat(read.score).isZero()
        assertThat(read.active).isFalse()
    }

    @Test
    fun `masking covers non ascii letters and inherited fields`() {
        login("user")
        val read = read()

        processor.postAuthorizeParam(read)

        // First three characters stay, every later letter or digit of any script is masked.
        assertThat(read.email).isEqualTo("çağ**.*****@*******.***")
    }

    @Test
    fun `short strings are fully masked`() {
        login("user")
        val read = Read().apply { phone = "ab1" }

        processor.postAuthorizeParam(read)

        assertThat(read.phone).isEqualTo("***")
    }

    @Test
    fun `nested objects collections maps and arrays are censored`() {
        login("user")
        val read = read()

        processor.postAuthorizeParam(ResponseEntity.ok(read))

        val masked = "chi**-******"
        assertThat(read.child!!.secret).isEqualTo(masked)
        assertThat(read.children!!.map { it.secret }).containsExactly(masked, masked)
        assertThat(read.childrenByKey!!.getValue("a").secret).isEqualTo(masked)
        assertThat(read.childArray!![0].secret).isEqualTo(masked)
    }

    @Test
    fun `page items and top level collections are censored`() {
        login("user")
        val first = read()
        val second = read()
        val third = read()

        processor.postAuthorizeParam(PageResponse(1, 2, 2, 1, hasNext = false, hasPrevious = false, items = listOf(first, second)).build())
        processor.postAuthorizeParam(listOf(third))

        assertThat(listOf(first, second, third).map { it.phone }).containsOnly("555*******")
    }

    @Test
    fun `unauthenticated and anonymous callers get censored data instead of an error`() {
        val read = read()
        processor.postAuthorizeParam(ResponseEntity.ok(read))
        assertThat(read.phone).isEqualTo("555*******")

        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        val anonymous = read()
        processor.postAuthorizeParam(anonymous)
        assertThat(anonymous.salary).isNull()
    }

    @Test
    fun `cyclic object graphs terminate`() {
        login("user")
        val a = read()
        val b = read()
        a.parent = b
        b.parent = a

        processor.postAuthorizeParam(a)

        assertThat(a.phone).isEqualTo("555*******")
        assertThat(b.phone).isEqualTo("555*******")
    }

    @Test
    fun `results without secured fields and empty bodies are left alone`() {
        assertThatCode {
            processor.postAuthorizeParam(ResponseEntity.noContent().build<Any>())
            processor.postAuthorizeParam(Response("ok").build())
            processor.postAuthorizeParam("plain")
        }.doesNotThrowAnyException()
    }

    // endregion

    @Test
    fun `the aspect intercepts BaseController implementations`() {
        class Controller : BaseController<Create, Create, Read, ParameterModel> {
            override fun create(create: Create) = ok(read())
            override fun findById(id: String) = ok(read())
            override fun findAll(param: ParameterModel) = paged(listOf(read()), param.ofPageable(), 1)
            override fun update(id: String, update: Create) = ok(read())
            override fun delete(id: String) = message("deleted")
        }

        val factory = AspectJProxyFactory(Controller())
        factory.addAspect(processor)
        val controller = factory.getProxy<BaseController<Create, Create, Read, ParameterModel>>()

        login("user")
        assertThatThrownBy { controller.create(Create().apply { role = "ADMIN" }) }.isInstanceOf(AccessDenied::class.java)
        assertThat(controller.findById("1").body!!.phone).isEqualTo("555*******")
        assertThat((controller.findAll(ParameterModel()).body!!.items.single() as Read).phone).isEqualTo("555*******")

        login("admin")
        assertThat(controller.create(Create().apply { role = "ADMIN" }).body!!.phone).isEqualTo("5551234567")
    }
}
