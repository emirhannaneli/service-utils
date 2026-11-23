package net.lubble.util.spec

import net.lubble.util.model.ParameterModel
import net.lubble.util.projection.LElasticNativeProjection
import net.lubble.util.projection.LElasticProjection
import net.lubble.util.projection.elasticSourceFilter
import net.lubble.util.spec.QueryToolsTest.ElasticSpec
import net.lubble.util.spec.QueryToolsTest.NativeSpec
import net.lubble.util.spec.QueryToolsTest.Product
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Query

class ElasticProjectionTest {
    private val required = arrayOf("id", "pk", "sk", "createdAt", "updatedAt")
    private val operations = mock(ElasticsearchOperations::class.java)

    @BeforeEach
    fun context() {
        @Suppress("UNCHECKED_CAST")
        val hits = mock(SearchHits::class.java) as SearchHits<Product>
        `when`(hits.searchHits).thenReturn(emptyList())
        `when`(hits.iterator()).thenReturn(mutableListOf<org.springframework.data.elasticsearch.core.SearchHit<Product>>().iterator())
        `when`(operations.search(any(Query::class.java), eq(Product::class.java))).thenReturn(hits)
        TestContext.install { it.beanFactory.registerSingleton("elasticsearchOperations", operations) }
    }

    @AfterEach
    fun reset() = TestContext.reset()

    private fun executed(): Query = ArgumentCaptor.forClass(Query::class.java)
        .also { verify(operations).search(it.capture(), eq(Product::class.java)) }
        .value

    @Test
    fun `without a field list only the identifying and audit fields are loaded`() {
        assertThat(elasticSourceFilter(null).includes).containsExactlyInAnyOrder(*required)
        assertThat(elasticSourceFilter(emptyList()).includes).containsExactlyInAnyOrder(*required)
        assertThat(elasticSourceFilter(null).excludes).isEmpty()
    }

    @Test
    fun `requested fields are loaded in addition to the required ones`() {
        assertThat(elasticSourceFilter(listOf("title", "pk")).includes).containsExactlyInAnyOrder(*required, "title")
    }

    @Test
    fun `criteria projection restricts the source the same way as the native one`() {
        val criteria = object : LElasticProjection<Product> {}
        val native = object : LElasticNativeProjection<Product> {}

        criteria.findOne(ElasticSpec(ParameterModel()))
        assertThat(executed().sourceFilter!!.includes).containsExactlyInAnyOrder(*required)

        org.mockito.Mockito.clearInvocations(operations)
        native.findOne(NativeSpec(ParameterModel()))
        assertThat(executed().sourceFilter!!.includes).containsExactlyInAnyOrder(*required)
    }

    @Test
    fun `criteria projection passes requested fields through`() {
        object : LElasticProjection<Product> {}.findOne(ElasticSpec(ParameterModel()).apply { fields = listOf("title") })

        assertThat(executed().sourceFilter!!.includes).containsExactlyInAnyOrder(*required, "title")
    }
}
