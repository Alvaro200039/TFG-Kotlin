package com.example.tfg_kotlin

import java.util.Locale

object Utils {
    fun naturalOrderKey(s: String): List<Any> {
        val regex = Regex("""(\d+|\D+)""")
        return regex.findAll(s.lowercase(Locale.ROOT)).map {
            val part = it.value
            part.toIntOrNull() ?: part
        }.toList()
    }

    fun compareNaturalKeys(a: List<Any>, b: List<Any>): Int {
        val minSize = minOf(a.size, b.size)
        for (i in 0 until minSize) {
            val comp = when {
                a[i] is Int && b[i] is Int -> (a[i] as Int).compareTo(b[i] as Int)
                a[i] is String && b[i] is String -> (a[i] as String).compareTo(b[i] as String)
                a[i] is Int && b[i] is String -> -1
                a[i] is String && b[i] is Int -> 1
                else -> 0
            }
            if (comp != 0) return comp
        }
        return a.size.compareTo(b.size)
    }
}