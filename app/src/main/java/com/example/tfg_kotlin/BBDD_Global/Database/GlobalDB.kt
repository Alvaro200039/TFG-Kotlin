package com.example.tfg_kotlin.BBDD_Global.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfg_kotlin.BBDD_Global.Dao.FranjaHorariaDao
import com.example.tfg_kotlin.BBDD_Global.Dao.PisoDao
import com.example.tfg_kotlin.BBDD_Global.Dao.ReservaDao
import com.example.tfg_kotlin.BBDD_Global.Dao.SalaDao
import com.example.tfg_kotlin.BBDD_Global.Dao.UsuarioDao
import com.example.tfg_kotlin.relations.ExtrasConverter
import com.example.tfg_kotlin.BBDD_Global.Entities.FranjaHoraria
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Salas
import com.example.tfg_kotlin.BBDD_Global.Entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario


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
        @Volatile
        private var INSTANCE: GlobalDB? = null

        fun getDatabase(context: Context): GlobalDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlobalDB::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}