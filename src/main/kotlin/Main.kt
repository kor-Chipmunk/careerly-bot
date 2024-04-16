import com.google.gson.Gson
import fuel.httpGet
import fuel.httpPost
import kotlinx.coroutines.runBlocking

suspend fun main() {
    runBlocking {
        val rawResponse = "https://news.publy.co/api/public/comments/popular/best?limit=10".httpGet().body
        val response = Gson().fromJson(rawResponse, Response::class.java)

        val embeds = response.data.mapIndexed { id, it ->
            val comment = it.comments.first()
            val userProfile = comment.userProfile

            Embed(
                color = "38912",
                author = Author(
                    name = "${userProfile.name} - ${userProfile.title}",
                    url = userProfile.profileUrl,
                    icon_url = userProfile.small_image_url,
                ),
                title = comment.title,
                url = it.postUrl,
                thumbnail = Image(url = comment.photoUrl),
                description = comment.description.substring(0, minOf(comment.description.length, 200)) + "..."
            )
        }.toList()

        val webHookData = WebHookData(
            username = "커리어리 봇",
            avatar_url = "https://careerly.co.kr/favicon.png",
            allowed_mentions = AllowedMentions(
                parse = listOf("users", "roles")
            ),
            embeds = embeds,
            content = "**< 주간 인기 TOP 10 >**"
        )

        for (webhook in ENV_KEY_DISCORD_WEBHOOKS) {
            "https://discord.com/api/webhooks/1229910660691923036/U_f-elAnpA_V8HUG_Ouy7Xx6UpFlluSruhJfntYUyRW8drCuyU90IWrUaJyABwjVvbe-".httpPost(
                headers = mapOf("Content-Type" to "application/json"),
                body = Gson().toJson(webHookData)
            )
        }
    }
}

data class Response(
    val statusCode: Int,
    val message: String,
    val data: List<Data>,
)

data class Data(
    val postId: Int,
    val comments: List<Comment>,
    val payload: Payload,
) {
    val postUrl: String
        get() = "https://careerly.co.kr/comments/$postId"
}

data class Comment(
    val postId: Int,
    val title: String,
    val photoUrl: String,
    val userProfile: UserProfile,
    val description: String,
)

data class UserProfile(
    val id: Int,
    val name: String,
    val title: String,
    val small_image_url: String,
) {
    val profileUrl: String
        get() = "https://careerly.co.kr/profiles/$id"
}

data class Payload(
    val postViewCount: Int,
)

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