package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.TypeConverter

class ExtrasConverter {

    @TypeConverter
    fun fromExtrasList(extras: List<String>): String {
        return Gson().toJson(extras)
    }

    @TypeConverter
    fun toExtrasList(data: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(data, listType)
    }
}