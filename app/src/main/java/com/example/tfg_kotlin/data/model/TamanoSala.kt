package com.example.tfg_kotlin.data.model

/**
 * Enum que centraliza los tamaños posibles de una sala.
 * Elimina el riesgo de errores tipográficos.
 */
enum class TamanoSala(val etiqueta: String) {
    PEQUENA("Pequeña"),
    MEDIANA("Mediana"),
    GRANDE("Grande");

    companion object {
        /** Devuelve las etiquetas como array para usar en Spinners. */
        fun etiquetas(): Array<String> = entries.map { it.etiqueta }.toTypedArray()
    }
}
