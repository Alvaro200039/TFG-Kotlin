package com.example.tfg_kotlin.data.repository

import android.util.Log
import com.example.tfg_kotlin.data.model.Empresa
import com.example.tfg_kotlin.data.model.FranjaHoraria
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        private const val TAG = "FirestoreRepository"
    }

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
        val doc = empresaRef.get().await()
        if (!doc.exists()) return null
        
        return try {
            doc.toObject(Empresa::class.java)?.apply { this.nombre = doc.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Empresa ${doc.id}", e)
            Empresa(
                cif = doc.getString("cif") ?: "",
                dominio = doc.getString("dominio") ?: "",
                nombre = doc.id,
                apertura = doc.getString("apertura") ?: "08:00",
                cierre = doc.getString("cierre") ?: "20:00",
                diasApertura = (doc.get("diasApertura") as? List<*>)?.filterIsInstance<Number>()?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5),
                diasBloqueados = (doc.get("diasBloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                stepSize = doc.getDouble("stepSize")?.toFloat() ?: 0.5f,
                maxDuration = doc.getLong("maxDuration")?.toInt() ?: 2
            )
        }
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
        
        return snapshot.mapNotNull { FranjaHoraria(hora = it.id) }
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
            doc.toObject(Sala::class.java)?.apply { 
                id = doc.id 
                idPiso = pisoId
            }
        }
    }

    suspend fun getEmpresaByNombre(nombre: String): Empresa? {
        val doc = db.collection("empresas").document(nombre).get().await()
        if (!doc.exists()) return null
        
        return try {
            doc.toObject(Empresa::class.java)?.apply { this.nombre = doc.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Empresa ${doc.id}", e)
            Empresa(
                cif = doc.getString("cif") ?: "",
                dominio = doc.getString("dominio") ?: "",
                nombre = doc.id,
                apertura = doc.getString("apertura") ?: "08:00",
                cierre = doc.getString("cierre") ?: "20:00",
                diasApertura = (doc.get("diasApertura") as? List<*>)?.filterIsInstance<Number>()?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5),
                diasBloqueados = (doc.get("diasBloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                stepSize = doc.getDouble("stepSize")?.toFloat() ?: 0.5f,
                maxDuration = doc.getLong("maxDuration")?.toInt() ?: 2
            )
        }
    }


    suspend fun savePiso(empresaId: String, piso: Piso): String? {
        return try {
            if (piso.id != null) {
                db.collection("empresas")
                    .document(empresaId)
                    .collection("pisos")
                    .document(piso.id!!)
                    .set(piso)
                    .await()
                piso.id
            } else {
                val ref = db.collection("empresas")
                    .document(empresaId)
                    .collection("pisos")
                    .document()
                
                piso.id = ref.id
                ref.set(piso).await()
                ref.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving piso", e)
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
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveSala(empresaId: String, pisoId: String, sala: Sala): Boolean {
        return try {
            val docId = sala.id ?: db.collection("empresas").document(empresaId).collection("pisos").document(pisoId).collection("salas").document().id
            sala.id = docId
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
            Log.e(TAG, "Error in saveSala", e)
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
            Log.e(TAG, "Error in deleteSala", e)
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
            Log.e(TAG, "Error in saveEmpresa", e)
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
            Log.e(TAG, "Error in createUsuario", e)
            false
        }
    }
}
