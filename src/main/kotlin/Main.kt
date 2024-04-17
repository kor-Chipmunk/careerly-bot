import com.google.gson.Gson
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import fuel.httpPost
import kotlinx.coroutines.runBlocking


fun mapToEmbeds(rssChannel: RssChannel): List<Embed> {
    return rssChannel.items.map {
        Embed(
            color = GREEN_COLOR,
            title = it.title ?: "",
            url = it.link ?: "",
            thumbnail = Image(url = it.image),
            description = it.content ?: ""
        )
    }
}

suspend fun sendWebHooks(webHookData: WebHookData) {
    for (webhook in ENV_KEY_DISCORD_WEBHOOKS) {
        System.getenv(webhook).httpPost(
            headers = mapOf("Content-Type" to "application/json"),
            body = Gson().toJson(webHookData)
        )
    }
}

fun main() = runBlocking {
    val rssParser = RssParser()
    val rssChannel = rssParser.getRssChannel(REQUEST_URL)

    val embeds = mapToEmbeds(rssChannel)
    val webHookData = WebHookData(
        username = "커리어리 봇",
        avatar_url = "https://careerly.co.kr/favicon.png",
        allowed_mentions = AllowedMentions(
            parse = listOf("users", "roles")
        ),
        embeds = embeds,
        content = ":bell: 띵동! **커리어리 일간 피드**가 도착했습니다!"
    )
    sendWebHooks(webHookData)
}


data class WebHookData(
    val username: String,
    val avatar_url: String,
    val allowed_mentions: AllowedMentions,
    val embeds: List<Embed>,
    val content: String,
)

data class AllowedMentions(val parse: List<String>)

data class Embed(
    val color: String,
    val title: String,
    val url: String,
    val thumbnail: Image,
    val description: String
)

data class Image(val url: String?)

val ENV_KEY_DISCORD_WEBHOOKS = arrayOf("DISCORD_WEBHOOK")

val REQUEST_URL = "https://careerly.co.kr/rss/dev"

val GREEN_COLOR = "38912"
