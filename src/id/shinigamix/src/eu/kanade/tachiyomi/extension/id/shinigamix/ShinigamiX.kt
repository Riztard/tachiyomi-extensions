package eu.kanade.tachiyomi.extension.id.shinigamix

import android.util.Base64
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ShinigamiX : HttpSource() {

    override val name = "Shinigami X"

    override val baseUrl = "https://shinigami.moe"

    private val apiurl = "https://api.shinigami.ae"

    override val lang = "id"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val apiHeaders: Headers by lazy { apiHeadersBuilder().build() }

    private val encodedString = "AAAAaAAAAHQAAAB0AAAAc" + "AAAADoAiBuAAAvAAAALwAAADEAA" + "AA1AAAANAAAbC4AAAAyAAAANgAABC4" + "AAAAxAAAAMwAAADMAAAAuAAAANg" + "AAADMAAAA6AAAAOAAAADAAAAA4AAAAMA=="

    private val decodedString = Base64.decode(
        encodedString
            .replace("AbC", "AAC", true)
            .replace("DoAiBu", "DoA", true),
        Base64.DEFAULT,
    )
        .toString(Charsets.UTF_32)

    private val apiUrl = decodedString

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(12, 1, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("X-Requested-With", "")

    private fun apiHeadersBuilder(): Headers.Builder = headersBuilder()
        .add("Accept", "application/json")
        .add("User-Agent", "okhttp/3.14.9")

    override fun popularMangaRequest(page: Int): Request {
        val url = "$apiUrl/$API_BASE_PATH/filter/views".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("multiple", "true")
            .toString()

        return GET(url, apiHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = response.parseAs<List<ShinigamiXBrowseDto>>()

        val projectList = result.map(::popularMangaFromObject)

        val hasNextPage = true

        return MangasPage(projectList, hasNextPage)
    }

    private fun popularMangaFromObject(obj: ShinigamiXBrowseDto): SManga = SManga.create().apply {
        title = obj.title.toString()
        thumbnail_url = obj.thumbnail
        url = obj.url.toString()
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiUrl/$API_BASE_PATH/filter/latest".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("multiple", "true")
            .toString()

        return GET(url, apiHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiUrl/$API_BASE_PATH".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())

        if (query.isNotEmpty()) {
            url.addPathSegment("search")
            url.addQueryParameter("keyword", query)
        }

        return GET(url.toString(), apiHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    override fun getMangaUrl(manga: SManga): String {
        return manga.url.substringAfter("?url=")
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$apiUrl/$API_BASE_PATH/comic?url=${manga.url}", apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val mangaDetails = response.parseAs<ShinigamiXMangaDetailDto>()

        return SManga.create().apply {
            author = getValue(mangaDetails.detailList, "Author(s)").replace("Updating", "")
            artist = getValue(mangaDetails.detailList, "Artist(s)").replace("Updating", "")
            status = getValue(mangaDetails.detailList, "Tag(s)").toStatus()
            description = mangaDetails.description

            val type = getValue(mangaDetails.detailList, "Type")
            genre = getValue(mangaDetails.detailList, "Genre(s)") +
                if (type.isNullOrBlank().not()) ", $type" else ""
        }
    }

    private fun getValue(detailList: List<ShinigamiXMangaDetailListDto>?, name: String): String {
        val value = detailList!!.firstOrNull { it.name == name }?.value

        return value.orEmpty()
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return chapter.url.substringAfter("?url=")
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<ShinigamiXChapterListDto>()

        return result.chapterList!!.map(::chapterFromObject)
    }

    private fun chapterFromObject(obj: ShinigamiXChapterDto): SChapter = SChapter.create().apply {
        name = obj.name
        date_upload = obj.date.toDate()
        url = "$apiUrl/$API_BASE_PATH_2/chapter?url=" + obj.url
    }

    override fun pageListRequest(chapter: SChapter): Request {
        return GET(chapter.url, apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<ShinigamiXChapterDto>()
        return result.pages.mapIndexedNotNull { index, data ->
            // filtering image
            if (data == null || data.contains("_desktop")) null else Page(index = index, imageUrl = data)
        }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body.string())
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "Updating", "OnGoing" -> SManga.ONGOING
        "Completed", "Finished" -> SManga.COMPLETED
        "Canceled" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    companion object {
        private const val API_BASE_PATH = "api/v1"
        private const val API_BASE_PATH_2 = "api/v2"

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
        }
    }
}
