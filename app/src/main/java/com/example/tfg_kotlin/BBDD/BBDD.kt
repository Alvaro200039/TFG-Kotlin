package com.example.tfg_kotlin.BBDD

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase



@Database(
    entities = [Empleados::class],
    version = 2
)
abstract class BBDD : RoomDatabase() {
    abstract fun appDao(): Operaciones

}
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Añadir columnas nuevas con valor por defecto vacío
        database.execSQL("ALTER TABLE Empleados ADD COLUMN apellidos TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE Empleados ADD COLUMN nif TEXT NOT NULL DEFAULT ''")
    }
}


// Para conectar a la bases de datos en las activity debemos pegar el siguiente código

// Antes del Override fun OnCreate
//  lateinit var database: Operaciones

// dentro del OnCreate
/*
database = Room.databaseBuilder(
            applicationContext, BBDD::class.java, "reservas_db"
            ).allowMainThreadQueries().build().appDao()
*/
