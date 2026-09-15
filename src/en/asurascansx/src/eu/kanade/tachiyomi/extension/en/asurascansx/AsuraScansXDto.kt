package eu.kanade.tachiyomi.extension.en.asurascansx

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.runCatching

private fun SimpleDateFormat.tryParse(date: String?): Long {
    return try {
        date?.let { parse(it)?.time } ?: 0L
    } catch (_: Exception) {
        0L
    }
}

@Serializable
class DataDto<T>(
    val data: T? = null,
    val meta: MetaDto? = null,
)

@Serializable
class MetaDto(
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
class MangaDetailsDto(
    val series: MangaDto,
)

@Serializable
class MangaDto(
    @SerialName("public_url")
    val publicUrl: String,
    val slug: String,
    private val title: String,
    private val cover: String,
    private val author: String? = null,
    private val artist: String? = null,
    private val description: String? = null,
    private val rating: Double? = null,
    @SerialName("alt_titles")
    @JsonNames("alternativeTitles")
    private val alternativeTitles: List<String>? = null,
    private val type: String? = null,
    private val genres: List<GenreDto>? = null,
    private val status: String? = null,
) {
    fun toSManga() = SManga.create().apply {
        title = this@MangaDto.title
        thumbnail_url = cover
        url = "/series/$slug" // Keep the old URL structure for compatibility with existing bookmarks
    }

    fun toSMangaDetails() = SManga.create().apply {
        title = this@MangaDto.title
        thumbnail_url = cover
        author = this@MangaDto.author
        artist = this@MangaDto.artist
        description = parseDescription()
        genre = buildList {
            genres?.map { it.name }?.let { addAll(it) }
            type?.replaceFirstChar { it.uppercase() }?.let { add(it) }
        }.joinToString()
        status = parseStatus()
    }

    fun parseDescription(): String = buildString {
        description
            ?.let { Jsoup.parseBodyFragment(it).text() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                if (isNotEmpty()) append("\n\n")
                append(it)
            }

        rating?.let {
            if (isNotEmpty()) append("\n\n")
            append("Rating: %.2f".format(it))
        }

        val cleanAltTitles = alternativeTitles
            ?.flatMap { title ->
                if (title.contains("•")) title.split("•") else listOf(title)
            }
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        if (!cleanAltTitles.isNullOrEmpty()) {
            if (isNotEmpty()) append("\n\n")
            append("Alternative Titles:\n")
            cleanAltTitles.joinTo(this, "\n") { "- $it" }
        }
    }
    fun parseStatus() = when (status?.lowercase()) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.COMPLETED
        "hiatus" -> SManga.ON_HIATUS
        "dropped", "axed" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }
}

@Serializable
class GenreDto(
    val name: String,
)

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

@Serializable
class ChapterListDto(
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
class ChapterDto(
    private val number: Float,
    private val title: String? = null,
    @SerialName("created_at") private val createdAt: String = "",
    @SerialName("is_premium")
    @JsonNames("is_locked")
    private val isPremium: Boolean = false,
    @SerialName("early_access_until")
    private val earlyAccessUntil: String? = null,
    @SerialName("series_slug") private val seriesSlug: String? = null,
) {
    val isLocked: Boolean
        get() {
            if (isPremium) return true
            val until = earlyAccessUntil ?: return false
            return runCatching {
                Instant.parse(until).toEpochMilli() > System.currentTimeMillis()
            }.getOrDefault(false)
        }
    fun toSChapter() = SChapter.create().apply {
        val numberStr = number.toString().removeSuffix(".0")
        url = "/series/$seriesSlug/chapter/$numberStr"
        name = buildString {
            if (isLocked) append("🔒 ")
            append("Chapter $numberStr")
            title?.let { append(" - $it") }
        }

        // Strip fractional seconds if they exist before parsing
        val cleanDate = if (createdAt.contains(".")) {
            createdAt.substringBefore(".") + "Z"
        } else {
            createdAt
        }

        date_upload = dateFormat.tryParse(cleanDate)
    }
}

@Serializable
class PremiumPageListDto(
    val data: ChapterWrapper,
) {
    @Serializable
    class ChapterWrapper(
        val chapter: PageListDto,
    )
}

@Serializable
class PageListDto(
    val pages: List<PageDto> = emptyList(),
)

@Serializable
class PageDto(
    val url: String,
    val tiles: List<Int>? = null,
    @SerialName("tile_cols")
    val tileCols: Int? = null,
    @SerialName("tile_rows")
    val tileRows: Int? = null,
)

@Serializable
class PageData(
    val tiles: List<Int>,
    val tileCols: Int,
    val tileRows: Int,
)
