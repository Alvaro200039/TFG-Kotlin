package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.EmpresaDao
import com.example.tfg_kotlin.entities.Empresa

class MasterRepository (private val empresaDao: EmpresaDao) {

    suspend fun insertarEmpresa(empresa: Empresa): Long = empresaDao.insertarEmpresa(empresa)
    suspend fun obtenerEmpresaPorCif(cif: String): Empresa? = empresaDao.getEmpresaPorCif(cif)
    suspend fun obtenerTodasLasEmpresas(): List<Empresa> = empresaDao.obtenerTodasLasEmpresas()

    suspend fun actualizarEmpresa(empresa: Empresa) {
        empresaDao.actualizarEmpresa(empresa)
    }

    suspend fun eliminarEmpresa(empresa: Empresa) {
        empresaDao.eliminarEmpresa(empresa)
    }

    suspend fun buscarPorDominio(dominio: String): Empresa?{
        return empresaDao.getEmpresaPorDominio(dominio)
    }


}