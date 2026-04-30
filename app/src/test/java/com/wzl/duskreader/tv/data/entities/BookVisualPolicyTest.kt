package com.wzl.duskreader.tv.data.entities

import org.junit.Assert.assertEquals
import org.junit.Test

class BookVisualPolicyTest {

    @Test
    fun preferredBackdropPath_prefersExplicitBackdrop() {
        val book = Book(
            title = "Test",
            path = "/tmp/test.txt",
            format = "TXT",
            coverPath = "/covers/cover.png",
            backdropPath = "/backdrops/backdrop.png",
        )

        assertEquals("/backdrops/backdrop.png", book.preferredBackdropPath())
    }

    @Test
    fun preferredBackdropPath_fallsBackToCoverPath() {
        val book = Book(
            title = "Test",
            path = "/tmp/test.txt",
            format = "TXT",
            coverPath = "/covers/cover.png",
        )

        assertEquals("/covers/cover.png", book.preferredBackdropPath())
    }
}
