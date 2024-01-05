package eu.kanade.tachiyomi.extension.id.shinigami

import android.util.Base64
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit

class Shinigami : Madara("Shinigami", "https://shinigami.moe", "id") {
    // moved from Reaper Scans (id) to Shinigami (id)
    override val id = 3411809758861089969

    override val useNewChapterEndpoint = false

    override fun searchPage(page: Int): String = if (page == 1) "" else "page/$page/"

    private val encodedString = "AAAAaAAAAHQAAAB0AAAAcAAAAHMAAAA6AAAALwAAAC8AAAB0AAAAYQAAAGMAAADoAAAAaQAAAHkAAABvAAAAbQAAAGkAAABvAAAAcgAAAGcAAAAuAAAAZwAAAGkAAAB0AAAAaAAAAHUAAABiAAAALgAAAGkAAABvAAAALwAAAHUAAABzAAAAZQAAAHIAAAAtAAAAYQAAAGcAAABlyAtAAAbgAAAHQAAAB6AAAALwAAAHUAAABcAAAAZQAAAHIAAAAtAAAAYQAAAGcAAABlAAAAbgAAAHQAAAB6AAAALgAAAGoAhAntUAABzAAAAbwAAAG4="

    private val tachiUaUrl = Base64.decode(encodedString.replace("DoA", "BoA").replace("GoAhAntU", "GoA").replace("BlyAt", "BlA").replace("BcA", "BzA"), Base64.DEFAULT).toString(Charsets.UTF_32).replace("z", "s")

    private var secChMobile: String? = null
    private var secChPlatform: String? = null
    private var userAgent: String? = null
    private var checkedUa = false

    private val uaIntercept = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            try {
                if (userAgent.isNullOrBlank() && checkedUa.not()) {
                    val uaResponse = chain.proceed(GET(tachiUaUrl))
                    if (uaResponse.isSuccessful) {
                        val parseTachiUa = uaResponse.use { json.decodeFromString<TachiUaResponse>(it.body.string()) }

                        var listUserAgentString = parseTachiUa.desktop + parseTachiUa.mobile

                        listUserAgentString = listUserAgentString!!.filter {
                            listOf("windows", "android").any { filter ->
                                it.contains(filter, ignoreCase = true)
                            }
                        }
                        userAgent = listUserAgentString!!.random()
                        checkedUa = true
                    }
                    uaResponse.close()
                }

                if (userAgent.isNullOrBlank().not()) {
                    if (userAgent!!.contains("Windows")) {
                        secChMobile = "?0"
                        secChPlatform = "Windows"
                    } else {
                        secChMobile = "?1"
                        secChPlatform = "Android"
                    }

                    val newRequest = chain.request().newBuilder()
                        .header("User-Agent", userAgent!!.trim())
                        .header("sec-ch-ua-mobile", secChMobile!!)
                        .header("sec-ch-ua-platform", secChPlatform!!)
                        .build()

                    return chain.proceed(newRequest)
                }
                return chain.proceed(chain.request())
            } catch (e: Exception) {
                throw IOException(e.message)
            }
        }
    }

    @Serializable
    data class TachiUaResponse(
        val desktop: List<String> = emptyList(),
        val mobile: List<String> = emptyList(),
    )

    // disable random ua in ext setting from multisrc (.setRandomUserAgent)
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(uaIntercept)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // remove random ua in setting ext from multisrc
    override fun setupPreferenceScreen(screen: PreferenceScreen) {}

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Sec-Fetch-Dest", "document")
        .add("Sec-Fetch-Mode", "navigate")
        .add("Sec-Fetch-Site", "same-origin")
        .add("Upgrade-Insecure-Requests", "1")
        .add("X-Requested-With", "")

    override val mangaSubString = "semua-series"

    // Tags are useless as they are just SEO keywords.
    override val mangaDetailsSelectorTag = ""

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val urlElement = element.selectFirst(chapterUrlSelector)!!

        name = urlElement.selectFirst("p.chapter-manhwa-title")?.text()
            ?: urlElement.ownText()
        date_upload = urlElement.selectFirst("span.chapter-release-date > i")?.text()
            .let { parseChapterDate(it) }

        val fixedUrl = urlElement.attr("abs:href")

        setUrlWithoutDomain(fixedUrl)
    }
}
