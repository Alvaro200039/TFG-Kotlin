package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.tfg_kotlin.BBDD_Master.entities.Empresa

@Entity(tableName = "pisos")
data class Piso(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val empresaId: Int?,
    val imagen: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Piso

        if (id != other.id) return false
        if (empresaId != other.empresaId) return false
        if (nombre != other.nombre) return false
        if (!imagen.contentEquals(other.imagen)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (empresaId ?: 0)
        result = 31 * result + nombre.hashCode()
        result = 31 * result + (imagen?.contentHashCode() ?: 0)
        return result
    }
}
