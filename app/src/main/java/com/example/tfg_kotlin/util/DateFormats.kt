package com.example.tfg_kotlin.util

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formateadores de fecha centralizados para evitar la creación repetida de
 * [SimpleDateFormat] (14+ instancias dispersas por el código).
 *
 * Cada propiedad usa [ThreadLocal] para garantizar seguridad en hilos.
 */
object DateFormats {

    /** Formato día: "dd/MM/yyyy" */
    val dayFormat: SimpleDateFormat
        get() = dayFormatTL.get()!!

    /** Formato completo: "dd/MM/yyyy HH:mm" */
    val fullFormat: SimpleDateFormat
        get() = fullFormatTL.get()!!

    /** Formato mes-año: "MMMM yyyy" */
    val monthYearFormat: SimpleDateFormat
        get() = monthYearFormatTL.get()!!

    // --- ThreadLocal instances ---

    private val dayFormatTL = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

    private val fullFormatTL = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    private val monthYearFormatTL = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
}
