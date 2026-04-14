package com.example.tfg_kotlin.data.repository

import android.util.Log
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.util.DateFormats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class ReservationRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        private const val TAG = "ReservationRepository"
    }

    suspend fun getReservationsByUser(empresaId: String, userId: String): List<Reserva> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .whereEqualTo("idUsuario", userId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
    }

    suspend fun getReservationsByEmpresa(empresaId: String): List<Reserva> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
    }

    suspend fun getReservationsByDay(empresaId: String, fecha: String): List<Reserva> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .whereGreaterThanOrEqualTo("fechaHora", "$fecha ")
            .whereLessThanOrEqualTo("fechaHora", "$fecha ~") // ~ es mayor que 23:59 lexicográficamente
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
    }


    suspend fun addReservation(empresaId: String, reserva: Reserva): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .add(reserva)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reservation", e)
            false
        }
    }

    suspend fun cancelReservation(empresaId: String, reservaId: String): Boolean {
        return try {
            db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .document(reservaId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reservation", e)
            false
        }
    }

    suspend fun deletePastReservations(empresaId: String) {
        try {
            val format = DateFormats.fullFormat
            
            // Obtener la hora de cierre de la empresa para los puestos flex
            val empresaDoc = db.collection("empresas").document(empresaId).get().await()
            val cierreStr = empresaDoc.getString("cierre") ?: "23:59"
            
            val snapshot = db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .get()
                .await()
            
            val now = Date()
            val batch = db.batch()
            var hasOperations = false

            snapshot.documents.forEach { doc ->
                val it = doc.toObject(Reserva::class.java) ?: return@forEach
                try {
                    val shouldDelete = if (it.tipo == TipoElemento.PUESTO.valor) {
                        val datePart = it.fechaHora.split(" ")[0]
                        val reservaEnd = format.parse("$datePart $cierreStr")
                        reservaEnd != null && reservaEnd.before(now)
                    } else {
                        val parts = it.fechaHora.split(" - ")
                        if (parts.size == 2) {
                            val datePart = parts[0].split(" ")[0]
                            val endTimeStr = parts[1].trim()
                            val reservaEnd = format.parse("$datePart $endTimeStr")
                            reservaEnd != null && reservaEnd.before(now)
                        } else {
                            val reservaStart = format.parse(it.fechaHora)
                            reservaStart != null && reservaStart.before(now)
                        }
                    }

                    if (shouldDelete) {
                        batch.delete(doc.reference)
                        hasOperations = true
                    }
                } catch (_: Exception) { }
            }
            
            if (hasOperations) batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting past reservations", e)
        }
    }

    suspend fun cascadeUpdateSala(empresaId: String, idSala: String, nuevoNombreSala: String, nuevoNombrePiso: String) {
        try {
            val snapshot = db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .whereEqualTo("idSala", idSala)
                .get()
                .await()
            
            if (snapshot.isEmpty) return

            val batch = db.batch()
            for (doc in snapshot) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "nombreSala" to nuevoNombreSala,
                        "piso" to nuevoNombrePiso
                    )
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error in cascadeUpdateSala", e)
        }
    }

    suspend fun getReservationsByFloor(empresaId: String, pisoNombre: String): List<Reserva> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .whereEqualTo("piso", pisoNombre)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
    }

    suspend fun markReservationsAsOrphanedByFloor(empresaId: String, pisoNombre: String) {
        try {
            val snapshot = db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .whereEqualTo("piso", pisoNombre)
                .get()
                .await()
            
            if (snapshot.isEmpty) return

            val batch = db.batch()
            for (doc in snapshot) {
                batch.update(doc.reference, "lugarEliminado", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error in markReservationsAsOrphanedByFloor", e)
        }
    }

    suspend fun markReservationsAsOrphanedBySala(empresaId: String, idSala: String, nombreSala: String? = null, pisoNombre: String? = null) {
        try {
            val batch = db.batch()
            var hasOperations = false

            // Caso 1: Por ID exacto (Lo más fiable)
            val snapshotById = db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .whereEqualTo("idSala", idSala)
                .get()
                .await()
            
            for (doc in snapshotById) {
                batch.update(doc.reference, "lugarEliminado", true)
                hasOperations = true
            }

            // Caso 2: Fallback por Nombre y Piso (para casos donde el ID falló o cambió)
            if (nombreSala != null && pisoNombre != null) {
                val snapshotByName = db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .whereEqualTo("nombreSala", nombreSala)
                    .whereEqualTo("piso", pisoNombre)
                    .get()
                    .await()
                for (doc in snapshotByName) {
                    batch.update(doc.reference, "lugarEliminado", true)
                    hasOperations = true
                }
            }
            
            if (hasOperations) batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error in markReservationsAsOrphanedBySala", e)
        }
    }
}
