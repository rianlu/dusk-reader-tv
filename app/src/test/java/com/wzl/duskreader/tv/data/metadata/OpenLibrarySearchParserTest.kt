package com.wzl.duskreader.tv.data.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLibrarySearchParserTest {
    @Test
    fun parsesExactTitleAndAuthorMatch() {
        val json = """
            {"docs":[
                {"key":"/works/OL45883W","title":"The Adventures of Sherlock Holmes","author_name":["Arthur Conan Doyle"],"cover_i":12345},
                {"key":"/works/OL00000W","title":"The Adventures of Sherlock Holmes","author_name":["Other Author"],"cover_i":999}
            ]}
        """.trimIndent()

        val match = OpenLibrarySearchParser.parse(
            json = json,
            expectedTitle = "The Adventures of Sherlock Holmes",
            expectedAuthor = "Arthur Conan Doyle",
        )

        assertEquals("The Adventures of Sherlock Holmes", match?.title)
        assertEquals(listOf("Arthur Conan Doyle"), match?.authors)
        assertEquals(12345, match?.coverId)
        assertEquals("/works/OL45883W", match?.key)
    }

    @Test
    fun rejectsDifferentAuthorWhenAuthorIsProvided() {
        val json = """
            {"docs":[
                {"key":"/works/OL00000W","title":"The Adventures of Sherlock Holmes","author_name":["Other Author"],"cover_i":999}
            ]}
        """.trimIndent()

        val match = OpenLibrarySearchParser.parse(
            json = json,
            expectedTitle = "The Adventures of Sherlock Holmes",
            expectedAuthor = "Arthur Conan Doyle",
        )

        assertNull(match)
    }

    @Test
    fun acceptsTitleOnlyWhenAuthorIsMissing() {
        val json = """
            {"docs":[
                {"key":"/works/OL45883W","title":"The Adventures of Sherlock Holmes","author_name":["Arthur Conan Doyle"],"cover_i":12345}
            ]}
        """.trimIndent()

        val match = OpenLibrarySearchParser.parse(
            json = json,
            expectedTitle = "The Adventures of Sherlock Holmes",
            expectedAuthor = null,
        )

        assertEquals(12345, match?.coverId)
    }
}
