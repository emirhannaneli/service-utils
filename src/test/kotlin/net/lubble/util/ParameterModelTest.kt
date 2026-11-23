package net.lubble.util

import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOptions
import net.lubble.util.model.SortOrder
import net.lubble.util.model.SpecOptions
import net.lubble.util.spec.PaginationSpec
import net.lubble.util.spec.tool.SpecTool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import tools.jackson.databind.json.JsonMapper

class ParameterModelTest {
    open class UserParam : ParameterModel() {
        var name: String? = null
        var age: Int? = null
    }

    class AdminParam : UserParam() {
        var role: String? = null
    }

    private fun options(allowed: Set<String>? = null, disallowed: Set<String>? = null) = SpecOptions().apply {
        sort = SortOptions().apply {
            allowedSortFields = allowed?.toHashSet()
            disallowedSortFields = disallowed?.toHashSet()
        }
    }

    @AfterEach
    fun resetLimit() {
        PaginationSpec.maxSize = PaginationSpec.DEFAULT_MAX_SIZE
    }

    @Test
    fun `search and sortBy are normalized`() {
        val param = ParameterModel().apply {
            search = "  Ada LOVELACE "
            sortBy = " name "
            sortOrderString = " desc "
        }

        assertThat(param.search).isEqualTo("ada lovelace")
        assertThat(param.sortBy).isEqualTo("name")
        assertThat(param.sortOrderString).isEqualTo("DESC")
        assertThat(param.getSortOrderValue()).isEqualTo("DESC")
        assertThat(ParameterModel().getSortOrderValue()).isEqualTo("ASC")
        assertThat(ParameterModel().apply { sortOrder = SortOrder.DESC }.getSortOrderValue()).isEqualTo("DESC")
    }

    @Test
    fun `page and size never drop below one`() {
        val param = ParameterModel().apply { page = -3; size = 0 }

        assertThat(param.page).isEqualTo(1)
        assertThat(param.size).isEqualTo(1)
        assertThat(param.ofPageable().pageNumber).isZero()
        assertThat(ParameterModel().size).isEqualTo(10)
    }

    @Test
    fun `pageable is zero based and carries the sort`() {
        val param = ParameterModel().apply { page = 3; size = 25 }

        val pageable = param.ofPageable(Sort.by("name"))

        assertThat(pageable.pageNumber).isEqualTo(2)
        assertThat(pageable.pageSize).isEqualTo(25)
        assertThat(pageable.offset).isEqualTo(50)
        assertThat(pageable.sort).isEqualTo(Sort.by("name"))
    }

    @Test
    fun `size is capped at one thousand unless configured otherwise`() {
        assertThat(ParameterModel().apply { size = 1_000 }.size).isEqualTo(1_000)
        assertThat(ParameterModel().apply { size = 1_001 }.size).isEqualTo(1_000)
        assertThat(ParameterModel().apply { size = Int.MAX_VALUE }.ofPageable().pageSize).isEqualTo(1_000)

        PaginationSpec.maxSize = 5_000
        assertThat(ParameterModel().apply { size = 2_000 }.size).isEqualTo(2_000)
    }

    @Test
    fun `size is capped by the configured maximum`() {
        PaginationSpec.maxSize = 100

        val param = ParameterModel().apply { size = 1_000_000 }

        assertThat(param.size).isEqualTo(100)
        assertThat(param.ofPageable().pageSize).isEqualTo(100)
    }

    @Test
    fun `huge page numbers cannot overflow the row offset`() {
        val param = ParameterModel().apply { page = Int.MAX_VALUE; size = 1_000 }

        val pageable = param.ofPageable()

        assertThat(pageable.offset).isBetween(0L, Int.MAX_VALUE.toLong())
        assertThat(pageable.offset.toInt()).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `isFiltering reflects search and subclass fields only`() {
        assertThat(ParameterModel().apply { page = 4; sortBy = "name" }.isFiltering).isFalse()
        assertThat(ParameterModel().apply { search = "ada" }.isFiltering).isTrue()
        assertThat(ParameterModel().apply { search = "   " }.isFiltering).isFalse()
        assertThat(UserParam().isFiltering).isFalse()
        assertThat(UserParam().apply { name = "  " }.isFiltering).isFalse()
        assertThat(UserParam().apply { age = 3 }.isFiltering).isTrue()
        // Fields of intermediate classes count as well.
        assertThat(AdminParam().apply { name = "ada" }.isFiltering).isTrue()
        assertThat(AdminParam().apply { role = "root" }.isFiltering).isTrue()
    }

    @Test
    fun `sort entries keep request order and pair directions by position`() {
        val param = ParameterModel().apply { sortBy = "name, age ,,createdAt"; sortOrderString = "desc,asc" }

        assertThat(param.sortEntries(null)).containsExactly("name" to true, "age" to false, "createdAt" to false)
    }

    @Test
    fun `a single direction applies to every field and duplicates collapse`() {
        val param = ParameterModel().apply { sortBy = "name,age,name"; sortOrder = SortOrder.DESC }

        assertThat(param.sortEntries(null)).containsExactly("name" to true, "age" to true)
    }

    @Test
    fun `unknown directions sort ascending instead of failing`() {
        val param = ParameterModel().apply { sortBy = "name,age"; sortOrderString = "sideways,; drop table" }

        assertThat(param.sortEntries(null)).containsExactly("name" to false, "age" to false)
        assertThat(SpecTool(param).ofSortedPageable().sort).isEqualTo(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("age")))
    }

    @Test
    fun `no sort entries without sortBy`() {
        assertThat(ParameterModel().sortEntries(null)).isEmpty()
        assertThat(ParameterModel().apply { sortBy = " , " }.sortEntries(null)).isEmpty()
        assertThat(SpecTool(ParameterModel()).ofSortedPageable().sort.isSorted).isFalse()
    }

    @Test
    fun `allowed and disallowed sort fields are enforced`() {
        val param = ParameterModel().apply { sortBy = "password,name,age"; sortOrderString = "desc,asc,desc" }

        assertThat(param.sortEntries(options(allowed = setOf("age", "name")))).containsExactly("name" to false, "age" to true)
        assertThat(param.sortEntries(options(disallowed = setOf("password")))).containsExactly("name" to false, "age" to true)
        assertThat(param.sortEntries(options(allowed = emptySet()))).isEmpty()
        assertThat(param.sortEntries(options())).hasSize(3)
    }

    @Test
    fun `sort options filter by intersection or subtraction`() {
        val fields = hashSetOf("a", "b", "c")

        assertThat(SortOptions().filterAllowedFields(fields)).isSameAs(fields)
        assertThat(SortOptions().apply { allowedSortFields = hashSetOf("a", "x") }.filterAllowedFields(fields)).containsExactly("a")
        assertThat(SortOptions().apply { disallowedSortFields = hashSetOf("a") }.filterAllowedFields(fields))
            .containsExactlyInAnyOrder("b", "c")
        // The allow list wins when both are present.
        assertThat(
            SortOptions().apply { allowedSortFields = hashSetOf("a"); disallowedSortFields = hashSetOf("a") }.filterAllowedFields(fields)
        ).containsExactly("a")
    }

    @Test
    fun `spec tool builds a sorted pageable honouring its options`() {
        val param = ParameterModel().apply { page = 2; size = 5; sortBy = "secret,name"; sortOrderString = "asc,desc" }
        val tool = object : SpecTool(param) {
            override val options = options(disallowed = setOf("secret"))
        }

        val pageable = tool.ofSortedPageable()

        assertThat(pageable.pageNumber).isEqualTo(1)
        assertThat(pageable.pageSize).isEqualTo(5)
        assertThat(pageable.sort).isEqualTo(Sort.by(Sort.Order.desc("name")))
        assertThat(tool.ofPageable().sort.isSorted).isFalse()
        assertThat(tool.ofPageable(Sort.by("x")).sort).isEqualTo(Sort.by("x"))
    }

    @Test
    fun `paging and sorting parameters are not serialized`() {
        val json = JsonMapper.builder().build().writeValueAsString(UserParam().apply { name = "ada"; search = "x"; sortBy = "name" })

        assertThat(json).contains("\"name\":\"ada\"").doesNotContain("page").doesNotContain("sortBy").doesNotContain("search")
    }

    @Test
    fun `sort order enum exposes label and value`() {
        assertThat(SortOrder.ASC.value).isEqualTo("ASC")
        assertThat(SortOrder.DESC.label).isEqualTo("sort.order.desc")
    }
}
