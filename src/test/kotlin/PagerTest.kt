@file:OptIn(ExperimentalCoroutinesApi::class)

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows

class PagerTest {

    @Test
    fun `for initialLoadSize of 0 throws IllegalInitialLoadSize`() {
        val config = PagerConfig(
            initialLoadSize = 0,
            loadSize = 1,
            preLoadDistance = 1,
        )
        assertThrows<Pager.Error.IllegalInitialLoadSize> {
            Pager<Int, Int>(
                config = config,
                load = { _ -> Page(null, null, emptyList()) },
            )
        }
    }

    @Test
    fun `for initialLoadSize less than 0 throws IllegalInitialLoadSize`() {
        val config = PagerConfig(
            initialLoadSize = -1,
            loadSize = 1,
            preLoadDistance = 1,
        )
        assertThrows<Pager.Error.IllegalInitialLoadSize> {
            Pager<Int, Int>(
                config = config,
                load = { _ -> Page(null, null, emptyList()) },
            )
        }
    }

    @Test
    fun `for loadSize of 0 throws IllegalLoadSize`() {
        val config = PagerConfig(
            initialLoadSize = 1,
            loadSize = 0,
            preLoadDistance = 1,
        )
        assertThrows<Pager.Error.IllegalLoadSize> {
            Pager<Int, Int>(
                config = config,
                load = { _ -> Page(null, null, emptyList()) },
            )
        }
    }

    @Test
    fun `for loadSize of less than 0 throws IllegalLoadSize`() {
        val config = PagerConfig(
            initialLoadSize = 1,
            loadSize = -1,
            preLoadDistance = 1,
        )
        assertThrows<Pager.Error.IllegalLoadSize> {
            Pager<Int, Int>(
                config = config,
                load = { _ -> Page(null, null, emptyList()) },
            )
        }
    }

    @Test
    fun `for preLoadDistance of less than 0 throws IllegalPreLoadDistance`() {
        val config = PagerConfig(
            initialLoadSize = 1,
            loadSize = 1,
            preLoadDistance = -1,
        )
        assertThrows<Pager.Error.IllegalPreLoadDistance> {
            Pager<Int, Int>(
                config = config,
                load = { _ -> Page(null, null, emptyList()) },
            )
        }
    }

    @Test
    fun `when returning null nextKey can't append pages anymore`() = runTest {
        val config = PagerConfig(1, 1, 1)
        val pager = Pager<Int, Int>(config) { _ -> Page(0, null, listOf(1)) }

        pager.loadInitialPages()
        pager.appendPage()

        assertEquals(2, pager.pages.size)
    }

    @Test
    fun `when returning null previousKey can't prepend pages anymore`() = runTest {
        val config = PagerConfig(2, 1, 1)
        val pager = Pager<Int, Int>(config) { _ -> Page(null, 2, listOf(1)) }

        pager.loadInitialPages()
        pager.prependPage()

        assertEquals(2, pager.pages.size)
    }

    @Test
    fun `initially loads initialLoadSize number of items`() = runTest {
        val initialLoadSize = 3
        val config = PagerConfig(initialLoadSize, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val nextKey = key ?: 1
            Page(null, nextKey + 1, listOf(nextKey))
        }

        pager.loadInitialPages()

        assertEquals(initialLoadSize, pager.pages.size)
    }

    @Test
    fun `can append page`() = runTest {
        val config = PagerConfig(2, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val nextKey = key ?: 1
            Page(null, nextKey + 1, listOf(nextKey))
        }

        pager.loadInitialPages()
        pager.appendPage()

        assertEquals(3, pager.pages.size)
    }

    @Test
    fun `can prepend page`() = runTest {
        val config = PagerConfig(2, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }

        pager.loadInitialPages()
        pager.prependPage()

        assertEquals(4, pager.pages.size)
        val items = pager.pages.flatMap(Page<Int, Int>::items)
        assertEquals(listOf(-1, 0, 1, 2), items)
    }

    @Test
    fun `load accepts previously returned nextKey`() = runTest {
        val config = PagerConfig(1, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }

        pager.loadInitialPages()
        pager.appendPage()
        pager.appendPage()

        assertEquals(5, pager.pages.size)
        val items = pager.pages.flatMap(Page<Int, Int>::items)
        assertEquals(listOf(0, 1, 2, 3, 4), items)
    }

    @Test
    fun `can get first page`() = runTest {
        val config = PagerConfig(1, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }
        pager.loadInitialPages()

        val page = pager.get(0)

        println(pager.pages.flatMap { it.items })
        assertEquals(listOf(-1), page.items)
    }

    @Test
    fun `can get second page`() = runTest {
        val config = PagerConfig(1, 1, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(null, currentKey + 1, listOf(currentKey))
        }
        pager.loadInitialPages()
        pager.appendPage()

        val page = pager.get(1)

        assertEquals(listOf(2), page.items)
    }

    @Test
    fun `can prepend page and get it`() = runTest {
        val config = PagerConfig(1, 1, 0)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, null, listOf(currentKey))
        }

        pager.loadInitialPages()
        pager.prependPage()

        val page = pager.get(0)
        assertEquals(listOf(0), page.items)
    }

    @Test
    fun `for initialLoadSize of 1 and having preLoadDistance of 2 appends 2 pages`() = runTest {
        val config = PagerConfig(1, 1, 2)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }

        pager.loadInitialPages()

        assertEquals(5, pager.pages.size)
    }

    @Test
    fun `for 2 initially loaded pages and preLoadDistance of 1 after getting 2nd page appends page`() =
        runTest {
            val config = PagerConfig(2, 1, 1)
            val pager = Pager<Int, Int>(config) { key ->
                val currentKey = key ?: 1
                Page(null, currentKey + 1, listOf(currentKey))
            }

            pager.loadInitialPages()
            pager.get(1)

            assertEquals(3, pager.pages.size)
            val lastPageItem = pager.peek(2).items.single()
            assertEquals(3, lastPageItem)
        }

    @Test
    fun `initialLoadSize = 2, loadSize = 1, preLoadDistance = 1 getting pages 1 and 2 append page`() =
        runTest {
            val config = PagerConfig(2, 1, 1)
            val pager = Pager<Int, Int>(config) { key ->
                val currentKey = key ?: 1
                Page(null, currentKey + 1, listOf(currentKey))
            }

            pager.loadInitialPages()
            pager.get(1)
            pager.get(2)

            val pages = pager.pages
            assertEquals(4, pages.size)
        }

    @Test
    fun `initialLoadSize = 2, loadSize = 2, preLoadDistance = 1 getting pages 1 appends 2 pages`() =
        runTest {
            val config = PagerConfig(2, 2, 1)
            val pager = Pager<Int, Int>(config) { key ->
                val currentKey = key ?: 1
                Page(currentKey - 1, currentKey + 1, listOf(currentKey))
            }

            pager.loadInitialPages()
            pager.get(1)

            val pages = pager.pages
            assertEquals(4, pages.size)
        }

    @Test
    fun `getting into preLoadDistance appends loadSize number of pages`() = runTest {
        val config = PagerConfig(2, 3, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }

        pager.loadInitialPages()

        pager.get(1)
        assertEquals(5, pager.pages.size)

        pager.get(4)
        assertEquals(8, pager.pages.size)

        pager.get(8)
        assertEquals(11, pager.pages.size)
    }

    @Test
    fun `initialLoadSize = 2, loadSize = 1, preLoadDistance = 1 getting page 0 prepends page`() =
        runTest {
            val config = PagerConfig(2, 1, 1)
            val pager = Pager<Int, Int>(config) { key ->
                val currentKey = key ?: 1
                Page(currentKey - 1, null, listOf(currentKey))
            }

            pager.loadInitialPages()
            pager.get(0)

            assertEquals(3, pager.pages.size)
            val firstPageItem = pager.peek(0).items.single()
            assertEquals(-1, firstPageItem)
        }

    @Test
    fun `getting into preLoadDistance prepends loadSize number of pages`() = runTest {
        val config = PagerConfig(2, 3, 1)
        val pager = Pager<Int, Int>(config) { key ->
            val currentKey = key ?: 1
            Page(currentKey - 1, currentKey + 1, listOf(currentKey))
        }

        pager.loadInitialPages()

        assertEquals(5, pager.pages.size)

        pager.get(0)
        assertEquals(8, pager.pages.size)
        assertEquals(-5, pager.peek(0).items.single())

        pager.get(0)
        assertEquals(11, pager.pages.size)
        assertEquals(-8, pager.peek(0).items.single())

        pager.get(0)
        assertEquals(14, pager.pages.size)
        assertEquals(-11, pager.peek(0).items.single())
    }
}