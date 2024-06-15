package flashtanki.server.garage.lootbox

import com.squareup.moshi.Json

class LootboxPrizeService {
    val rewardsMap = mapOf(
        "17,9" to "3500 кри",
        "12,2" to "125 дд",
        "11,6" to "125 да",
        "10,9" to "125 нитры",
        "10,7" to "125 мин",
        "8,7" to "10к кри",
        "5,3" to "125 ремок",
        "4,2" to "100 всех припасов",
        "2,9" to "краска с фиолетового",
        "2,8" to "5 золотых",
        "2,1" to "25к кри",
        "1,2" to "3 дня премки",
        "1,1" to "10 золотых",
        "1" to "250 всех припасов",
        "0,6" to "краска с голд.свечения",
        "0,6" to "100к кри",
        "0,4" to "10 дней премки",
        "0,1" to "300к кри",
        "0,07" to "ХТ"
    )

    suspend fun getRandomReward(count: Int): List<LootboxPrize> {
        var prizes = mutableListOf<LootboxPrize>()
        prizes.add(LootboxPrize("COMMON", 1, 978053, reward))
        return prizes
    }
}

data class LootboxPrize(
    @Json val category: String,
    @Json val count: Int,
    @Json val preview: Int,
    @Json val name: String
)

