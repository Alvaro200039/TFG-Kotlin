package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfg_kotlin.daoApp.FranjaHorariaDao
import com.example.tfg_kotlin.daoApp.PisoDao
import com.example.tfg_kotlin.daoApp.ReservaDao
import com.example.tfg_kotlin.daoApp.SalaDao
import com.example.tfg_kotlin.daoApp.UsuarioDao
import com.example.tfg_kotlin.entitiesApp.ExtrasConverter
import com.example.tfg_kotlin.entitiesApp.FranjaHoraria
import com.example.tfg_kotlin.entitiesApp.Piso
import com.example.tfg_kotlin.entitiesApp.Salas
import com.example.tfg_kotlin.entitiesApp.Reserva
import com.example.tfg_kotlin.entitiesApp.Usuario
import com.example.tfg_kotlin.relations.Converters


@Database(
    entities = [Usuario::class, Salas::class, Reserva::class, FranjaHoraria::class, Piso::class],
    version = 1,
    exportSchema = false
)

@TypeConverters(ExtrasConverter::class, Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun salaDao(): SalaDao
    abstract fun reservaDao(): ReservaDao
    abstract fun franjahorariaDao(): FranjaHorariaDao
    abstract fun pisoDao(): PisoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}