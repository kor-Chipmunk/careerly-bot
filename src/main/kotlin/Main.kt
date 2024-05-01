import com.google.gson.Gson
import fuel.httpGet
import fuel.httpPost
import kotlinx.coroutines.runBlocking

suspend fun getResponse(): Response {
    val rawResponse = REQUEST_URL.httpGet().body
    return Gson().fromJson(rawResponse, Response::class.java)
}

fun mapToEmbeds(response: Response): List<Embed> {
    return response.data.comments.mapIndexed { _, it ->
        val userProfile = it.userProfile

        Embed(
            color = GREEN_COLOR,
            author = Author(
                name = "${userProfile.name} - ${userProfile.headline}",
                url = userProfile.profileUrl,
                icon_url = userProfile.smallImageUrl,
            ),
            title = it.title,
            url = it.postUrl,
            thumbnail = Image(url = it.photoUrl),
            description = it.description.substring(0, minOf(it.description.length, 200)) + "..."
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

fun main(): Unit = runBlocking {
    val response = getResponse()
    val embeds = mapToEmbeds(response)

    embeds.chunked(MAX_EMBED_SIZE) { chunk ->
        val isFirstChunk = chunk == embeds.chunked(MAX_EMBED_SIZE).first()

        val webHookData = WebHookData(
            username = "커리어리 봇",
            avatar_url = "https://careerly.co.kr/favicon.png",
            allowed_mentions = AllowedMentions(
                parse = listOf("users", "roles")
            ),
            embeds = chunk,
            content = if (isFirstChunk) {
                "**< 커리어리 트렌드 >**\n지난 30일 동안 각 분야에서 반응이 좋았던 게시물을 만나보세요."
            } else {
                ""
            }
        )

        runBlocking {
            sendWebHooks(webHookData)
        }
    }
}

data class Response(
    val statusCode: Int,
    val message: String,
    val data: Data,
)

data class Data(
    val interestId: Int,
    val publishDate: String,
    val comments: List<Comment>,
)

data class Comment(
    val postId: Int,
    val title: String,
    val photoUrl: String,
    val userProfile: UserProfile,
    val description: String,
) {
    val postUrl: String
        get() = "https://careerly.co.kr/comments/$postId"
}

data class UserProfile(
    val id: Int,
    val name: String,
    val headline: String,
    val imageUrl: String,
) {
    val profileUrl: String
        get() = "https://careerly.co.kr/profiles/$id"

    val smallImageUrl: String
        get() = imageUrl.replace("publy-cdn.s3.ap-northeast-2.amazonaws.com", "publy.imgix.net")
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
    val author: Author,
    val title: String,
    val url: String,
    val thumbnail: Image,
    val description: String
)

data class Author(
    val name: String,
    val url: String,
    val icon_url: String
)

data class Image(val url: String?)

val ENV_KEY_DISCORD_WEBHOOKS = arrayOf("DISCORD_WEBHOOK")

val REQUEST_URL = "https://news.publy.co/api/public/comments/popular/trend?interestId=9"

val MAX_EMBED_SIZE = 10

val GREEN_COLOR = "38912"
