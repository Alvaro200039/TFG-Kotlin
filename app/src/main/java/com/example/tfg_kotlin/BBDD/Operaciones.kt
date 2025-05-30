package com.example.tfg_kotlin.BBDD

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface Operaciones {

//Operaciones
    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND contrasena = :contrasena")
    fun login(nombre: String, contrasena: String): TablaEmpleados?

    // Empleados
    @Insert
    fun insertarEmpleado(empleado: TablaEmpleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    fun buscarEmpleado(nombre: String, apellidos: String): TablaEmpleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    fun obtenerJefes(): List<TablaEmpleados>

    // Salas
    @Insert
    fun insertarSala(sala: TablaSalas)

    @Query("SELECT * FROM Salas_creadas")
    fun obtenerSalas(): List<TablaSalas>

    // Reservas
    @Insert
    fun reservarSala(reserva: TablaSalaReservada)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre")
    fun buscarEmpleadoPorNombre(nombre: String): TablaEmpleados?

}
