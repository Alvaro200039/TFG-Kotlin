package com.example.tfg_kotlin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.Menu
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tfg_kotlin.BBDD_Global.Entities.Empresa
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import java.util.concurrent.TimeUnit
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class activity_menu_empleado : AppCompatActivity() {

    // Creación variable global para acceder a firabase
    private lateinit var firestore: FirebaseFirestore

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

        // Insttancia para el acceso a la BD de firestore
        val auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Atentica el usuario actual
        val currentUser = auth.currentUser
        // En caso de no estar loquieado el usuario sale el siguiente mensaje
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Accedemos a datos desde Sesion.datos
        val sesion = Sesion.datos
        // Si no hay sesión iniciada da el siguiente mensaje
        if (sesion == null) {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Creación de variables para guardar los datos de la rama y la información deseada
        val cifUsuario = sesion.empresa.cif
        val correoUsuario = sesion.usuario.email
        val idUsuario = sesion.usuario.id
        var nombreUsuario = sesion.usuario.nombre

        // Si ne se encuentra algún dato dará el siguiente mensaje
        if (cifUsuario.isEmpty() || correoUsuario.isEmpty() || idUsuario == null) {
            Toast.makeText(this, "Faltan datos de usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Si el nombre del usuario no encuantra
        if (nombreUsuario.isEmpty()) {
            // coge el id (correo)de la base de datos de la coleción de usauarios que se encuentran dentro de una empresa
            firestore.collection("empresas")
                .document(cifUsuario)
                .collection("usuarios")
                .document(correoUsuario)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    // En caso de que exista el correo
                    if (documentSnapshot.exists()) {
                        // Guarde el usuario como un objeto de la dataClass correspondiente
                        val usuario = documentSnapshot.toObject(Usuario::class.java)
                        // Si el usuario existe
                        if (usuario != null) {
                            nombreUsuario = usuario.nombre
                            // Actualizar también en la sesión para mantener coherencia
                            Sesion.datos = sesion.copy(usuario = usuario)
                        } else {
                            // En caso de que no exista salta este mensaje
                            Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                // Crea excepción en caso de que no se pueda cargar el usuario actual
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error cargando usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Botón para realizar una nueva reserva
        findViewById<Button>(R.id.btnNuevaReserva).setOnClickListener {
            // Usa una corre corrutina (hilo) para que la apliación no se bloquee
            lifecycleScope.launch {
                try {
                    // Coge el nombre de la emresa (Id BD) y la guarda en una variable
                    val empresaId = sesion.empresa.nombre

                    // En caso de que no se encuentre el nombre de la empresa
                    if (empresaId.isEmpty()) {
                        // Salta el siguiente mensaje en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@activity_menu_empleado, "Empresa no identificada", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Se posiciona en la coleccion de empresas
                    val empresaDoc = firestore.collection("empresas")
                        .document(empresaId)
                        .get()
                        .await()

                    // En caso de que la empresa no exista sala el mensaje en el hilo principal
                    if (!empresaDoc.exists()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@activity_menu_empleado, "Empresa no encontrada", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Cargar empresa en sesión si hace falta
                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    empresa?.nombre = empresaDoc.id
                    empresa?.let { sesion.empresa = it }

                    // Guarda en una variable el acceso a la colección de pisos que se encuentra dentro de las empresas
                    val pisosSnapshot = firestore.collection("empresas")
                        .document(empresaId)
                        .collection("pisos")
                        .get()
                        .await()

                    // Guarda los pisos como objetos de la dataClass correspondiente
                    val pisos = pisosSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Piso::class.java)?.apply { id = doc.id }
                    }

                    // Si la colección de pisos no está vacio
                    if (pisos.isNotEmpty()) {
                        // Obtiene la lista de pisos
                        sesion?.pisos = listOf(pisos.last()) // o sesion?.pisos = pisos si quieres todos
                        // Se abrirá la activity en el hilo principal
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@activity_menu_empleado, Activity_empleados::class.java))
                        }
                    } else {
                        // En casod no encontrar pisos, saltará el siguiente mensaje en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@activity_menu_empleado, "No se ha creado ningún piso", Toast.LENGTH_SHORT).show()
                        }
                    }
                //Creación de excepción en caso de no poder obtener los prisos (Ejecuta en el hilo principal)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@activity_menu_empleado, "Error al cargar pisos: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        }

        // Definición de acción para botón ver reservas
        findViewById<Button>(R.id.btnVerReservas).setOnClickListener {
            mostrarDialogoReservas()
        }

        // Acción para el botón de logout (Instancia una desconexión a firebase, cierra la sisión actual e inicia la activity de login)
        findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Sesion.cerrarSesion() // Limpiar sesión al cerrar sesión
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // En caso de loguearte con un usario que tenga reservas anteriores a la fecha actual, se elimianan,
    // en caso de tener reservas pendientes muestra la siquiente más cercans
    override fun onResume() {
        super.onResume()
        limpiarReservasPasadas()
        mostrarSiguienteReserva()
    }

    // Infla la toolbar para realizar acciones
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    // Opciones a realizar con la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Volber a la pantalla anterior
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            // Mopstrar diñalogo para notificiones
            R.id.action_options -> {
                mostrarDialogoNotificaciones()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Solicitud de permisos (para notificaiones)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Comportamiento estandar
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verifica la restuesta para el código
        if (requestCode == 1001) {
            // Valida que el permiso sea concedido
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permiso denegado, muestra el siguiente mensaje
                Toast.makeText(this, "No se podrán mostrar notificaciones de reservas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para diálogo de las notificaiones
    private fun mostrarDialogoNotificaciones() {
        // Infla el layout del menú para que salga el diálogo
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notificaciones, null)

        // Activa un switch y un seleccionador numérico para guardar datos de configuración de notificaiones
        val switch = dialogView.findViewById<MaterialSwitch>(R.id.switchNotificaciones)
        val picker = dialogView.findViewById<NumberPicker>(R.id.pickerMinutos)

        // Guarda los datos con valoers por defecto en un fichero shared preferences
        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
        val notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true)
        val minutosAntes = prefs.getInt("minutos_antes", 10)

        // En caso de tener activadas las notificaiones, tengrá los siguientes valores
        switch.isChecked = notificacionesActivadas
        picker.minValue = 1
        picker.maxValue = 60
        picker.value = minutosAntes

        // Creación del builder para crear el diálogo le pone título, elige el inlador para el diálogo y añade un botón de guardar que realiza acciones
        val dialog = AlertDialog.Builder(this)
            .setTitle("Notificaciones")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Usa un editor de preferencias y los guarda, salta un mensaje de confirmación
                val editor = prefs.edit()
                editor.putBoolean("notificaciones_activadas", switch.isChecked)
                editor.putInt("minutos_antes", picker.value)
                editor.apply()
                Toast.makeText(this, "Preferencias guardadas", Toast.LENGTH_SHORT).show()
            }
            // En caso de presionar el botón de cancelar no se gardará
            .setNegativeButton("Cancelar", null)
            .create()

        // Fondo transparente para que solo se vea tu diseño personalizado
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Muestra el diálogo
        dialog.show()
        // Personaliza el color del diálogo
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.red))
    }

    // función para mostrar el diálogo de reservas
    private fun mostrarDialogoReservas() {
        // En caso de que existan reservas anteriores a la fecha actual, se eliminan
        limpiarReservasPasadas()
        // Accede a la sesión de firebase, al nombre de la empresa y a id(correo) de firebase
        val sesion = Sesion.datos
        val db = FirebaseFirestore.getInstance()
        val nombreEmpresa = sesion?.empresa?.nombre
        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // En caso de que no exita el correo del usuario o el nombre de la empresa, dará el siguiente mensaje
        if (uidUsuario.isBlank() || nombreEmpresa.isNullOrBlank()) {
            Toast.makeText(this, "Usuario o empresa no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Guarda el nombre de la empresa (ID en BD) en una variable
        val empresaId = nombreEmpresa

        // Usa una corrutina
        lifecycleScope.launch {
            try {
                // Accede a la coleción de reservas que se encuentra dentro de la colección del empresas
                val snapshot = db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .get()
                    .await()

                // Guarde las reservas como objeto de la dataClass correspondiente
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                // Crea filtro para sabre las reservas del usuario actual
                val reservasUsuario = reservas.filter { it.idusuario == uidUsuario }

                // En caso de que no existan reservas, saltará el siguiente mensaje en el hilo principal
                if (reservasUsuario.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@activity_menu_empleado, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Guarda las reservas por piso (agrupación por piso)
                val reservasPorPiso = reservasUsuario.groupBy { it.piso }

                // En el hilo principal
                withContext(Dispatchers.Main) {
                    // Infla un diálogo que muestren las reservas y los almacena en una contenedor
                    val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
                    val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)
                    lateinit var dialog: AlertDialog

                    // Recorre el mapa de las reservas agrupadas por piso
                    for ((piso, lista) in reservasPorPiso) {
                        // TextView para nombre del piso, guarda valores para el formato del mensaje
                        val pisoText = TextView(this@activity_menu_empleado).apply {
                            text = piso
                            textSize = 18f
                            setPadding(0, 16, 0, 8)
                            setTextColor(Color.BLACK)
                            setTypeface(null, Typeface.BOLD)
                        }
                        // Añade el título del piso en el contenedor
                        contenedor.addView(pisoText)

                        // Recorre la lista de pisos de ese piso
                        lista.forEach { reserva ->
                            // Crea un  textView para cada reserva y guarda los valores del formato con el que se verá
                            val reservaText = TextView(this@activity_menu_empleado).apply {
                                // Nombre de sala + fecha y hora
                                text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                                setPadding(16, 4, 0, 4)
                                setTextColor(Color.DKGRAY)
                                // Al hacer click en la reserva abre un mensaje de confirmación de conaclación con título, mensaje y dos botones
                                setOnClickListener {
                                    val confirmDialog = AlertDialog.Builder(this@activity_menu_empleado)
                                        .setTitle("¿Cancelar reserva?")
                                        .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                                        .setPositiveButton("Sí") { _, _ ->
                                            // Acción al clickar el boton sí, relizan en un segundo hilo, elimina la reserva
                                            lifecycleScope.launch {
                                                try {
                                                    reserva.id?.let { idDoc ->
                                                        db.collection("empresas")
                                                            .document(empresaId)
                                                            .collection("reservas")
                                                            .document(idDoc)
                                                            .delete()
                                                            .await() // Espera a que se realiza la acción
                                                    }
                                                    // Cierra el diálogo
                                                    dialog.dismiss()
                                                    // actualiza el listado
                                                    mostrarDialogoReservas()
                                                    // muestra la siguiente reserva disponible
                                                    mostrarSiguienteReserva()
                                                // Cración de una excepción que mostrará el mensaje de error en el hilo pricipal
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(this@activity_menu_empleado, "Error al eliminar la reserva", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                        // En caso de darla al botón no se realiza ninguna acción
                                        .setNegativeButton("No", null)
                                        .create()

                                    // Personalización del mensaje de los diálogos
                                    confirmDialog.setOnShowListener {
                                        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                                        confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                                    }
                                    confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                    confirmDialog.show() // Mouestra el diálogo
                                }
                            }
                            // Añade la reserva al piso
                            contenedor.addView(reservaText)
                        }

                        // Añade una línea divisoria entre los grupos de pisos
                        val divider = View(this@activity_menu_empleado).apply {
                            setBackgroundColor(Color.LTGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2
                            ).apply { setMargins(0, 16, 0, 16) }
                        }
                        contenedor.addView(divider) // Añade la línea al contenedor
                    }

                    // Crea la el constructor para pantalla de diálogo para ver reservas activas
                    dialog = AlertDialog.Builder(this@activity_menu_empleado)
                        // titulo, texto del diálogo, botón de cerrar que anula acciones
                        .setTitle("Tus reservas activas")
                        .setView(dialogView)
                        .setPositiveButton("Cerrar", null)
                        .create() // Creación del diálogo

                    // Muestra el diálogo y cambia el color del mensaje
                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                }
            // Creación de una ecepción en caso de error
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@activity_menu_empleado, "Error cargando reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Función para eliminar reservas pasadas
    private fun limpiarReservasPasadas() {
        // VAriabel que guarda el formato de fecha-hora
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Accede al nombre de la empresa en firebase
        val sesion = Sesion.datos
        val nombreEmpresa = sesion?.empresa?.nombre

        // En caso de no encontra el nombre de la empresa
        if (nombreEmpresa.isNullOrBlank()) return

        // Acciones que se ejecutan en un hilo secundario
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Acceso a la coleccion de reservas de firestore
                val snapshot = firestore.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .get()
                    .await()

                // Guarda las reservas como objetos de dataObject correspondientes, copia el id
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                // Crea filtro para ver reservas anteriores la fecha actual
                val reservasAntiguas = reservas.filter {
                    try {
                        val fechaReserva = formato.parse(it.fechaHora)
                        fechaReserva != null && fechaReserva.before(Date())
                        // Crea una excepción
                    } catch (e: Exception) {
                        false
                    }
                }

                // Recorre la lista de resevas antiguas y las elimina
                reservasAntiguas.forEach { reserva ->
                    reserva.id?.let { idDoc ->
                        firestore.collection("empresas")
                            .document(nombreEmpresa)
                            .collection("reservas")
                            .document(idDoc)
                            .delete()
                            .await()// Espera hasta que realiza la acción
                    }
                }
            // Crea una excepción
            } catch (e: Exception) {
                // Log.e("limpiarReservasPasadas", "Error al limpiar reservas", e)
            }
        }
    }

    // Cración de función para mostrar reservas siguientes
    private fun mostrarSiguienteReserva() {
        // Creación de variables para mostrar texto, guardar formato de fecha-hora y guardar la fecha actual
        val textView = findViewById<TextView>(R.id.textProximaReserva)
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        // Variables que guardan inicio de sesión, nombre de emresa y correo de usuario actualmente logueado
        val sesion = Sesion.datos
        val nombreEmpresa = sesion?.empresa?.nombre
        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        // En caso de no encontrar el nombre de la empresa y el correo del usuario saltará el siguiente mensaje
        if (uidUsuario.isBlank() || nombreEmpresa.isNullOrBlank()) {
            textView.text = "No hay usuario o empresa válidos"
            return
        }

        // En caso de que se encuentren los datos anteriores, se ejecutan accione en una hilo secundario
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtine las reservas que el usaurio logueado tenga disponibles
                val snapshot = firestore.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("idusuario", uidUsuario)
                    .get()
                    .await()

                // Guarda las resrvas como objeto del dataClass correspondinte
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                // Recorre la lista de de reservas y las filtra por aquellas que se encuentren después de la fecha actiual
                val reservasFuturas = reservas.filter {
                    try {
                        val fechaReserva = formato.parse(it.fechaHora)
                        fechaReserva != null && fechaReserva.after(ahora)
                    } catch (e: Exception) {
                        false
                    }
                }

                // Si no existen reservas futuras a la fecha actual, saltería este mensaje en el hilo principal
                if (reservasFuturas.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        textView.text = "No hay reservas próximas"
                    }
                    return@launch
                }

                // Guarda con formaro las fechas de las siguiente reserva más cercana
                val siguienteReserva = reservasFuturas.minByOrNull {
                    formato.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
                }

                // muestra qué reserva es la siguiente disponible
                siguienteReserva?.let { reserva ->
                    val texto = "Siguiente reserva: ${reserva.piso} \n${reserva.nombreSala} el ${reserva.fechaHora}"
                    // Se muestra en el hilo principal
                    withContext(Dispatchers.Main) {
                        textView.text = texto
                    }

                    // Compruaba el tiempo restante hasta la siguiente reserva
                    val fechaReserva = formato.parse(reserva.fechaHora)
                    fechaReserva?.let { fecha ->
                        val tiempoRestante = fecha.time - System.currentTimeMillis()

                        // Guarda en el shared preferences que nos notificaciones esten activas y avise antes de 10 min
                        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
                        val notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true)
                        val minutosAntes = prefs.getInt("minutos_antes", 10)

                        // En caso de que las notificaciones estén activas
                        if (notificacionesActivadas) {
                            // Comprueba cuanto falta hasta que sea la reserva
                            val tiempoAntes = TimeUnit.MINUTES.toMillis(minutosAntes.toLong())
                            val delay = tiempoRestante - tiempoAntes

                            // el tiempo es mayor a 0 segundos
                            if (delay > 0) {
                                // Crea una variable que almacena la fecha y hora de la reserva y el nombre de la sala
                                val inputData = Data.Builder()
                                    .putString("hora_reserva", reserva.fechaHora)
                                    .putString("nombre_sala", reserva.nombreSala)
                                    .build()

                                // Crea una variable para la solicitud de ejecución  con el tiempo restante y los datos almacenados anteriormente
                                val workRequest = OneTimeWorkRequestBuilder<ReservaWorker>()
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .build()

                                // Define el nombre con el que se ejecutará
                                val workName = "recordatorio_reserva_${reserva.nombreSala}_${reserva.fechaHora}"

                                // Evita dupicados, en caso de que exista el nombre, se reemplaza y se realiza
                                WorkManager.getInstance(this@activity_menu_empleado)
                                    .enqueueUniqueWork(
                                        workName,
                                        ExistingWorkPolicy.REPLACE,
                                        workRequest
                                    )
                            }
                        }
                    }
                }
            // Crea excepsión en caso de que no se pueda cargar la reserva
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textView.text = "Error al cargar reservas"
                }
            }
        }
    }
}
