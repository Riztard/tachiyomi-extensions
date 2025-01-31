package eu.kanade.tachiyomi.extension.id.shinigamix2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShinigamiX2BrowseDto(
    val data: List<ShinigamiX2BrowseDataDto>,
    val meta: MetaDto,
)

@Serializable
data class ShinigamiX2BrowseDataDto(
    @SerialName("cover_image_url") val thumbnail: String? = "",
    @SerialName("manga_id") val mangaId: String? = "",
    val title: String? = "",
)

@Serializable
data class MetaDto(
    val page: Int,
    @SerialName("total_page") val totalPage: Int,
)

@Serializable
data class ShinigamiX2MangaDetailDto(
    val data: ShinigamiX2MangaDetailDataDto,
)

@Serializable
data class ShinigamiX2MangaDetailDataDto(
    val description: String = "",
//    @SerialName("alternative_title") val alternativeTitle: String = "",
    val status: Int = 0,
    val taxonomy: Map<String, List<TaxonomyItemDto>> = emptyMap(),
)

@Serializable
data class TaxonomyItemDto(
    val name: String,
)

@Serializable
data class ShinigamiX2ChapterListDto(
    @SerialName("data") val chapterList: List<ShinigamiX2ChapterListDataDto>,
    val meta: MetaDto,
)

@Serializable
data class ShinigamiX2ChapterListDataDto(
    @SerialName("release_date") val date: String = "",
    @SerialName("chapter_title") val title: String = "",
    @SerialName("chapter_number") val name: Int = 0,
    @SerialName("chapter_id") val chapterId: String = "",
)

@Serializable
data class ShinigamiX2PageListDto(
    @SerialName("data") val pageList: ShinigamiX2PagesDataDto,
)

@Serializable
data class ShinigamiX2PagesDataDto(
    @SerialName("chapter") val chapterPage: ShinigamiX2PagesData2Dto,
)

@Serializable
data class ShinigamiX2PagesData2Dto(
    val path: String,
    @SerialName("data") val pages: List<String> = emptyList(),
)
