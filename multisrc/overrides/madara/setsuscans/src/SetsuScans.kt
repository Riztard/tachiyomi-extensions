package eu.kanade.tachiyomi.extension.en.setsuscans

import eu.kanade.tachiyomi.multisrc.madara.Madara

class SetsuScans : Madara("Setsu Scans", "https://setsuscans.com", "en") {
    override val mangaDetailsSelectorStatus = "div.summary-heading:contains(status) + div.summary-content"
}
