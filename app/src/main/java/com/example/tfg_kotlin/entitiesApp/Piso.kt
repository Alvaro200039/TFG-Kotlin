package com.example.tfg_kotlin.entitiesApp

import androidx.room.*

@Entity(tableName = "pisos")
data class Piso(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val empresaCif: String,
    val imagen: ByteArray?
)
 {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Piso

        if (id != other.id) return false
        if (empresaCif != other.empresaCif) return false
        if (nombre != other.nombre) return false
        if (!imagen.contentEquals(other.imagen)) return false

        return true
    }

     override fun hashCode(): Int {
         var result = id
         result = 31 * result + empresaCif.hashCode()
         result = 31 * result + nombre.hashCode()
         result = 31 * result + (imagen?.contentHashCode() ?: 0)
         return result
     }
}
