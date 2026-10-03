package com.example.zgmusicplayer

enum class SortOrder(val label: String) {
    TITLE("Name (A–Z)"),
    TITLE_DESC("Name (Z–A)"),
    ARTIST("Artist (A–Z)"),
    DATE_ADDED("Recently added"),
    DURATION_LONG("Longest first"),
    DURATION_SHORT("Shortest first")
}

fun List<Song>.applySort(order: SortOrder): List<Song> = when (order) {
    SortOrder.TITLE -> sortedBy { it.title.lowercase() }
    SortOrder.TITLE_DESC -> sortedByDescending { it.title.lowercase() }
    SortOrder.ARTIST -> sortedWith(
        compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
    )
    SortOrder.DATE_ADDED -> sortedByDescending { it.dateAdded }
    SortOrder.DURATION_LONG -> sortedByDescending { it.duration }
    SortOrder.DURATION_SHORT -> sortedBy { it.duration }
}