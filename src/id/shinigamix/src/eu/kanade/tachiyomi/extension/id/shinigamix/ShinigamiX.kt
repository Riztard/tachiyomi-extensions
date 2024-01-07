            author = getValue(mangaDetails.detailList, "Author(s)").replace("Updating", "")
            artist = getValue(mangaDetails.detailList, "Artist(s)").replace("Updating", "")
            status = getValue(mangaDetails.detailList, "Tag(s)").toStatus()
        val result = response.parseAs<ShinigamiXChapterListDto>()

        return result.chapterList!!.map(::chapterFromObject)
    }
    private fun chapterFromObject(obj: ShinigamiXChapterDto): SChapter = SChapter.create().apply {
        name = obj.name
