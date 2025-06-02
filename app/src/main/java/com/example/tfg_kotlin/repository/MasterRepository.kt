package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.EmpresaDao
import com.example.tfg_kotlin.entities.Empresa

class MasterRepository (private val empresaDao: EmpresaDao) {

    suspend fun insertarEmpresa(empresa: Empresa): Long = empresaDao.insertarEmpresa(empresa)
    suspend fun obtenerEmpresaPorCif(cif: String): Empresa? = empresaDao.getEmpresaPorCif(cif)
    suspend fun obtenerTodasLasEmpresas(): List<Empresa> = empresaDao.obtenerTodasLasEmpresas()
}