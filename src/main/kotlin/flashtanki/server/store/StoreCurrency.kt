package flashtanki.server.store

enum class StoreCurrency(val key: String, val displayName: String) {
  RUB("rub", "RUB"),
  USD("usd", "USD");

  companion object {
    private val map by lazy { values().associateBy(StoreCurrency::key) }

    fun get(key: String) = map[key]
  }
}
