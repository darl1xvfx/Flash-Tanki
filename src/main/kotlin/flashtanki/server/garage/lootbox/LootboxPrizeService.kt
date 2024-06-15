package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json

class LootboxPrizeService {
    val rewardsMap = mapOf(
        "17,9" to "Пакет 3500 кристаллов",
        "12,2" to "Набор 125 двойного урона",
        "11,6" to "Набор 125 повышенной защиты",
        "10,9" to "Набор 125 ускорений",
        "10,7" to "Набор 125 мин",
        "8,7" to "Пакет 10 000 кристаллов",
        "5,3" to "Набор 125 ремкомплектов",
        "4,2" to "Комплект 100 всех припасов",
        "2,9" to "краска с фиолетового",
        "2,8" to "Набор 5 золотых ящиков",
        "2,1" to "Пакет 25 000 кристаллов",
        "1,2" to "3 дня премиум аккаунта",
        "1,1" to "Набор 10 золотых ящиков",
        "1" to "Комплект 250 всех припасов",
        "0,6" to "краска с голд.свечения",
        "0,6" to "Пакет 100 000 кристаллов",
        "0,4" to "10 дней премиум аккаунта",
        "0,1" to "Пакет 300 000 кристаллов",
        "0,07" to "ХТ"
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        var names = mutableListOf("тест", "тест2", "тест3", "тест4", "тест5", "тест6", "тест7", "тест8", "тест9", "тест10", "тест11", "тест12", "тест13", "тест14", "тест15")
        var previews = mutableListOf(978053, 153186, 504645, 716565, 71622, 824172, 209092, 629496, 730749, 882375, 542698, 826132, 468704, 254675, 350240)
        var prizes = mutableListOf<LootboxPrize>()
        var i = 0
        while (i < count) {
            prizes.add(LootboxPrize("COMMON", 1, previews[i], names[i]))
            i++
        }
        return prizes
    }
}

data class LootboxPrize(
    @Json val category: String,
    @Json val count: Int,
    @Json val preview: Int,
    @Json val name: String
)

