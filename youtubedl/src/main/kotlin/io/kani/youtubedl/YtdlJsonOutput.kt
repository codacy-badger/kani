package io.kani.youtubedl

import java.net.URL

// --ignore-config --all-subs --no-color -J

// --geo-bypass
// --proxy
//

/**
 * Represents output of `youtube-dl -J <link>`
 * for any reference see
 * [this](https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/common.py#L79)
 */
data class YtdlJsonOutput(
    val id: String,
    val title: String,
    val url: URL?
)


//val url = URL("")
//val dl = Youtubedl(url)
//val formats = dl.fetchFormats()
