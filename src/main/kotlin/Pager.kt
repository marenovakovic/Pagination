data class PagerConfig(
    val initialLoadSize: Int,
    val loadSize: Int,
    val preLoadDistance: Int,
)

@Suppress("FunctionName")
fun <Key : Any, Value : Any> Pager(
    config: PagerConfig,
    load: suspend (Key?) -> Page<Key, Value>,
): Pager<Key, Value> {
    if (config.initialLoadSize <= 0) throw Pager.Error.IllegalInitialLoadSize
    if (config.loadSize <= 0) throw Pager.Error.IllegalLoadSize
    if (config.preLoadDistance < 0) throw Pager.Error.IllegalPreLoadDistance
    return PagerImpl(config, load)
}

interface Pager<Key : Any, Value : Any> {
    val pages: List<Page<Key, Value>>

    suspend fun get(pageIndex: Int): Page<Key, Value>
    fun peek(pageIndex: Int): Page<Key, Value>
    suspend fun loadInitialPages()
    suspend fun appendPage()
    suspend fun prependPage()

    sealed class Error(message: String) : Throwable(message) {
        object IllegalInitialLoadSize : Error("initialLoadSize can't be 0 or less.")
        object IllegalLoadSize : Error("loadSize can't be 0 or less.")
        object IllegalPreLoadDistance : Error("preLoadDistance can't be less than 0.")
    }
}

private class PagerImpl<Key : Any, Value : Any>(
    private val config: PagerConfig,
    private val load: suspend (Key?) -> Page<Key, Value>,
) : Pager<Key, Value> {
    private val _pages = mutableListOf<Page<Key, Value>>()
    override val pages: List<Page<Key, Value>>
        get() = _pages

    private var lastAccessedPage: Int = 0

    private val isAppendEnd: Boolean
        get() = pages.last().nextKey == null
    private val isPrependEnd: Boolean
        get() = pages.first().previousKey == null

    private val isInPreLoadAppendDistance
        get() = pages.size - lastAccessedPage <= config.preLoadDistance
    private val isInPreLoadPrependDistance
        get() = lastAccessedPage < config.preLoadDistance

    override suspend fun get(pageIndex: Int): Page<Key, Value> {
        lastAccessedPage = pageIndex
        preLoadPagesIfInDistance()
        return pages[lastAccessedPage]
    }

    override fun peek(pageIndex: Int): Page<Key, Value> = pages[pageIndex]

    override suspend fun loadInitialPages() {
        _pages += load(null)
        repeat(config.initialLoadSize - 1) { appendPage() }
        preLoadPagesIfInDistance()
    }

    private suspend fun preLoadPagesIfInDistance() {
        while (!isAppendEnd && isInPreLoadAppendDistance) repeat(config.loadSize) { appendPage() }
        while (!isPrependEnd && isInPreLoadPrependDistance) repeat(config.loadSize) { prependPage() }
    }

    override suspend fun appendPage() {
        val lastPage = _pages.last()
        val nextKey = lastPage.nextKey ?: return
        _pages += load(nextKey)
    }

    override suspend fun prependPage() {
        val firstPage = _pages.first()
        val previousKey = firstPage.previousKey ?: return
        val page = load(previousKey)
        _pages.add(0, page)
        lastAccessedPage++
    }
}