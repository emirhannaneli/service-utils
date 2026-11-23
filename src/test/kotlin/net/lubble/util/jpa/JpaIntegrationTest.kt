package net.lubble.util.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOptions
import net.lubble.util.model.SpecOptions
import net.lubble.util.repo.BaseRepo
import net.lubble.util.spec.BaseSpec
import net.lubble.util.spec.tool.SpecTool.SearchType
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@Entity
@Table(name = "authors")
open class Author : BaseModel() {
    open var name: String = ""
}

@Entity
@Table(name = "books")
open class Book : BaseModel() {
    open var title: String = ""
    open var pages: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    open var author: Author? = null
}

interface AuthorRepo : BaseRepo.JPA<Author>
interface BookRepo : BaseRepo.JPA<Book>

class BookSpec(
    param: ParameterModel = ParameterModel(),
    private val searchType: SearchType = SearchType.LIKE,
) : BaseSpec.JPA<Book>(param) {
    override var id: String? = null
    override var ids: List<String>? = null
    override var deleted: Boolean? = null
    override var archived: Boolean? = null

    override fun ofSearch(): Specification<Book> = Specification { root, query, builder ->
        var predicate = defaultPredicates(root, query, builder, param)
        param.search?.let { predicate = searchPredicate(predicate, builder, root, it, searchType, "title") }
        predicate
    }
}

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = [BookRepo::class])
open class JpaTestConfig {
    @Bean
    open fun dataSource(): DataSource = DriverManagerDataSource("jdbc:h2:mem:lubble;DB_CLOSE_DELAY=-1")

    @Bean
    open fun entityManagerFactory(dataSource: DataSource) = LocalContainerEntityManagerFactoryBean().apply {
        this.dataSource = dataSource
        setPackagesToScan("net.lubble.util.jpa", "net.lubble.util.converter")
        jpaVendorAdapter = HibernateJpaVendorAdapter()
        setJpaPropertyMap(mapOf("hibernate.hbm2ddl.auto" to "create-drop"))
    }

    @Bean
    open fun transactionManager(entityManagerFactory: EntityManagerFactory) = JpaTransactionManager(entityManagerFactory)
}

@SpringJUnitConfig(JpaTestConfig::class)
@Transactional
class JpaIntegrationTest {
    companion object {
        @JvmStatic
        @AfterAll
        fun reset() = TestContext.reset()
    }

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var books: BookRepo

    @Autowired
    lateinit var authors: AuthorRepo

    private lateinit var saved: Map<String, Book>

    @BeforeEach
    fun seed() {
        AppContextUtil.initialize(context)
        val ada = authors.save(Author().apply { name = "Ada" })
        val zed = authors.save(Author().apply { name = "Zed" })
        saved = listOf(
            Triple("Kotlin in Action", 300, zed),
            Triple("Effective Java", 400, ada),
            Triple("100% Pure", 100, ada),
            Triple("Under_score", 200, zed),
            Triple("Clean Code", 500, ada),
        ).associate { (title, pages, author) ->
            title to books.save(Book().apply { this.title = title; this.pages = pages; this.author = author })
        }
        books.flush()
    }

    private fun param(configure: ParameterModel.() -> Unit = {}) = ParameterModel().apply(configure)

    private fun titles(spec: BookSpec) = books.findAll(spec).content.map { it.title }

    @Test
    fun `finds one entity by pk or sk`() {
        val book = saved.getValue("Clean Code")

        assertThat(books.findOne(BookSpec().apply { id = book.pk.toString() }).get().title).isEqualTo("Clean Code")
        assertThat(books.findOne(BookSpec().apply { id = book.sk.value }).get().title).isEqualTo("Clean Code")
        assertThat(books.findOne(BookSpec().apply { id = "zzzzz-00000-zzzzz" })).isEmpty
        assertThat(books.findOne(BookSpec().apply { id = "123456789" })).isEmpty
    }

    @Test
    fun `filters by a mixed list of pks and sks`() {
        val spec = BookSpec().apply {
            ids = listOf(saved.getValue("Clean Code").pk.toString(), saved.getValue("Effective Java").sk.value, "999")
        }

        assertThat(titles(spec)).containsExactlyInAnyOrder("Clean Code", "Effective Java")
        assertThat(books.fetchAll(spec).map { it.title }).containsExactlyInAnyOrder("Clean Code", "Effective Java")
    }

    @Test
    fun `pages through results and reports the total`() {
        val first = books.findAll(BookSpec(param { size = 2; sortBy = "pages" }))
        val third = books.findAll(BookSpec(param { page = 3; size = 2; sortBy = "pages" }))

        assertThat(first.totalElements).isEqualTo(5)
        assertThat(first.totalPages).isEqualTo(3)
        assertThat(first.content.map { it.pages }).containsExactly(100, 200)
        assertThat(third.content.map { it.pages }).containsExactly(500)
        assertThat(books.findAll(BookSpec(param { page = 9; size = 2 })).content).isEmpty()
    }

    @Test
    fun `sorts by own inherited and joined fields in the requested direction`() {
        assertThat(titles(BookSpec(param { sortBy = "pages"; sortOrderString = "desc" })).first()).isEqualTo("Clean Code")
        assertThat(titles(BookSpec(param { sortBy = "title" }))).isSorted()
        assertThat(books.findAll(BookSpec(param { sortBy = "pk"; sortOrderString = "desc" })).content.map { it.pk })
            .isSortedAccordingTo(Comparator.reverseOrder())
        assertThat(titles(BookSpec(param { sortBy = "author.name,pages"; sortOrderString = "desc,asc" })))
            .containsExactly("Under_score", "Kotlin in Action", "100% Pure", "Effective Java", "Clean Code")
    }

    @Test
    fun `sort fields outside the allow list and unknown fields are ignored`() {
        val restricted = BookSpec(param { sortBy = "pages"; sortOrderString = "desc" }).apply {
            options = SpecOptions().apply { sort = SortOptions().apply { allowedSortFields = hashSetOf("title") } }
        }

        assertThat(books.findAll(restricted).content).hasSize(5)
        assertThat(restricted.ofSortedPageable().sort.isSorted).isFalse()
        assertThat(books.findAll(BookSpec(param { sortBy = "nope; drop table books" })).content).hasSize(5)
    }

    @Test
    fun `specification executor queries sort by pk descending by default and honour valid sort fields`() {
        assertThat(books.findAll(BookSpec().ofSearch()).map { it.pk }).isSortedAccordingTo(Comparator.reverseOrder())
        assertThat(books.findAll(BookSpec(param { sortBy = "pages" }).ofSearch()).map { it.pages }).containsExactly(100, 200, 300, 400, 500)
        // createdAt is declared on BaseModel: inherited fields are valid sort fields as well.
        assertThat(books.findAll(BookSpec(param { sortBy = "createdAt,pages" }).ofSearch())).hasSize(5)
    }

    @Test
    fun `search matches every word case insensitively`() {
        assertThat(titles(BookSpec(param { search = "CODE clean" }))).containsExactly("Clean Code")
        assertThat(titles(BookSpec(param { search = "java kotlin" }))).isEmpty()
        assertThat(titles(BookSpec(param { search = "   " }))).hasSize(5)
    }

    @Test
    fun `search types anchor the term`() {
        assertThat(titles(BookSpec(param { search = "clean" }, SearchType.STARTS_WITH))).containsExactly("Clean Code")
        assertThat(titles(BookSpec(param { search = "code" }, SearchType.STARTS_WITH))).isEmpty()
        assertThat(titles(BookSpec(param { search = "code" }, SearchType.ENDS_WITH))).containsExactly("Clean Code")
        assertThat(titles(BookSpec(param { search = "pure" }, SearchType.EQUAL))).isEmpty()
    }

    @Test
    fun `like wildcards in the search term are matched literally`() {
        assertThat(titles(BookSpec(param { search = "%" }))).containsExactly("100% Pure")
        assertThat(titles(BookSpec(param { search = "_" }))).containsExactly("Under_score")
        assertThat(titles(BookSpec(param { search = "e%e" }))).isEmpty()
        assertThat(titles(BookSpec(param { search = "!" }))).isEmpty()
    }

    @Test
    fun `soft delete and archive flags filter results`() {
        saved.getValue("Clean Code").deleted = true
        saved.getValue("Effective Java").archived = true
        books.flush()

        assertThat(titles(BookSpec().apply { deleted = true })).containsExactly("Clean Code")
        assertThat(titles(BookSpec().apply { deleted = false })).hasSize(4)
        assertThat(titles(BookSpec().apply { archived = true })).containsExactly("Effective Java")
        assertThat(books.count(BookSpec().apply { deleted = false; archived = false }).let { context.getBean(jakarta.persistence.EntityManager::class.java).createQuery(it).singleResult })
            .isEqualTo(3)
    }

    @Test
    fun `field projection loads only the requested columns`() {
        val spec = BookSpec(param { sortBy = "pages"; size = 2 }).apply { fields = listOf("title", "author.name") }

        val page = books.findAll(spec)

        assertThat(page.totalElements).isEqualTo(5)
        assertThat(page.content.map { it.title }).containsExactly("100% Pure", "Under_score")
        assertThat(page.content.map { it.author!!.name }).containsExactly("Ada", "Zed")
        assertThat(page.content.map { it.pages }).containsOnly(0)
        assertThat(page.content[0].getId()).isEqualTo(saved.getValue("100% Pure").getId())
        assertThat(books.fetchAll(spec)).hasSize(5)
    }

    @Test
    fun `class projection maps matching fields`() {
        val projected = books.findAll(BookSpec(param { sortBy = "pages" }), Book::class.java)

        assertThat(projected.content.map { it.title }).first().isEqualTo("100% Pure")
        assertThat(books.fetchAll(BookSpec(), Book::class.java)).hasSize(5)
        assertThat(books.findOne(BookSpec().apply { id = saved.getValue("Clean Code").pk.toString() }, Book::class.java).get().pages)
            .isEqualTo(500)
        assertThat(books.findOne(BookSpec().apply { id = "1" }, Book::class.java)).isEmpty
    }

    @Test
    fun `fetch all honours sorting and eager joins`() {
        val spec = BookSpec(param { sortBy = "pages"; sortOrderString = "desc" }).apply { joins = listOf("author") }

        assertThat(books.fetchAll(spec).map { it.pages }).containsExactly(500, 400, 300, 200, 100)
        assertThat(books.findOne(BookSpec().apply { id = saved.getValue("Clean Code").pk.toString(); joins = listOf("author") }).get().author!!.name)
            .isEqualTo("Ada")
    }

    @Test
    fun `specs resolve their entity class and reject non entities`() {
        assertThat(BookSpec().clazz).isEqualTo(Book::class.java)

        val notAnEntity = object : BaseSpec.JPA<BaseModel>(ParameterModel()) {
            override var id: String? = null
            override var ids: List<String>? = null
            override var deleted: Boolean? = null
            override var archived: Boolean? = null
            override fun ofSearch() = Specification<BaseModel> { _, _, builder -> builder.conjunction() }
        }
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (books as BaseRepo.JPA<BaseModel>).findAll(notAnEntity) }
            .hasMessageContaining("Not an entity")
    }
}
