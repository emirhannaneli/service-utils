package net.lubble.util.controller

import graphql.language.Field
import graphql.language.InlineFragment
import graphql.language.SelectionSet
import graphql.schema.DataFetchingEnvironment
import net.lubble.util.GraphPageResponse
import net.lubble.util.GraphResponse
import net.lubble.util.LK
import net.lubble.util.Response
import net.lubble.util.controller.reactive.ReactiveBaseController
import net.lubble.util.dto.BulkFailure
import net.lubble.util.dto.BulkIdRequest
import net.lubble.util.dto.RBase
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

class ControllerTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun context() {
            TestContext.install(mapOf("item.deleted" to "Silindi"))
        }

        @JvmStatic
        @AfterAll
        fun reset() = TestContext.reset()
    }

    private val rest = object : BaseController<Any, Any, RBase, ParameterModel> {
        override fun create(create: Any) = created(RBase().apply { pk = 1 }, "/items/1")
        override fun findById(id: String) = ok(RBase().apply { pk = id.toLong() })
        override fun findAll(param: ParameterModel) = paged(listOf(1, 2), param.ofPageable(), 12)
        override fun update(id: String, update: Any) = ok(RBase())
        override fun delete(id: String) = message("item.deleted")
    }

    private val graph = object : BaseGraphController<Any, Any, RBase, ParameterModel> {
        override fun create(input: Any) = RBase()
        override fun findById(id: String, env: DataFetchingEnvironment) = RBase()
        override fun findAll(options: ParameterModel?, env: DataFetchingEnvironment) = GraphPageResponse.empty()
        override fun update(id: String, input: Any) = RBase()
        override fun delete(id: String) = message("item.deleted")
    }

    private val reactive = object : ReactiveBaseController<Any, Any, RBase, ParameterModel> {
        override fun create(create: Any) = created(RBase().apply { pk = 1 }, "/items/1")
        override fun findById(id: String) = ok(RBase())
        override fun findAll(param: ParameterModel) = paged(PageImpl(listOf(1), PageRequest.of(0, 1), 3), listOf("one"))
        override fun update(id: String, update: Any) = ok(RBase())
        override fun delete(id: String) = message("item.deleted")
    }

    @Test
    fun `rest helpers build the expected responses`() {
        val created = rest.create(Any())
        assertThat(created.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(created.headers.location.toString()).isEqualTo("/items/1")
        assertThat(rest.findById("7").body!!.pk).isEqualTo(7)
        assertThat(rest.ok(true).body).isTrue()

        val page = rest.findAll(ParameterModel().apply { page = 2; size = 5 }).body!!
        assertThat(page.current).isEqualTo(2)
        assertThat(page.totalPages).isEqualTo(3)
        assertThat(rest.paged(PageImpl(listOf(1)), listOf("x")).body!!.items).containsExactly("x")
        assertThat(rest.delete("1").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `optional rest operations are unsupported until overridden`() {
        val param = ParameterModel()
        val bulk = BulkIdRequest(listOf("1"))
        listOf<() -> Any>(
            { rest.findAllArchived(param) }, { rest.archive("1") }, { rest.unarchive("1") }, { rest.recycleBin(param) },
            { rest.clearRecycleBin() }, { rest.restore("1") }, { rest.deletePermanently("1") }, { rest.bulkDelete(bulk) },
            { rest.bulkArchive(bulk) }, { rest.bulkUnarchive(bulk) }, { rest.bulkRestore(bulk) }, { rest.bulkDeletePermanently(bulk) },
        ).forEach { assertThatThrownBy { it() }.isInstanceOf(UnsupportedOperationException::class.java) }
    }

    @Test
    fun `bulk response separates processed ids from missing ones`() {
        val first = BaseModel(11L, LK("aaaaa-11111-aaaaa"))
        val second = BaseModel(22L, LK("bbbbb-22222-bbbbb"))

        val response = rest.bulkResponseOf(listOf("11", "bbbbb-22222-bbbbb", "33", "ccccc-33333-ccccc", "011"), listOf(first, second)).body!!

        assertThat(response.successIds).containsExactly("11", "bbbbb-22222-bbbbb", "011")
        assertThat(response.failed).containsExactly(BulkFailure("33", "not_found"), BulkFailure("ccccc-33333-ccccc", "not_found"))
    }

    @Test
    fun `bulk response of nothing is empty`() {
        val response = rest.bulkResponseOf(emptyList(), emptyList()).body!!

        assertThat(response.successIds).isEmpty()
        assertThat(response.failed).isEmpty()
        assertThat(BulkIdRequest().ids).isEmpty()
    }

    @Test
    fun `graph helpers build the expected responses`() {
        assertThat(graph.delete("1").message).isEqualTo("Silindi")
        assertThat(graph.paged(PageImpl(listOf(1), PageRequest.of(0, 1), 3), listOf("one")).meta.totalPages).isEqualTo(3)
        assertThat(graph.paged(listOf("one"), PageRequest.of(0, 1), 3).data).containsExactly("one")
    }

    @Test
    fun `optional graph operations are unsupported until overridden`() {
        val env = mock(DataFetchingEnvironment::class.java)
        listOf<() -> Any>(
            { graph.findAllArchived(null, env) }, { graph.archive("1") }, { graph.unarchive("1") }, { graph.recycleBin(null, env) },
            { graph.clearRecycleBin() }, { graph.restore("1") }, { graph.deletePermanently("1") },
        ).forEach { assertThatThrownBy { it() }.isInstanceOf(UnsupportedOperationException::class.java) }
    }

    private fun field(name: String, vararg children: graphql.language.Selection<*>): Field =
        Field.newField(name).apply { if (children.isNotEmpty()) selectionSet(SelectionSet(children.toList())) }.build()

    private fun environment(root: Field): DataFetchingEnvironment =
        mock(DataFetchingEnvironment::class.java).also { `when`(it.field).thenReturn(root) }

    @Test
    fun `query fields flattens the selection below data into dotted paths`() {
        val root = field(
            "users",
            field("meta", field("current")),
            field(
                "data",
                field("pk"),
                field("address", field("city"), field("country", field("code"))),
                InlineFragment.newInlineFragment().selectionSet(SelectionSet(listOf(field("email")))).build(),
            ),
        )

        assertThat(graph.queryFields(environment(root))).containsExactly("pk", "address.city", "address.country.code", "email")
    }

    @Test
    fun `query fields is empty without a data selection`() {
        assertThat(graph.queryFields(environment(field("users", field("meta", field("current")))))).isEmpty()
        assertThat(graph.queryFields(environment(field("users", field("data"))))).isEmpty()
    }

    @Test
    fun `reactive helpers wrap responses in monos`() {
        val created = reactive.create(Any()).block()!!
        assertThat(created.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(created.headers.location.toString()).isEqualTo("/items/1")
        assertThat(reactive.ok(true).block()!!.body).isTrue()
        assertThat(reactive.findAll(ParameterModel()).block()!!.body!!.totalItems).isEqualTo(3)
        assertThat(reactive.delete("1").block()!!.body).isInstanceOf(Response::class.java)
    }

    @Test
    fun `optional reactive operations are unsupported until overridden`() {
        val param = ParameterModel()
        listOf<() -> Mono<*>>(
            { reactive.findAllArchived(param) }, { reactive.archive("1") }, { reactive.unarchive("1") }, { reactive.recycleBin(param) },
            { reactive.clearRecycleBin() }, { reactive.restore("1") }, { reactive.deletePermanently("1") },
        ).forEach { assertThatThrownBy { it() }.isInstanceOf(UnsupportedOperationException::class.java) }
    }

    @Test
    fun `graph response type is usable as a plain value`() {
        assertThat(GraphResponse(message = "raw", code = null, details = null).message).isEqualTo("raw")
    }
}
