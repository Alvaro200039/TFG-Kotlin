package com.example.tfg_kotlin.BBDD

import androidx.room.*

@Dao
interface Operaciones {


    // Empleados
    @Insert
    fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    fun buscarEmpleado(nombre: String, apellidos: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    fun obtenerJefes(): List<Empleados>

    @Query("SELECT * FROM Empleados WHERE correo = :correo AND contrasena = :contrasena")
    fun loginUsuario(correo: String, contrasena: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE cif = :cif AND esJefe = 1")
    fun existeEmpresaConCif(cif: String): Empleados?


    @Query("SELECT * FROM Empleados WHERE :dominio = SUBSTR(correo, INSTR(correo, '@') + 1) AND esJefe = 1")
    fun getEmpresaPorDominio(dominio: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    fun buscarEmpleadoPorCorreo(correo: String): Empleados?

    @Insert
    fun insertarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM Empresa WHERE cif = :cif")
    fun getEmpresaPorCif(cif: String): Empresa?

    fun construirNombreBD(dominio: String): String {
        return "empresa_${dominio.lowercase()}.db"
    }
    @Query("UPDATE Empleados SET contrasena = :nuevaPass WHERE correo = :correo")
    fun actualizarContrasena(correo: String, nuevaPass: String)



    // Salas
    /**
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
    **/
}
