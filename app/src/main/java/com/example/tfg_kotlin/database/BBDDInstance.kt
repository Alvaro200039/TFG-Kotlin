package com.example.tfg_kotlin.database


import android.content.Context
import androidx.room.Room

object BBDDInstance {
    private val instancias = mutableMapOf<String, BBDDMaestra>()

    fun getDatabase(context: Context, nombreBD: String = "maestra_db"): BBDDMaestra {
        if (!instancias.containsKey(nombreBD)) {
            val instancia = Room.databaseBuilder(
                context.applicationContext,
                BBDDMaestra::class.java,
                nombreBD
            ).allowMainThreadQueries().build()
            instancias[nombreBD] = instancia
        }
        return instancias[nombreBD]!!
    }
}