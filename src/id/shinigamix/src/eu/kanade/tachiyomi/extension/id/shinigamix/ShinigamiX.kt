            author = getValue(mangaDetails.detailList, "Author(s)").replace("Updating", "")
            artist = getValue(mangaDetails.detailList, "Artist(s)").replace("Updating", "")
            status = getValue(mangaDetails.detailList, "Tag(s)").toStatus()
