package flashtanki.server.news

import kotlin.io.path.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import flashtanki.server.IResourceManager
import flashtanki.server.client.SocketLocale

class NewsLoader : KoinComponent {
    private val logger = KotlinLogging.logger { }
    private val json by inject<Moshi>()
    private val resourceManager by inject<IResourceManager>()

    fun loadNews(locale: SocketLocale?): List<ServerNewsData> {
        val newsFileName = when (locale) {
            SocketLocale.Russian -> "news/ru_news.json"
            SocketLocale.English -> "news/en_news.json"
            else -> "news/en_news.json"
        }

        val newsJson = resourceManager.get(newsFileName)

        val newsAdapter: JsonAdapter<List<ServerNewsData>> = json.adapter(
            Types.newParameterizedType(List::class.java, ServerNewsData::class.java)
        )

        return newsAdapter.fromJson(newsJson.readText())
            ?: emptyList()
    }
}
