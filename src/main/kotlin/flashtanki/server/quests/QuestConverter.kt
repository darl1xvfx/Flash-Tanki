package flashtanki.server.quests

import org.koin.core.component.KoinComponent
import flashtanki.server.client.SocketLocale
import kotlin.random.Random

interface IQuestConverter {
  fun   toClientDailyQuest(quest: ServerDailyQuest, locale: SocketLocale): DailyQuest
}

class QuestConverter : IQuestConverter, KoinComponent {
  override fun toClientDailyQuest(quest: ServerDailyQuest, locale: SocketLocale): DailyQuest {
    // TODO(Assasans): Quest information
    return DailyQuest(
      canSkipForFree = true,
      description = quest.description.get(locale),
      finishCriteria = quest.required,
      image = quest.preview,
      questId = quest.id.toInt() + Random.nextInt(1, 300),
      progress = quest.current,
      skipCost = 1000,
      prizes = quest.rewards
        .sortedBy { reward -> reward.index }
        .map { reward ->
          DailyQuestPrize(
            name = reward.type.name,
            count = reward.count
          )
        }
    )
  }
}
