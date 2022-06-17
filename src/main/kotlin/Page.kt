data class Page<Key : Any, Value : Any>(
    val previousKey: Key?,
    val nextKey: Key?,
    val items: List<Value>,
)