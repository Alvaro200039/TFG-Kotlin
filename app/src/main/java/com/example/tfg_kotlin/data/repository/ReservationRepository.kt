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
        val snapshot = db.collection("empresas")
            .document(empresaId)
            .collection("reservas")
            .get()
            .await()
        
        val now = Date()
        val reservations = snapshot.documents.mapNotNull { it.toObject(Reserva::class.java)?.copy(id = it.id) }
        
        reservations.filter {
            try {
                val reservaDate = format.parse(it.fechaHora)
                reservaDate != null && reservaDate.before(now)
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
}
