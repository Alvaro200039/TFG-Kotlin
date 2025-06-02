package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.BBDD_Maestra.Dao.MasterDao
import com.example.tfg_kotlin.BBDD_Maestra.Entities.Empresa

class MasterRepository(internal val empresaDao: MasterDao) {

    suspend fun insertarEmpresa(empresa: Empresa): Long {
        return empresaDao.insertarEmpresa(empresa)
    }

    suspend fun actualizarEmpresa(empresa: Empresa) {
        empresaDao.actualizarEmpresa(empresa)
    }

    suspend fun eliminarEmpresa(empresa: Empresa) {
        empresaDao.eliminarEmpresa(empresa)
    }

    suspend fun buscarPorCif(cif: String): Empresa? {
        return empresaDao.buscarPorCif(cif)
    }

    suspend fun buscarPorDominio(dominio: String): Empresa?{
        return empresaDao.buscarPorDominio(dominio)
    }

    suspend fun obtenerTodasLasEmpresas(): List<Empresa> {
        return empresaDao.obtenerTodasLasEmpresas()
    }
}
