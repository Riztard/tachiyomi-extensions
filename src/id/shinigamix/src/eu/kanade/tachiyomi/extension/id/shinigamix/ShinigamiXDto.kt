package eu.kanade.tachiyomi.extension.id.shinigamix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShinigamiXBrowseDto(
    val data: List<ShinigamiXBrowseDataDto>,
    val meta: MetaDto,
)

@Serializable
data class ShinigamiXBrowseDataDto(
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
data class ShinigamiXMangaDetailDto(
    val data: ShinigamiXMangaDetailDataDto,
)

@Serializable
data class ShinigamiXMangaDetailDataDto(
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
data class ShinigamiXChapterListDto(
    @SerialName("data") val chapterList: List<ShinigamiXChapterListDataDto>,
    val meta: MetaDto,
)

@Serializable
data class ShinigamiXChapterListDataDto(
    @SerialName("release_date") val date: String = "",
    @SerialName("chapter_title") val title: String = "",
    @SerialName("chapter_number") val name: Double = 0.0,
    @SerialName("chapter_id") val chapterId: String = "",
)

@Serializable
data class ShinigamiXPageListDto(
    @SerialName("data") val pageList: ShinigamiXPagesDataDto,
)

@Serializable
data class ShinigamiXPagesDataDto(
    @SerialName("chapter") val chapterPage: ShinigamiXPagesData2Dto,
)

@Serializable
data class ShinigamiXPagesData2Dto(
    val path: String,
    @SerialName("data") val pages: List<String> = emptyList(),
)
