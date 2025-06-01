package com.example.tfg_kotlin.database


import android.content.Context
import androidx.room.Room

object BBDDInstance {
    private val instancias = mutableMapOf<String, BBDD>()

    fun getDatabase(context: Context, nombreBD: String = "maestra_db"): BBDD {
        if (!instancias.containsKey(nombreBD)) {
            val instancia = Room.databaseBuilder(
                context.applicationContext,
                BBDD::class.java,
                nombreBD
            ).allowMainThreadQueries().build()
            instancias[nombreBD] = instancia
        }
        return instancias[nombreBD]!!
    }
}