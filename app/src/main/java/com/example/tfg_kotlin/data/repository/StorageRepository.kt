package com.example.tfg_kotlin.data.repository

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {

    suspend fun uploadPisoImagen(empresaId: String, pisoId: String, imageBytes: ByteArray): String? {
        return try {
            val ref = storage.reference.child("empresas/$empresaId/pisos/$pisoId/plano.jpg")
            ref.putBytes(imageBytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePisoImagen(imageUrl: String): Boolean {
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
