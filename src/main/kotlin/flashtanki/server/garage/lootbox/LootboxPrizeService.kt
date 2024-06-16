package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json
import kotlin.random.Random

data class Prize(
    val name: String,
    val rarity: String,
    val preview: Int,
    val id: String
)

class LootboxPrizeService {
    private val prizes = listOf(
        Prize("Пакет 3500 кристаллов", "COMMON", 978053, "crystals_3500"),
        Prize("Набор 125 двойного урона", "COMMON", 153186, "double_damage_125"),
        Prize("Набор 125 повышенной защиты", "COMMON", 504645, "double_armor_125"),
        Prize("Набор 125 ускорений", "COMMON", 716565, "nitro_125"),
        Prize("Набор 125 мин", "COMMON", 71622, "mine_125"),
        Prize("Пакет 10 000 кристаллов", "UNCOMMON", 824172, "crystals_10000"),
        Prize("Набор 125 ремкомплектов", "UNCOMMON", 209092, "health_125"),
        Prize("Комплект 100 всех припасов", "UNCOMMON", 629496, "allsupplies_100"),
        Prize("краска с фиолетового", "EPIC", 730749, "epic_paint"),
        Prize("Набор 5 золотых ящиков", "UNCOMMON", 882375, "goldboxes_5"),
        Prize("Пакет 25 000 кристаллов", "RARE", 542698, "crystals_25000"),
        Prize("3 дня премиум аккаунта", "UNCOMMON", 826132, "premiumdays_3"),
        Prize("Набор 10 золотых ящиков", "RARE", 468704, "goldboxes_10"),
        Prize("Комплект 250 всех припасов", "RARE", 254675, "allsupplies_250"),
        Prize("краска с голд.свечения", "LEGENDARY", 350240, "legendary_paint"),
        Prize("Пакет 100 000 кристаллов", "EPIC", 978053, "crystals_100000"),
        Prize("10 дней премиум аккаунта", "RARE", 153186, "premiumdays_10"),
        Prize("Пакет 300 000 кристаллов", "LEGENDARY", 504645, "crystals_300000"),
        Prize("ХТ", "EXOTIC", 716565, "xt"),
        Prize("Пакет 1 000 000 кристаллов", "EXOTIC", 1500009, "crystals_1000000")
    )

    private val probabilities = mapOf(
        "COMMON" to 0.34,
        "UNCOMMON" to 0.30,
        "RARE" to 0.14,
        "EPIC" to 0.10,
        "LEGENDARY" to 0.08,
        "EXOTIC" to 0.08
    )

    private val categoryOrder = mapOf(
        "COMMON" to 1,
        "UNCOMMON" to 2,
        "RARE" to 3,
        "EPIC" to 4,
        "LEGENDARY" to 5,
        "EXOTIC" to 6
    )

    private val prizeOrder = mapOf(
        "crystals_3500" to 1,
        "double_damage_125" to 2,
        "double_armor_125" to 3,
        "nitro_125" to 4,
        "mine_125" to 5,
        "crystals_10000" to 7,
        "health_125" to 8,
        "allsupplies_100" to 9,
        "goldboxes_5" to 10,
        "premiumdays_3" to 11,
        "crystals_25000" to 12,
        "goldboxes_10" to 13,
        "allsupplies_250" to 14,
        "premiumdays_10" to 15,
        "epic_paint" to 16,
        "crystals_100000" to 17,
        "legendary_paint" to 18,
        "crystals_300000" to 19,
        "xt" to 20,
        "crystals_1000000" to 21
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        require(count <= prizes.size) { "Requested count exceeds available elements." }

        val selectedPrizes = mutableListOf<Prize>()
        val prizeCounts = mutableMapOf<String, Int>()
        val random = Random.Default
        var lastSelectedPrize: Prize? = null

        while (selectedPrizes.size < count) {
            val isDuplicate = random.nextDouble() < 0.10
            val isTriplicate = random.nextDouble() < 0.05
            val filteredPrizes = if ((isDuplicate || isTriplicate) && selectedPrizes.isNotEmpty()) {
                selectedPrizes.filter {
                    (prizeCounts[it.id] ?: 0 < 3 || (isTriplicate && prizeCounts[it.id] ?: 0 < 3)) &&
                            it.id !in listOf("legendary_paint", "epic_paint", "xt") && it != lastSelectedPrize
                }
            } else {
                val rarity = selectRarity()
                prizes.filter {
                    it.rarity == rarity && (prizeCounts[it.id] ?: 0) < 3 &&
                            !selectedPrizes.any { selectedPrize -> selectedPrize.id == it.id }
                }
            }

            if (filteredPrizes.isNotEmpty()) {
                val selectedPrize = filteredPrizes[random.nextInt(filteredPrizes.size)]
                selectedPrizes.add(selectedPrize)
                prizeCounts[selectedPrize.id] = (prizeCounts[selectedPrize.id] ?: 0) + 1
                lastSelectedPrize = selectedPrize
            }
        }

        selectedPrizes.sortWith(compareBy { prizeOrder[it.id] ?: Int.MAX_VALUE })

        return selectedPrizes.map { prize ->
            LootboxPrize(
                category = prize.rarity,
                count = 1,
                preview = prize.preview,
                name = prize.name
            )
        }.sortedWith(compareBy { categoryOrder[it.category] })
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
