package com.example.tfg_kotlin.data.repository

import com.example.tfg_kotlin.data.model.Reserva
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

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

    suspend fun getReservationsByDateTime(empresaId: String, fechaHora: String): List<Reserva> {
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .whereEqualTo("fechaHora", fechaHora)
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deletePastReservations(empresaId: String) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        // Obtener la hora de cierre de la empresa para los puestos flex
        val empresaDoc = db.collection("empresas").document(empresaId).get().await()
        val cierreStr = empresaDoc.getString("cierre") ?: "23:59"
        
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .get()
            .await()
        
        val now = Date()
        val reservations = snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
        
        reservations.filter {
            try {
                if (it.tipo == "PUESTO") {
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
            } catch (_: Exception) {
                false
            }
        }.forEach { reserva ->
            reserva.id?.let { id ->
                db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .document(id)
                    .delete()
                    .await()
            }
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
            for (doc in snapshot) {
                doc.reference.update(
                    mapOf(
                        "nombreSala" to nuevoNombreSala,
                        "piso" to nuevoNombrePiso
                    )
                ).await()
            }
        } catch (_: Exception) {}
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
            for (doc in snapshot) {
                doc.reference.update("lugarEliminado", true).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun markReservationsAsOrphanedBySala(empresaId: String, idSala: String, nombreSala: String? = null, pisoNombre: String? = null) {
        try {
            // Caso 1: Por ID exacto (Lo más fiable)
            val snapshotById = db.collection("empresas")
                .document(empresaId)
                .collection("reservas")
                .whereEqualTo("idSala", idSala)
                .get()
                .await()
            for (doc in snapshotById) {
                doc.reference.update("lugarEliminado", true).await()
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
                    doc.reference.update("lugarEliminado", true).await()
                }
            }
        } catch (_: Exception) {}
    }
}
