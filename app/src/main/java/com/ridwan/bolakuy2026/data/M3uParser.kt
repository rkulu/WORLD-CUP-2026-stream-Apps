package com.ridwan.bolakuy2026.data

data class Channel(
    val id: String,
    val tvgId: String,
    val tvgName: String,
    val tvgLogo: String,
    val groupTitle: String,
    val name: String,
    val url: String
)

object M3uParser {
    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.lineSequence().map { it.trim() }.toList()
        
        var currentInfoLine: String? = null
        
        for (line in lines) {
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTM3U")) continue
            
            if (line.startsWith("#EXTINF:")) {
                currentInfoLine = line
            } else if (!line.startsWith("#")) {
                if (currentInfoLine != null) {
                    val channel = parseLine(currentInfoLine, line)
                    channels.add(channel)
                    currentInfoLine = null
                }
            }
        }
        return channels
    }

    private fun parseAttribute(line: String, attribute: String): String {
        val pattern = """$attribute="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.get(1) ?: ""
    }

    private fun parseLine(infoLine: String, url: String): Channel {
        val tvgId = parseAttribute(infoLine, "tvg-id")
        val tvgName = parseAttribute(infoLine, "tvg-name")
        val tvgLogo = parseAttribute(infoLine, "tvg-logo")
        val groupTitle = parseAttribute(infoLine, "group-title")
        
        val commaIndex = infoLine.lastIndexOf(',')
        val name = if (commaIndex != -1 && commaIndex < infoLine.length - 1) {
            infoLine.substring(commaIndex + 1).trim()
        } else {
            if (tvgName.isEmpty()) "Unknown Channel" else tvgName
        }
        
        val id = ((if (tvgId.isEmpty()) name else tvgId) + url).hashCode().toString()
        
        return Channel(
            id = id,
            tvgId = tvgId,
            tvgName = if (tvgName.isEmpty()) name else tvgName,
            tvgLogo = tvgLogo,
            groupTitle = if (groupTitle.isEmpty()) "General" else groupTitle,
            name = name,
            url = url
        )
    }
}
