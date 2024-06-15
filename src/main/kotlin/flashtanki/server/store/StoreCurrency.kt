package flashtanki.server.store

enum class StoreCurrency(val key: String, val displayName: String) {
  RUB("rub", "RUB");

  companion object {
    private val map by lazy { values().associateBy(StoreCurrency::key) }

    fun get(key: String) = map[key]
  }
}
