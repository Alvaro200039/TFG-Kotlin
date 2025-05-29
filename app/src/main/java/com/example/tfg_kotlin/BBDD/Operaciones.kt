package com.example.tfg_kotlin.BBDD

import androidx.room.*

@Dao
interface Operaciones {

// Operaciones BD Maestra Registrio empresas
    @Insert
    fun insertarEmpresa(empresa: TablaEmpresa)

    @Query("SELECT * FROM Empresas WHERE dominio = :dominio")
    fun buscarPorDominio(dominio: String): TablaEmpresa?

    @Query("SELECT * FROM Empresas WHERE cif = :cif")
    fun buscarPorCif(cif: String): TablaEmpresa?

    @Query("SELECT * FROM Empresas")
    fun obtenerTodas(): List<TablaEmpresa>

/*
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

    @Query("""
        SELECT Empleados.nombre, Empleados.apellidos
        FROM Salas_reservadas
        INNER JOIN Empleados ON Salas_reservadas.idPersona = Empleados.correo
        WHERE Salas_reservadas.idSala = :salaId
    """)
    fun obtenerPersonasPorSala(salaId: Int): List<TablaEmpleados>


    @Query("SELECT * FROM Empleados WHERE nombre = :nombre")
    fun buscarEmpleadoPorNombre(nombre: String): TablaEmpleados?


*/

}
