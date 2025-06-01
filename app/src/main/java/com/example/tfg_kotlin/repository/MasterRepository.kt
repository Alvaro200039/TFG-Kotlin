package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.daoMaster.EmpresaDao
import com.example.tfg_kotlin.entitiesMaster.Empresa

class MasterRepository(internal val empresaDao: EmpresaDao) {

    suspend fun insertarEmpresa(empresa: Empresa): Long {
        return empresaDao.insertarEmpresa(empresa)
    }

    suspend fun actualizarEmpresa(empresa: Empresa) {
        empresaDao.actualizarEmpresa(empresa)
    }

    suspend fun eliminarEmpresa(empresa: Empresa) {
        empresaDao.eliminarEmpresa(empresa)
    }

    suspend fun obtenerEmpresaPorId(cif: String): Empresa? {
        return empresaDao.obtenerEmpresaPorCif(cif)
    }

    suspend fun obtenerTodasLasEmpresas(): List<Empresa> {
        return empresaDao.obtenerTodasLasEmpresas()
    }
}
