package eu.kanade.tachiyomi.extension.id.shinigamix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShinigamiXBrowseDto(
    val url: String? = "",
    val title: String? = "",
    @SerialName("cover") val thumbnail: String? = "",
)

@Serializable
data class ShinigamiXMangaDetailDto(
    @SerialName("synopsis") val description: String = "",
    val detailList: List<ShinigamiXMangaDetailListDto>? = null,
)

@Serializable
data class ShinigamiXMangaDetailListDto(
    val name: String = "",
    val value: String = "",
)

@Serializable
data class ShinigamiXChapterListDto(
    val chapterList: List<ShinigamiXChapterDto>? = null,
)

@Serializable
data class ShinigamiXChapterDto(
    @SerialName("releaseDate") val date: String = "",
    @SerialName("title") val name: String = "",
    val url: String = "",
    @SerialName("imageList") val pages: List<String> = emptyList(),
    val slug: String = "",
)
