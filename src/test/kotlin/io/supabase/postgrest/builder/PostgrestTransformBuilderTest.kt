package io.supabase.postgrest.builder

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

internal class PostgrestTransformBuilderTest {

    private var transformBuilder: PostgrestTransformBuilder<Any>? = null

    @BeforeEach
    fun beforeEach() {
        transformBuilder = PostgrestTransformBuilder(PostgrestBuilder(URI(""), mockk(), mockk(), emptyMap(), null))
    }

    @Test
    fun select() {
        transformBuilder!!.select("foo,bar")
        assertSearchParam("select", "foo,bar")
    }

    @Test
    fun single() {
        transformBuilder!!.single()
        assertHeader("Accept", "application/vnd.pgrst.object+json")
    }

    @Nested
    inner class Order {

        @Test
        fun `ascending nulls first`() {
            transformBuilder!!.order(
                column = "col",
                ascending = true,
                nullsFirst = true
            )
            assertSearchParam("order", "col.asc.nullsfirst")
        }

        @Test
        fun `descending nulls first`() {
            transformBuilder!!.order(
                column = "col",
                ascending = false,
                nullsFirst = true
            )
            assertSearchParam("order", "col.desc.nullsfirst")
        }

        @Test
        fun `ascending nulls last`() {
            transformBuilder!!.order(
                column = "col",
                ascending = true,
                nullsFirst = false
            )
            assertSearchParam("order", "col.asc.nullslast")
        }

        @Test
        fun `descending nulls last`() {
            transformBuilder!!.order(
                column = "col",
                ascending = false,
                nullsFirst = false
            )

            assertSearchParam("order", "col.desc.nullslast")
        }

        @Test
        fun `with foreign table`() {
            transformBuilder!!.order(
                column = "col",
                ascending = true,
                nullsFirst = true,
                foreignTable = "foreign"
            )
            assertSearchParam("\"foreign\".order", "col.asc.nullsfirst")
        }
    }

    @Nested
    inner class Range {

        @Test
        fun `without foreign table`() {
            transformBuilder!!.range(
                from = 5,
                to = 10,
                foreignTable = null
            )

            assertSearchParam("offset", "5")
            assertSearchParam("limit", "6")
        }

        @Test
        fun `with foreign table`() {
            transformBuilder!!.range(
                from = 5,
                to = 10,
                foreignTable = "foreign"
            )

            assertSearchParam("\"foreign\".offset", "5")
            assertSearchParam("\"foreign\".limit", "6")
        }
    }

    @Nested
    inner class Limit {

        @Test
        fun `without foreign table`() {
            transformBuilder!!.limit(
                count = 100,
                foreignTable = null
            )
            assertSearchParam("limit", "100")
        }

        @Test
        fun `with foreign table`() {
            transformBuilder!!.limit(
                count = 100,
                foreignTable = "foreign"
            )
            assertSearchParam("\"foreign\".limit", "100")

        }
    }

    private fun assertSearchParam(name: String, value: String) {
        val searchParams = transformBuilder!!.getSearchParams()
        assertThat(searchParams[name]).isEqualTo(value)
    }

    private fun assertHeader(name: String, value: String) {
        assertThat(transformBuilder!!.getHeaders()[name]).isEqualTo(value)
    }

}