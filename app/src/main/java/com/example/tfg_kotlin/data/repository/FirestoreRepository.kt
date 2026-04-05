package com.example.tfg_kotlin.data.repository

import com.example.tfg_kotlin.data.model.Empresa
import com.example.tfg_kotlin.data.model.FranjaHoraria
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun getUsuarioByEmail(email: String): Usuario? {
        val snapshot = db.collectionGroup("usuarios")
            .whereEqualTo("email", email)
            .get()
            .await()
        
        return if (snapshot.isEmpty) null else snapshot.documents[0].toObject(Usuario::class.java)
    }

    suspend fun getEmpresaByUsuarioEmail(email: String): Empresa? {
        val snapshot = db.collectionGroup("usuarios")
            .whereEqualTo("email", email)
            .get()
            .await()
        
        if (snapshot.isEmpty) return null
        
        val empresaRef = snapshot.documents[0].reference.parent.parent ?: return null
        val empresaDoc = empresaRef.get().await()
        return empresaDoc.toObject(Empresa::class.java)?.apply { nombre = empresaDoc.id }
    }

    suspend fun getPisosByEmpresa(nombreEmpresa: String): List<Piso> {
        val snapshot = db.collection("empresas")
            .document(nombreEmpresa)
            .collection("pisos")
            .get()
            .await()
        
        return snapshot.mapNotNull { it.toObject(Piso::class.java).apply { id = it.id } }
    }

    suspend fun getFranjasByEmpresa(nombreEmpresa: String): List<FranjaHoraria> {
        val snapshot = db.collection("empresas")
            .document(nombreEmpresa)
            .collection("franjasHorarias")
            .get()
            .await()
        
        return snapshot.mapNotNull { it.toObject(FranjaHoraria::class.java) }
    }

    suspend fun getSalasByPiso(empresaId: String, pisoId: String): List<Sala> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(pisoId)
            .collection("salas")
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Sala::class.java)?.apply { id = doc.id }
        }
    }

    suspend fun getEmpresaByNombre(nombre: String): Empresa? {
        val doc = db.collection("empresas").document(nombre).get().await()
        return if (doc.exists()) doc.toObject(Empresa::class.java)?.apply { this.nombre = doc.id } else null
    }

    suspend fun addFranja(empresaId: String, franjaId: String): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("franjasHorarias")
                .document(franjaId)
                .set(mapOf("activo" to true))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFranja(empresaId: String, franjaId: String): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("franjasHorarias")
                .document(franjaId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun savePiso(empresaId: String, piso: Piso): String? {
        return try {
            val pisoData = hashMapOf(
                "nombre" to piso.nombre,
                "empresaCif" to piso.empresaCif,
                "imagenUrl" to piso.imagenUrl
            )
            if (piso.id != null) {
                db.collection("empresas")
                    .document(empresaId)
                    .collection("pisos")
                    .document(piso.id!!)
                    .set(pisoData)
                    .await()
                piso.id
            } else {
                val ref = db.collection("empresas")
                    .document(empresaId)
                    .collection("pisos")
                    .add(pisoData)
                    .await()
                ref.id
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePiso(empresaId: String, pisoId: String): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("pisos")
                .document(pisoId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveSala(empresaId: String, pisoId: String, sala: Sala): Boolean {
        return try {
            val docId = sala.id ?: db.collection("empresas").document(empresaId).collection("pisos").document(pisoId).collection("salas").document().id
            db.collection("empresas")
                .document(empresaId)
                .collection("pisos")
                .document(pisoId)
                .collection("salas")
                .document(docId)
                .set(sala)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getEmpresaByDominio(dominio: String): Empresa? {
        val snapshot = db.collection("empresas")
            .whereEqualTo("dominio", dominio)
            .get()
            .await()
        
        if (snapshot.isEmpty) return null
        val doc = snapshot.documents[0]
        return doc.toObject(Empresa::class.java)?.apply { nombre = doc.id }
    }

    suspend fun getEmpresaByCif(cif: String): Empresa? {
        val snapshot = db.collection("empresas")
            .whereEqualTo("cif", cif)
            .get()
            .await()
        
        if (snapshot.isEmpty) return null
        val doc = snapshot.documents[0]
        return doc.toObject(Empresa::class.java)?.apply { nombre = doc.id }
    }

    suspend fun saveEmpresa(empresa: Empresa): Boolean {
        return try {
            db.collection("empresas")
                .document(empresa.nombre)
                .set(empresa)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createUsuario(empresaNombre: String, usuario: Usuario): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaNombre)
                .collection("usuarios")
                .document(usuario.email)
                .set(usuario)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSala(empresaId: String, pisoId: String, salaId: String): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("pisos")
                .document(pisoId)
                .collection("salas")
                .document(salaId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
