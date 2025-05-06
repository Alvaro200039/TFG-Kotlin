package com.example.tfg_kotlin.BBDD

import androidx.room.*

@Dao
interface Operaciones {

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND contrasena = :contrasena")
    fun login(nombre: String, contrasena: String): Empleados?

    // Empleados
    @Insert
    fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    fun buscarEmpleado(nombre: String, apellidos: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    fun obtenerJefes(): List<Empleados>

    // Salas
    @Insert
    fun insertarSala(sala: Salas)

    @Query("SELECT * FROM Salas_creadas")
    fun obtenerSalas(): List<Salas>

    // Reservas
    @Insert
    fun reservarSala(reserva: SalaReservada)

    @Query("""
        SELECT Empleados.nombre, Empleados.apellidos, Empleados.seccion
        FROM Salas_reservadas
        INNER JOIN Empleados ON Salas_reservadas.idPersona = Empleados.id
        WHERE Salas_reservadas.idSala = :salaId
    """)
    fun obtenerPersonasPorSala(salaId: Int): List<Empleados>


    @Query("SELECT * FROM Empleados WHERE nombre = :nombre")
    fun buscarEmpleadoPorNombre(nombre: String): Empleados?
}
