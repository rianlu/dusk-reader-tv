package com.wzl.duskreader.tv.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverter：List<String> <-> JSON 字符串。
 * 给 Book.tags 提供序列化支持。
 */
class BookTypeConverters {

    @TypeConverter
    fun tagsToJson(tags: List<String>): String = Json.encodeToString(ListSerializer(String.serializer()), tags)

    @TypeConverter
    fun jsonToTags(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(String.serializer()), value)
}
