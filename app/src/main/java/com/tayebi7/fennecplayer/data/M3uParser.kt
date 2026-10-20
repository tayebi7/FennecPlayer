package com.tayebi7.fennecplayer.data

import java.io.BufferedReader

/** محلل ملفات M3U / M3U8 (#EXTINF) يقسم المحتوى إلى مباشر وأفلام ومسلسلات. */
object M3uParser {

    private val ATTR = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(reader: BufferedReader): Map<ContentType, Catalog> {
        val entries = ContentType.values().associateWith { mutableListOf<Entry>() }
        val groups = ContentType.values().associateWith { LinkedHashMap<String, Int>() }

        var name = ""
        var logo: String? = null
        var group = ""
        var pending = false
        var index = 0

        reader.useLines { lines ->
            for ((lineNo, raw) in lines.withIndex()) {
                var line = raw.trim()
                if (lineNo == 0) line = line.removePrefix("﻿")
                if (line.isEmpty()) continue

                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        val info = line.substringAfter(':', "")
                        val attrs = ATTR.findAll(info).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                        val lastQuote = info.lastIndexOf('"')
                        val comma = info.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                        val title = if (comma >= 0) info.substring(comma + 1).trim() else ""
                        name = title.ifEmpty { attrs["tvg-name"].orEmpty() }
                        logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                        group = attrs["group-title"].orEmpty().trim()
                        pending = true
                    }
                    line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        if (group.isEmpty()) group = line.substringAfter(':').trim()
                    }
                    line.startsWith("#") -> Unit
                    pending && line.contains("://") -> {
                        val groupName = group.ifEmpty { "غير مصنف" }
                        val type = classify(line, groupName)
                        index++
                        entries.getValue(type).add(
                            Entry(
                                id = index.toString(),
                                name = name.ifEmpty { "Channel $index" },
                                logo = logo,
                                categoryId = groupName,
                                type = type,
                                url = line
                            )
                        )
                        val map = groups.getValue(type)
                        map[groupName] = (map[groupName] ?: 0) + 1
                        name = ""; logo = null; group = ""; pending = false
                    }
                }
            }
        }

        return ContentType.values().associateWith { t ->
            Catalog(
                categories = groups.getValue(t).map { (g, c) -> Category(g, g, c) },
                entries = entries.getValue(t)
            )
        }
    }

    private fun classify(url: String, group: String): ContentType {
        val u = url.lowercase()
        val g = group.lowercase()
        return when {
            "/movie/" in u -> ContentType.MOVIE
            "/series/" in u -> ContentType.SERIES
            "vod" in g || "movie" in g || "film" in g || "أفلام" in g || "افلام" in g -> ContentType.MOVIE
            "serie" in g || "مسلسل" in g -> ContentType.SERIES
            u.substringBefore('?').let { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".avi") } -> ContentType.MOVIE
            else -> ContentType.LIVE
        }
    }
}
