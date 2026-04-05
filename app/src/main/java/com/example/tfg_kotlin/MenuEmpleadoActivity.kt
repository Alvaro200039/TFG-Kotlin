package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.BBDD_Global.Entities.Empresa
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MenuEmpleadoActivity : BaseMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_empleado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menuempleado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar con icono y funcionalidad
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Instanciación de Firebase
        val auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Autentica el usuario actual
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Accedemos a datos desde Sesion.datos
        val sesion = Sesion.datos
        if (sesion == null) {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Variables de sesión
        val correoUsuario = sesion.usuario.email
        val uidUsuario = sesion.usuario.uid
        var nombreUsuario = sesion.usuario.nombre
        val nombreEmpresa = sesion.empresa.nombre

        // Validación de datos requeridos
        if (correoUsuario.isEmpty() || nombreEmpresa.isEmpty()) {
            Toast.makeText(this, "Faltan datos de usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Si no se encuentra el nombre del usuario, lo carga desde Firestore
        if (nombreUsuario.isEmpty()) {
            firestore.collection("empresas")
                .document(nombreEmpresa)
                .collection("usuarios")
                .document(correoUsuario)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val usuario = documentSnapshot.toObject(Usuario::class.java)
                        if (usuario != null) {
                            nombreUsuario = usuario.nombre
                            Sesion.datos = sesion.copy(usuario = usuario)
                        } else {
                            Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error cargando usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Botón para realizar una nueva reserva
        findViewById<Button>(R.id.btnNuevaReserva).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val empresaId = sesion.empresa.nombre
                    if (empresaId.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MenuEmpleadoActivity, "Empresa no identificada", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val empresaDoc = firestore.collection("empresas")
                        .document(empresaId)
                        .get()
                        .await()

                    if (!empresaDoc.exists()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MenuEmpleadoActivity, "Empresa no encontrada", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    empresa?.nombre = empresaDoc.id
                    empresa?.let { sesion.empresa = it }

                    val pisosSnapshot = firestore.collection("empresas")
                        .document(empresaId)
                        .collection("pisos")
                        .get()
                        .await()

                    val pisos = pisosSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Piso::class.java)?.apply { id = doc.id }
                    }

                    if (pisos.isNotEmpty()) {
                        sesion.pisos = listOf(pisos.last())
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@MenuEmpleadoActivity, EmpleadosActivity::class.java))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MenuEmpleadoActivity, "No se ha creado ningún piso", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MenuEmpleadoActivity, "Error al cargar pisos: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        }

        // Botón ver reservas
        findViewById<Button>(R.id.btnVerReservas).setOnClickListener {
            mostrarDialogoReservas()
        }

        // Botón logout
        findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Sesion.cerrarSesion()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
