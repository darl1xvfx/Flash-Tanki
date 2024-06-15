package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json
import kotlin.random.Random

class LootboxPrizeService {
    private val rewardsMap = mapOf(
        "17,9" to Pair("Пакет 3500 кристаллов", "COMMON"),
        "12,2" to Pair("Набор 125 двойного урона", "COMMON"),
        "11,6" to Pair("Набор 125 повышенной защиты", "COMMON"),
        "10,9" to Pair("Набор 125 ускорений", "COMMON"),
        "10,7" to Pair("Набор 125 мин", "COMMON"),
        "8,7" to Pair("Пакет 10 000 кристаллов", "UNCOMMON"),
        "5,3" to Pair("Набор 125 ремкомплектов", "UNCOMMON"),
        "4,2" to Pair("Комплект 100 всех припасов", "UNCOMMON"),
        "2,9" to Pair("краска с фиолетового", "RARE"),
        "2,8" to Pair("Набор 5 золотых ящиков", "RARE"),
        "2,1" to Pair("Пакет 25 000 кристаллов", "RARE"),
        "1,2" to Pair("3 дня премиум аккаунта", "EPIC"),
        "1,1" to Pair("Набор 10 золотых ящиков", "EPIC"),
        "1" to Pair("Комплект 250 всех припасов", "EPIC"),
        "0,6" to Pair("краска с голд.свечения", "LEGENDARY"),
        "0,6" to Pair("Пакет 100 000 кристаллов", "LEGENDARY"),
        "0,4" to Pair("10 дней премиум аккаунта", "LEGENDARY"),
        "0,1" to Pair("Пакет 300 000 кристаллов", "EXOTIC"),
        "0,07" to Pair("ХТ", "EXOTIC")
    )

    private val namesAndRarities = rewardsMap.values.toList()

    private val previews = listOf(
        978053, 153186, 504645, 716565, 71622, 824172, 209092, 629496, 730749, 882375, 542698, 826132,
        468704, 254675, 350240
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        require(count <= namesAndRarities.size && count <= previews.size) { "Requested count exceeds available elements." }

        val shuffledRewards = namesAndRarities.shuffled()
        val shuffledPreviews = previews.shuffled()

        return List(count) { i ->
            LootboxPrize(
                category = shuffledRewards[i].second,
                count = 1,
                preview = shuffledPreviews[i],
                name = shuffledRewards[i].first
            )
        }
    }
}

data class LootboxPrize(
    @Json val category: String,
    @Json val count: Int,
    @Json val preview: Int,
    @Json val name: String
)
