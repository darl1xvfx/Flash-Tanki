package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json
import kotlin.random.Random

data class Prize(val name: String, val rarity: String)

class LootboxPrizeService {
    private val prizes = listOf(
        Prize("Пакет 3500 кристаллов", "COMMON"),
        Prize("Набор 125 двойного урона", "COMMON"),
        Prize("Набор 125 повышенной защиты", "COMMON"),
        Prize("Набор 125 ускорений", "COMMON"),
        Prize("Набор 125 мин", "COMMON"),
        Prize("Пакет 10 000 кристаллов", "UNCOMMON"),
        Prize("Набор 125 ремкомплектов", "UNCOMMON"),
        Prize("Комплект 100 всех припасов", "UNCOMMON"),
        Prize("краска с фиолетового", "RARE"),
        Prize("Набор 5 золотых ящиков", "RARE"),
        Prize("Пакет 25 000 кристаллов", "RARE"),
        Prize("3 дня премиум аккаунта", "EPIC"),
        Prize("Набор 10 золотых ящиков", "EPIC"),
        Prize("Комплект 250 всех припасов", "EPIC"),
        Prize("краска с голд.свечения", "LEGENDARY"),
        Prize("Пакет 100 000 кристаллов", "LEGENDARY"),
        Prize("10 дней премиум аккаунта", "LEGENDARY"),
        Prize("Пакет 300 000 кристаллов", "EXOTIC"),
        Prize("ХТ", "EXOTIC")
    )

    private val probabilities = mapOf(
        "COMMON" to 0.35,
        "UNCOMMON" to 0.25,
        "RARE" to 0.17,
        "EPIC" to 0.15,
        "LEGENDARY" to 0.08,
        "EXOTIC" to 0.08
    )

    private val previews = listOf(
        978053, 153186, 504645, 716565, 71622, 824172, 209092, 629496, 730749, 882375, 542698, 826132,
        468704, 254675, 350240
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        require(count <= prizes.size && count <= previews.size) { "Requested count exceeds available elements." }

        val randomPrizes = List(count) {
            val rarity = selectRarity()
            val filteredPrizes = prizes.filter { it.rarity == rarity }
            val index = Random.nextInt(filteredPrizes.size)
            filteredPrizes[index]
        }

        val shuffledPreviews = previews.shuffled()

        return randomPrizes.mapIndexed { i, prize ->
            LootboxPrize(
                category = prize.rarity,
                count = 1,
                preview = shuffledPreviews[i],
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
    @Json val category: String,
    @Json val count: Int,
    @Json val preview: Int,
    @Json val name: String
)
