package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfg_kotlin.dao.EmpresaDao
import com.example.tfg_kotlin.dao.FranjaHorariaDao
import com.example.tfg_kotlin.dao.PisoDao
import com.example.tfg_kotlin.dao.UsuarioDao
import com.example.tfg_kotlin.dao.ReservaDao
import com.example.tfg_kotlin.dao.SalaDao
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.entities.ExtrasConverter
import com.example.tfg_kotlin.entities.FranjaHoraria
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Usuario
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.entities.Reserva


@Database(
    entities = [Usuario::class, Salas::class, Reserva::class, FranjaHoraria::class, Piso::class, Empresa::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ExtrasConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun salaDao(): SalaDao
    abstract fun reservaDao(): ReservaDao
    abstract fun franjahorariaDao(): FranjaHorariaDao
    abstract fun pisoDao(): PisoDao
    abstract fun empresaDao(): EmpresaDao

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