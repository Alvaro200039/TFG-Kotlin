package com.example.tfg_kotlin.BBDD_Global.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodos(): List<Usuario>

    @Update
    suspend fun actualizar(usuario: Usuario)

    @Delete
    suspend fun eliminar(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE id = :idUsuario LIMIT 1")
    suspend fun getUsuarioById(idUsuario: Int): Usuario?

    @Query("SELECT * FROM usuarios WHERE nombre = :nombre LIMIT 1")
    suspend fun getUsuarioByNombre(nombre: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getUsuarioByEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getPrimerUsuario(): Usuario?

    @Query("SELECT * FROM usuarios WHERE nombre = :nombre AND apellidos = :apellidos")
    suspend fun buscarEmpleado(nombre: String, apellidos: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE esJefe = 1")
    suspend fun buscarJefes(): List<Usuario>

    @Query("SELECT * FROM usuarios WHERE email = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): Usuario?

}