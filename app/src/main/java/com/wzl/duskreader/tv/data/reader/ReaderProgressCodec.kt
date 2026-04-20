package com.wzl.duskreader.tv.data.reader

fun decodeSavedPosition(savedPos: Int, fullTextLength: Int): Int {
    if (fullTextLength <= 0) return 0

    val raw = if (savedPos in 1..100) {
        (savedPos.toDouble() / 100 * fullTextLength).toInt()
    } else {
        (savedPos.toDouble() / 1_000_000 * fullTextLength).toInt()
    }

    return raw.coerceIn(0, fullTextLength)
}

fun encodeSavedPosition(currentOffset: Int, fullTextLength: Int): Int {
    if (fullTextLength <= 0) return 0
    return (currentOffset.toDouble() / fullTextLength * 1_000_000).toInt()
}
