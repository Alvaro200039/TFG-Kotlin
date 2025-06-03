package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfg_kotlin.BBDD_Global.Dao.FranjaHorariaDao
import com.example.tfg_kotlin.BBDD_Global.Dao.PisoDao
import com.example.tfg_kotlin.BBDD_Global.Dao.ReservaDao
import com.example.tfg_kotlin.BBDD_Global.Dao.SalaDao
import com.example.tfg_kotlin.relations.ExtrasConverter
import com.example.tfg_kotlin.dao.UsuarioDao
import com.example.tfg_kotlin.entities.FranjaHoraria
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Reserva
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.entities.Usuario


@Database(
    entities = [Usuario::class, Salas::class, Reserva::class, FranjaHoraria::class, Piso::class],
    version = 1,
    exportSchema = false
)

@TypeConverters(ExtrasConverter::class)
abstract class GlobalDB : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun salaDao(): SalaDao
    abstract fun reservaDao(): ReservaDao
    abstract fun franjahorariaDao(): FranjaHorariaDao
    abstract fun pisoDao(): PisoDao

    companion object {
        private val instances: MutableMap<String, GlobalDB> = mutableMapOf()

        fun getDatabase(context: Context, nombreEmpresa: String): GlobalDB {
            val nombreLimpio = "empresa_${nombreEmpresa.replace(".", "-")}"
            return instances.getOrPut(nombreLimpio) {
                Room.databaseBuilder(
                    context.applicationContext,
                    GlobalDB::class.java,
                    nombreLimpio
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}