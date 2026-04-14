package com.example.tfg_kotlin.data.model

/**
 * Enum que centraliza los tipos de elemento (sala o puesto).
 * Los modelos Sala y Reserva siguen almacenando [String] en Firestore
 * para compatibilidad, pero todo el código Kotlin debe comparar con este enum.
 */
enum class TipoElemento(val valor: String) {
    SALA("SALA"),
    PUESTO("PUESTO");

    companion object {
        fun fromString(s: String): TipoElemento =
            entries.firstOrNull { it.valor.equals(s, ignoreCase = true) } ?: SALA
    }
}
