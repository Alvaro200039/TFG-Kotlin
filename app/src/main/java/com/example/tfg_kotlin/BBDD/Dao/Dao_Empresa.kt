package com.example.tfg_kotlin.BBDD.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.BBDD.entities.TablaEmpleados
import com.example.tfg_kotlin.BBDD.entities.TablaSalaReservada
import com.example.tfg_kotlin.BBDD.entities.TablaSalas


@Dao
interface Dao_Empresa {

//Operaciones
    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND contrasena = :contrasena")
    fun login(nombre: String, contrasena: String): TablaEmpleados?

    // Empleados
    @Insert
    suspend fun insertarEmpleado(empleado: TablaEmpleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    suspend fun buscarEmpleado(nombre: String, apellidos: String): TablaEmpleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    suspend fun obtenerJefes(): List<TablaEmpleados>

    @Query("SELECT * FROM Empleados WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): TablaEmpleados?

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
