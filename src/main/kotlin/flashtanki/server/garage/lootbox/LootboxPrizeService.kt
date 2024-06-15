package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json
import kotlin.random.Random

data class Prize(
    val name: String,
    val rarity: String,
    val preview: Int
)

class LootboxPrizeService {
    private val prizes = listOf(
        Prize("Пакет 3500 кристаллов", "COMMON", 978053),
        Prize("Набор 125 двойного урона", "COMMON", 153186),
        Prize("Набор 125 повышенной защиты", "COMMON", 504645),
        Prize("Набор 125 ускорений", "COMMON", 716565),
        Prize("Набор 125 мин", "COMMON", 71622),
        Prize("Пакет 10 000 кристаллов", "UNCOMMON", 824172),
        Prize("Набор 125 ремкомплектов", "UNCOMMON", 209092),
        Prize("Комплект 100 всех припасов", "UNCOMMON", 629496),
        Prize("краска с фиолетового", "RARE", 730749),
        Prize("Набор 5 золотых ящиков", "RARE", 882375),
        Prize("Пакет 25 000 кристаллов", "RARE", 542698),
        Prize("3 дня премиум аккаунта", "EPIC", 826132),
        Prize("Набор 10 золотых ящиков", "EPIC", 468704),
        Prize("Комплект 250 всех припасов", "EPIC", 254675),
        Prize("краска с голд.свечения", "LEGENDARY", 350240),
        Prize("Пакет 100 000 кристаллов", "LEGENDARY", 978053),
        Prize("10 дней премиум аккаунта", "LEGENDARY", 153186),
        Prize("Пакет 300 000 кристаллов", "EXOTIC", 504645),
        Prize("ХТ", "EXOTIC", 716565)
    )

    private val probabilities = mapOf(
        "COMMON" to 0.35,
        "UNCOMMON" to 0.25,
        "RARE" to 0.17,
        "EPIC" to 0.15,
        "LEGENDARY" to 0.08,
        "EXOTIC" to 0.08
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        require(count <= prizes.size) { "Requested count exceeds available elements." }

        val randomPrizes = List(count) {
            val rarity = selectRarity()
            val filteredPrizes = prizes.filter { it.rarity == rarity }
            val index = Random.nextInt(filteredPrizes.size)
            filteredPrizes[index]
        }

        return randomPrizes.map { prize ->
            LootboxPrize(
                category = prize.rarity,
                count = 1,
                preview = prize.preview,
                name = prize.name
            )
        }
    }

    private fun selectRarity(): String {
        val rand = Random.nextDouble()
        var cumulativeProbability = 0.0
        for ((rarity, probability) in probabilities) {
            cumulativeProbability += probability
            if (rand < cumulativeProbability) {
                return rarity
            }
        }
        return probabilities.keys.last()
    }
}

data class LootboxPrize(
    @Json(name = "category") val category: String,
    @Json(name = "count") val count: Int,
    @Json(name = "preview") val preview: Int,
    @Json(name = "name") val name: String
)
