package flashtanki.server.garage.lootbox

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

class LootboxPrizeService {
    private val objectMapper = ObjectMapper()

    private fun getLootBoxCategory(category: String): LootBoxCategory {
        return when (category) {
            "COMMON" -> LootBoxCategory.COMMON
            "UNCOMMON" -> LootBoxCategory.UNCOMMON
            "RARE" -> LootBoxCategory.RARE
            "EPIC" -> LootBoxCategory.EPIC
            "LEGENDARY" -> LootBoxCategory.LEGENDARY
            "EXOTIC" -> LootBoxCategory.EXOTIC
            else -> LootBoxCategory.COMMON
        }
    }

    fun createPrizes(jsonString: String?): List<String> {
        val prizes: MutableList<String> = ArrayList()
        jsonString?.let {
            try {
                val prizeList: List<LootboxPrize> = objectMapper.readValue(it, object : TypeReference<List<LootboxPrize>>() {})
                for (prize in prizeList) {
                    prizes.add(prize.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return prizes
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val service = LootboxPrizeService()
            val jsonString = "[{\"category\":\"LEGENDARY\",\"count\":1,\"preview\":1000001,\"name\":\"Краска Галактика\"}]"
            val prizes = service.createPrizes(jsonString)
            for (prize in prizes) {
                println(prize)
            }
        }
    }
}

data class LootboxPrize(
    val category: String,
    val count: Int,
    val preview: Int,
    val name: String
) {
    override fun toString(): String {
        return "LootboxPrize{" +
                "category='" + category + '\'' +
                ", count=" + count +
                ", preview=" + preview +
                ", name='" + name + '\'' +
                '}'
    }
}

enum class LootBoxCategory {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    EXOTIC
}
