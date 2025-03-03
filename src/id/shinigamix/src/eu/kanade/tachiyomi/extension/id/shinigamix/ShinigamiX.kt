package eu.kanade.tachiyomi.extension.id.shinigamix

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
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
import org.json.JSONObject
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ShinigamiX : ConfigurableSource, HttpSource() {

    // aplikasi premium shinigami ID APK free gratis

    override val name = "Shinigami X"

    override val baseUrl by lazy { getPrefBaseUrl() }

    private var defaultBaseUrl = "https://app.shinigami.asia"

    private val apiUrl = "https://api.shngm.io"

    private val cdnUrl = "https://storage.shngm.id"

    override val lang = "id"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val apiHeaders: Headers by lazy { apiHeadersBuilder().build() }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()
            val headers = request.headers.newBuilder().apply {
                removeAll("X-Requested-With")
            }.build()

            chain.proceed(request.newBuilder().headers(headers).build())
        }
        .rateLimit(24, 1, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("X-Requested-With", randomValue[Random.nextInt(randomValue.size)])

    private val randomValue = listOf("com.opera.gx", "com.mi.globalbrowser.mini", "com.opera.browser", "com.duckduckgo.mobile.android", "com.brave.browser", "com.vivaldi.browser", "com.android.chrome")

    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.3"

    private fun fetchUserAgents(): Pair<List<String>, List<String>> {
        val client = OkHttpClient()
        val request = Request.Builder().url(USER_AGENT_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch user-agents")
            val jsonData = JSONObject(response.body?.string() ?: throw Exception("Empty response"))
            val desktopAgents = jsonData.getJSONArray("desktop")
            val mobileAgents = jsonData.getJSONArray("mobile")

            val desktopList = List(desktopAgents.length()) { i -> desktopAgents.getString(i) }
            val mobileList = List(mobileAgents.length()) { i -> mobileAgents.getString(i) }

            return Pair(desktopList, mobileList)
        }
    }

    private fun getRandomUserAgent(
        desktopAgents: List<String>,
        mobileAgents: List<String>,
    ): String {
        return if (Random.nextInt(100) < 70) { // 70% chance for desktop
            desktopAgents.random()
        } else {
            mobileAgents.random()
        }
    }

    private val userAgent: String by lazy {
        try {
            val (desktopAgents, mobileAgents) = fetchUserAgents()
            getRandomUserAgent(desktopAgents, mobileAgents)
        } catch (t: Throwable) {
            defaultUserAgent
        }
    }

    private fun apiHeadersBuilder(): Headers.Builder = headersBuilder()
        .add("Accept", "application/json")
        .add("DNT", "1")
        .add("Origin", baseUrl)
        .add("Sec-GPC", "1")
        .add("User-Agent", userAgent)

    override fun popularMangaRequest(page: Int): Request {
        val url = "$apiUrl/$API_BASE_PATH/manga/list".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("page_size", "30")
            .addQueryParameter("sort", "popularity")
            .toString()

        return GET(url, apiHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val rootObject = response.parseAs<ShinigamiXBrowseDto>()
        val projectList = rootObject.data.map(::popularMangaFromObject)

        val hasNextPage = rootObject.meta.page < rootObject.meta.totalPage

        return MangasPage(projectList, hasNextPage)
    }

    private fun popularMangaFromObject(obj: ShinigamiXBrowseDataDto): SManga = SManga.create().apply {
        title = obj.title.toString()
        thumbnail_url = obj.thumbnail
        url = "$apiUrl/$API_BASE_PATH/manga/detail/" + obj.mangaId
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiUrl/$API_BASE_PATH/manga/list".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("page_size", "30")
            .addQueryParameter("sort", "latest")
            .toString()

        return GET(url, apiHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiUrl/$API_BASE_PATH/manga/list".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("page_size", "30")

        if (query.isNotEmpty()) {
            url.addQueryParameter("q", query)
        }

        return GET(url.toString(), apiHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    override fun getMangaUrl(manga: SManga): String {
        return "$baseUrl/series/" + manga.url.substringAfter("manga/detail/")
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        // Migration from old api urls to the new one
        if (manga.url.startsWith("https://shinigami0")) {
            throw Exception("Migrate dari $name ke $name (ekstensi yang sama)")
        }
        return GET(manga.url, apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val mangaDetailsResponse = response.parseAs<ShinigamiXMangaDetailDto>()
        val mangaDetails = mangaDetailsResponse.data

        return SManga.create().apply {
            author = mangaDetails.taxonomy["Author"]?.joinToString(", ") { it.name }.orEmpty()
            artist = mangaDetails.taxonomy["Artist"]?.joinToString(", ") { it.name }.orEmpty()
            status = mangaDetails.status.toStatus()
            description = mangaDetails.description
            // + "\n\nAlternative Title: " + mangaDetails.alternativeTitle

            val genres = mangaDetails.taxonomy["Genre"]?.joinToString(", ") { it.name }.orEmpty()
            val type = mangaDetails.taxonomy["Format"]?.joinToString(", ") { it.name }.orEmpty()
            genre = listOf(genres, type).filter { it.isNotBlank() }.joinToString(", ")
        }
    }

    private fun Int.toStatus(): Int {
        return when (this) {
            1 -> SManga.ONGOING
            2 -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }
    private val random = Random.Default

    override fun chapterListRequest(manga: SManga): Request {
        val randomPageSize = random.nextInt(2000, 9000)

        return GET(
            "$apiUrl/$API_BASE_PATH/chapter/" + manga.url.substringAfter("manga/detail/") +
                "/list?page_size=$randomPageSize",
            apiHeaders,
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<ShinigamiXChapterListDto>()

        return result.chapterList.map(::chapterFromObject)
    }

    private fun chapterFromObject(obj: ShinigamiXChapterListDataDto): SChapter = SChapter.create().apply {
        date_upload = obj.date.toDate()
        name = "Chapter ${obj.name.toString().replace(".0","")} ${obj.title}"
        url = "$apiUrl/$API_BASE_PATH/chapter/detail/" + obj.chapterId
    }

    override fun pageListRequest(chapter: SChapter): Request {
        // Migration from old api urls to the new one
        if (chapter.url.contains("api/v2/chapter?url=https://")) {
            throw Exception("Migrate dari $name ke $name (ekstensi yang sama)")
        }

        return GET(chapter.url, apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<ShinigamiXPageListDto>()

        return result.pageList.chapterPage.pages.mapIndexedNotNull { index, imageName ->
            // Exclude static image starts with 999 like "999-2-b2c059.jpg"
            if (imageName.startsWith("999-")) {
                null
            } else {
                Page(index = index, imageUrl = "$cdnUrl${result.pageList.chapterPage.path}$imageName")
            }
        }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .add("DNT", "1")
            .add("referer", "$baseUrl/")
            .add("sec-fetch-dest", "empty")
            .add("Sec-GPC", "1")
            .add("User-Agent", userAgent)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body.string())
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER_V2.parse(this)?.time }.getOrNull()
            ?: runCatching { DATE_FORMATTER.parse(this)?.time }.getOrNull()
            ?: 0
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val baseUrlPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = BASE_URL_PREF
            title = BASE_URL_PREF_TITLE
            summary = BASE_URL_PREF_SUMMARY
            this.setDefaultValue(defaultBaseUrl)
            dialogTitle = BASE_URL_PREF_TITLE
            dialogMessage = "Default: $defaultBaseUrl"

            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, RESTART_APP, Toast.LENGTH_LONG).show()
                true
            }
        }
        screen.addPreference(baseUrlPref)
    }

    private fun getPrefBaseUrl(): String =
        preferences.getString(BASE_URL_PREF, defaultBaseUrl)!!.trimEnd('/')

    init {
        preferences.getString(DEFAULT_BASE_URL_PREF, null).let { prefDefaultBaseUrl ->
            if (prefDefaultBaseUrl != defaultBaseUrl) {
                preferences.edit()
                    .putString(BASE_URL_PREF, defaultBaseUrl)
                    .putString(DEFAULT_BASE_URL_PREF, defaultBaseUrl)
                    .apply()
            }
        }
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
        }
        private val DATE_FORMATTER_V2 by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        }

        private const val API_BASE_PATH = "v1"
        private const val USER_AGENT_URL = "https://keiyoushi.github.io/user-agents/user-agents.json"
        private const val RESTART_APP = "Restart aplikasi untuk menerapkan perubahan."
        private const val BASE_URL_PREF_TITLE = "Ubah Domain"
        private const val BASE_URL_PREF = "overrideBaseUrl"
        private const val BASE_URL_PREF_SUMMARY = "Untuk penggunaan sementara. Memperbarui ekstensi akan menghapus pengaturan. \n\n❗ Restart aplikasi untuk menerapkan perubahan. ❗"
        private const val DEFAULT_BASE_URL_PREF = "defaultBaseUrl"
    }
}
