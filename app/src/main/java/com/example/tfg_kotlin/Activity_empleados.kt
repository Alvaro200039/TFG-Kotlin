package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.Entities.Salas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import android.util.Base64
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion


class Activity_empleados : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView
    private lateinit var spinnerPisos: Spinner
    private var pisoSeleccionado: String? = null
    private lateinit var textViewHora: TextView
    private var snackbarActivo: Snackbar? = null
    private lateinit var listaPisos: List<Piso>
    private var idUsuario: String = ""
    private var nombreUsuario: String = ""
    val db = FirebaseFirestore.getInstance()
    private lateinit var firestore: FirebaseFirestore
    val sesion = Sesion.datos



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_empleados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.empleados)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Accedemos a datos desde Sesion.datos
        if (sesion == null) {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val cifUsuario = sesion.empresa.cif
        val correoUsuario = sesion.usuario.email
        val idUsuario = sesion.usuario.id
        var nombreUsuario = sesion.usuario.nombre


        if (idUsuario === null || correoUsuario.isEmpty() || cifUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        container = findViewById(R.id.contentLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        spinnerPisos = Spinner(this).apply {
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                marginStart = 0
            }
        }
        toolbar.addView(spinnerPisos)



        db.collection("pisos")
            .get()
            .addOnSuccessListener { documents ->
                listaPisos = documents.mapNotNull { it.toObject(Piso::class.java) }
                val nombresPisos = listaPisos.map { it.nombre }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresPisos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPisos.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar pisos", Toast.LENGTH_SHORT).show()
            }

        spinnerPisos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val piso = listaPisos[position]
                pisoSeleccionado = piso.id
                val nombrePiso = piso.nombre
                val empresaId: String = piso.empresaCif

                cargarSalas(nombrePiso, empresaId)
                lifecycleScope.launch {
                    cargarImagenFondo(nombrePiso, empresaId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                pisoSeleccionado = null
                container.removeAllViews()
            }
        }

        val btnReservas = findViewById<LinearLayout>(R.id.btn_reservas)
        btnReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        val btnFranja = findViewById<LinearLayout>(R.id.btn_franja)
        btnFranja.setOnClickListener {
            if (fechaSeleccionada.isEmpty()) {
                snackbarActivo?.dismiss()
                snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                    .setAction("Seleccionar fecha") { mostrarDialogoFecha() }
                snackbarActivo?.show()
            } else {
                mostrarDialogoHoras()
            }
        }

        val botonSeleccionarFechaHora = ImageButton(this).apply {
            setImageResource(R.drawable.time)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarDialogoFecha() }
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginEnd = 20
                marginStart = 20
            }
        }
        toolbar.addView(botonSeleccionarFechaHora)

        val contenedorFechaHora = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = 8
            }
        }

        textViewFecha = TextView(this).apply {
            text = "Fecha:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        textViewHora = TextView(this).apply {
            text = "Hora:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        contenedorFechaHora.addView(textViewFecha)
        contenedorFechaHora.addView(textViewHora)
        toolbar.addView(contenedorFechaHora)
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cargarSalas(nombrePiso: String, empresaId: String) {
        container.removeAllViews()

        val db = FirebaseFirestore.getInstance()

        val salasRef = db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(nombrePiso)
            .collection("salas")

        salasRef.get()
            .addOnSuccessListener { result ->
                val salas = result.mapNotNull { it.toObject(Salas::class.java) }

                for (sala in salas) {
                    val salaButton = Button(this@Activity_empleados).apply {
                        text = formatearTextoSala(sala)
                        tag = sala

                        setOnClickListener {
                            if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                                snackbarActivo?.dismiss()
                                snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                                    .setAction("Seleccionar fecha") {
                                        mostrarDialogoFecha()
                                    }
                                snackbarActivo?.show()
                            } else {
                                mostrarDialogoDetallesSala(sala)
                            }
                        }

                        background = GradientDrawable().apply {
                            setColor(Color.GREEN)
                            cornerRadius = 50f
                        }

                        layoutParams = ConstraintLayout.LayoutParams(
                            sala.ancho.toInt(),
                            sala.alto.toInt()
                        ).apply {
                            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            setMargins(sala.x.toInt(), sala.y.toInt(), 0, 0)
                        }
                    }
                    container.addView(salaButton)
                }

                if (fechaSeleccionada.isNotEmpty() && horaSeleccionada.isNotEmpty()) {
                    verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun cargarImagenFondo(nombrePiso: String, empresaId: String) {
        val contentLayout = findViewById<ConstraintLayout>(R.id.contentLayout)

        if (empresaId.isEmpty()) {
            contentLayout.setBackgroundResource(R.drawable.wallpaper)
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(nombrePiso)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imagenBase64 = document.getString("imagenUrl") // o "imagenBase64" si lo nombraste así

                    if (imagenBase64.isNullOrEmpty()) {
                        contentLayout.setBackgroundResource(R.drawable.wallpaper)
                        return@addOnSuccessListener
                    }

                    try {
                        val imagenBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.size)

                        Glide.with(this@Activity_empleados)
                            .asBitmap()
                            .load(bitmap)
                            .centerCrop()
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    contentLayout.background = BitmapDrawable(resources, resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // Nada que hacer
                                }
                            })

                    } catch (e: Exception) {
                        e.printStackTrace()
                        contentLayout.setBackgroundResource(R.drawable.wallpaper)
                    }
                } else {
                    contentLayout.setBackgroundResource(R.drawable.wallpaper)
                }
            }
            .addOnFailureListener {
                contentLayout.setBackgroundResource(R.drawable.wallpaper)
            }
    }

    private fun mostrarDialogoFecha() {
        snackbarActivo?.dismiss()
        val calendario = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            android.R.style.Theme_Material_Dialog_MinWidth,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                textViewFecha.text = "Fecha: $fechaSeleccionada"
                mostrarDialogoHoras()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = calendario.timeInMillis

        datePickerDialog.setOnShowListener {
            datePickerDialog.window?.setBackgroundDrawableResource(R.drawable.datepicker_background)
        }

        datePickerDialog.show()
    }


    private fun mostrarDialogoReservas() {
        limpiarReservasPasadas()

        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (uidUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("reservas").get().await()
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                // Filtrar por UID Firebase (String)
                val reservasUsuario = reservas.filter { it.idusuario == uidUsuario }

                if (reservasUsuario.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Activity_empleados,
                            "No tienes reservas activas.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val reservasPorPiso = reservasUsuario.groupBy { it.piso }

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
                    val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

                    lateinit var dialog: AlertDialog

                    for ((piso, lista) in reservasPorPiso) {
                        val pisoText = TextView(this@Activity_empleados).apply {
                            text = piso
                            textSize = 18f
                            setPadding(0, 16, 0, 8)
                            setTextColor(Color.BLACK)
                            setTypeface(null, Typeface.BOLD)
                        }
                        contenedor.addView(pisoText)

                        lista.forEach { reserva ->
                            val reservaText = TextView(this@Activity_empleados).apply {
                                text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                                setPadding(16, 4, 0, 4)
                                setTextColor(Color.DKGRAY)
                                setOnClickListener {
                                    val confirmDialog = AlertDialog.Builder(this@Activity_empleados)
                                        .setTitle("¿Cancelar reserva?")
                                        .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                                        .setPositiveButton("Sí") { _, _ ->
                                            lifecycleScope.launch {
                                                try {
                                                    reserva.id?.let { idDoc ->
                                                        firestore.collection("reservas")
                                                            .document(idDoc)
                                                            .delete()
                                                            .await()
                                                    }
                                                    dialog.dismiss()
                                                    mostrarDialogoReservas() // Recargar
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@Activity_empleados,
                                                            "Error al eliminar la reserva",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                        .setNegativeButton("No", null)
                                        .create()

                                    confirmDialog.setOnShowListener {
                                        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            ?.setTextColor(Color.BLACK)
                                        confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                            ?.setTextColor(Color.RED)
                                    }
                                    confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                    confirmDialog.show()
                                }
                            }
                            contenedor.addView(reservaText)
                        }

                        val divider = View(this@Activity_empleados).apply {
                            setBackgroundColor(Color.LTGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2
                            ).apply { setMargins(0, 16, 0, 16) }
                        }
                        contenedor.addView(divider)
                    }

                    dialog = AlertDialog.Builder(this@Activity_empleados)
                        .setTitle("Tus reservas activas")
                        .setView(dialogView)
                        .setPositiveButton("Cerrar", null)
                        .create()

                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error cargando reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun limpiarReservasPasadas() {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("reservas").get().await()
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                val reservasAntiguas = reservas.filter {
                    try {
                        val fechaReserva = formato.parse(it.fechaHora)
                        fechaReserva != null && fechaReserva.before(Date())
                    } catch (e: Exception) {
                        false
                    }
                }

                reservasAntiguas.forEach { reserva ->
                    reserva.id?.let { idDoc ->
                        firestore.collection("reservas").document(idDoc).delete().await()
                    }
                }
            } catch (e: Exception) {
                // Opcional: Log.e("limpiarReservasPasadas", "Error al limpiar reservas", e)
            }
        }
    }

    private fun mostrarDialogoHoras() {
        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Primero selecciona una fecha.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val empresaId = intent.getStringExtra("empresaId") ?: "" // O el identificador que estés usando

        lifecycleScope.launch {
            try {
                val franjasOriginales = withContext(Dispatchers.IO) {
                    val snapshot = db.collection("franjasHorarias")
                        .whereEqualTo("empresaId", empresaId)
                        .get()
                        .await()

                    snapshot.documents.mapNotNull { it.getString("hora") }
                }

                if (franjasOriginales.isEmpty()) {
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val calendario = Calendar.getInstance()
                val hoy = String.format(
                    "%02d/%02d/%d",
                    calendario.get(Calendar.DAY_OF_MONTH),
                    calendario.get(Calendar.MONTH) + 1,
                    calendario.get(Calendar.YEAR)
                )

                val horaActual = String.format(
                    "%02d:%02d",
                    calendario.get(Calendar.HOUR_OF_DAY),
                    calendario.get(Calendar.MINUTE)
                )

                val horasDisponibles = if (fechaSeleccionada == hoy) {
                    franjasOriginales.filter { it >= horaActual }
                } else {
                    franjasOriginales
                }

                if (horasDisponibles.isEmpty()) {
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val horasArray = horasDisponibles.toTypedArray()

                val dialog = AlertDialog.Builder(this@Activity_empleados)
                    .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
                    .setItems(horasArray) { _, which ->
                        horaSeleccionada = horasArray[which]
                        textViewHora.text = "Hora: $horaSeleccionada"
                        verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
                    }
                    .setNegativeButton("Cerrar", null)
                    .create()

                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)

            } catch (e: Exception) {
                Toast.makeText(this@Activity_empleados, "Error al cargar franjas horarias.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun formatearTextoSala(sala: Salas): String {
        val builder = StringBuilder().append(sala.nombre)
        if (sala.extras.isNotEmpty()) {
            builder.append("\n")
            sala.extras.forEach {
                builder.append(
                    when (it) {
                        "WiFi" -> "📶 "
                        "Proyector" -> "📽️ "
                        "Pizarra" -> "🖍️ "
                        else -> ""
                    }
                )
            }
        }
        return builder.toString()
    }

    private fun verificarDisponibilidad(fechaHora: String) {
        lifecycleScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Obtener las reservas en esa fechaHora
                val reservasSnapshot = withContext(Dispatchers.IO) {
                    db.collection("reservas")
                        .whereEqualTo("fechaHora", fechaHora)
                        .get()
                        .await()
                }
                val reservas = reservasSnapshot.documents.mapNotNull { doc ->
                    val nombreSala = doc.getString("nombreSala")
                    val piso = doc.getString("piso")
                    if (nombreSala != null && piso != null) {
                        Pair(nombreSala.trim(), piso.trim())
                    } else null
                }
                 val empresaId = intent.getStringExtra("empresaId") ?: ""
                // 2. Mapear los IDs de piso a sus nombres
                // Puedes tener un mapa local si ya lo tienes cargado, o hacerlo desde Firestore
                val pisosSnapshot = withContext(Dispatchers.IO) {
                    db.collection("pisos")
                        .whereEqualTo("empresaId", empresaId)
                        .get()
                        .await()
                }

                val mapaPisos: Map<String, String> = pisosSnapshot.documents.associate { doc ->
                    val id = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    id to nombre
                }

                // 3. Recorrer las vistas y pintar según disponibilidad
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is Button) {
                        val sala = view.tag as? Salas ?: continue
                        val nombrePisoSala = mapaPisos[sala.id] ?: ""

                        val ocupada = reservas.any { (nombreSalaRes, pisoRes) ->
                            nombreSalaRes.equals(sala.nombre, ignoreCase = true) &&
                                    pisoRes.equals(nombrePisoSala, ignoreCase = true)
                        }

                        val color = if (ocupada) Color.RED else Color.GREEN
                        view.background = GradientDrawable().apply {
                            setColor(color)
                            cornerRadius = 50f
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@Activity_empleados, "Error al verificar disponibilidad", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoDetallesSala(sala: Salas) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"

        if (idUsuario == "-1") {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Obtener el documento del piso desde Firestore
                val pisoSnapshot = firestore.collection("pisos")
                    .document(sala.id.toString())
                    .get()
                    .await()
                val pisoNombre = pisoSnapshot.getString("nombre") ?: "Desconocido"

                // 2. Obtener reservas para la fechaHora
                val reservasSnapshot = firestore.collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get()
                    .await()

                val reservas = reservasSnapshot.documents.map { doc ->
                    Reserva(
                        id = doc.id,
                        nombreSala = doc.getString("nombreSala") ?: "",
                        idSala = doc.getString("idSala") ?: "",
                        fechaHora = doc.getString("fechaHora") ?: "",
                        nombreUsuario = doc.getString("nombreUsuario") ?: "",
                        idusuario = (doc.getLong("idusuario")?.toInt() ?: -1).toString(),
                        piso = doc.getString("piso") ?: ""
                    )
                }.toMutableList()

                // 3. Buscar reserva existente para esta sala y piso
                val reservaExistente = reservas.find {
                    it.nombreSala.trim().equals(sala.nombre.trim(), ignoreCase = true) &&
                            it.piso.trim().equals(pisoNombre.trim(), ignoreCase = true) &&
                            it.fechaHora == fechaHora
                }

                var nombreReservaPor: String? = null
                if (reservaExistente != null && reservaExistente.idusuario != idUsuario.toString()) {
                    nombreReservaPor = reservaExistente.nombreUsuario
                }


                // 4. Construir diálogo con detalles
                val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)
                dialogView.findViewById<TextView>(R.id.tvValorPiso).text = pisoNombre
                dialogView.findViewById<TextView>(R.id.tvValorTamano).text = sala.tamaño
                dialogView.findViewById<TextView>(R.id.tvValorExtras).text =
                    if (sala.extras.isNotEmpty()) sala.extras.joinToString(", ") else "Ninguno"
                dialogView.findViewById<TextView>(R.id.tvValorFecha).text = fechaSeleccionada
                dialogView.findViewById<TextView>(R.id.tvValorHora).text = horaSeleccionada

                val builder = AlertDialog.Builder(this@Activity_empleados)
                    .setTitle("Detalles de ${sala.nombre}")
                    .setView(dialogView)
                    .setNegativeButton("Cerrar") { dialog, _ -> dialog.dismiss() }

                when {
                    reservaExistente != null && reservaExistente.idusuario == idUsuario.toString() -> {
                        builder.setPositiveButton("Cancelar reserva") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    // Eliminar reserva en Firestore
                                    firestore.collection("reservas")
                                        .document(reservaExistente.id.toString())
                                        .delete()
                                        .await()

                                    verificarDisponibilidad(fechaHora)
                                    Toast.makeText(this@Activity_empleados, "Reserva cancelada para ${sala.nombre}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@Activity_empleados, "Error al cancelar reserva: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    reservaExistente == null -> {
                        builder.setPositiveButton("Reservar") { _, _ ->
                            reservarSala(sala.nombre)
                        }
                    }
                    else -> {
                        builder.setPositiveButton("Reservada", null)
                    }
                }

                val dialog = builder.create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                dialog.setOnShowListener {
                    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                    positiveButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                    negativeButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))

                    if (nombreReservaPor != null) {
                        positiveButton?.isEnabled = false
                        positiveButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.grey))
                        Toast.makeText(this@Activity_empleados, "Sala reservada por $nombreReservaPor a esta hora", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.show()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al cargar detalles: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun reservarSala(nombreSala: String) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"

        if (idUsuario == "-1" || idUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Obtener el piso seleccionado de Firestore
                val pisoSnapshot = firestore.collection("pisos")
                    .whereEqualTo("id", pisoSeleccionado)
                    .get()
                    .await()

                val pisoDoc = pisoSnapshot.documents.firstOrNull()
                val nombrePiso = pisoDoc?.getString("nombre") ?: ""
                val idPiso = pisoDoc?.id ?: ""

                if (idPiso.isEmpty()) {
                    Toast.makeText(this@Activity_empleados, "No se encontró el piso seleccionado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Buscar sala por nombre y pisoId (id del documento Piso)
                val salaSnapshot = firestore.collection("salas")
                    .whereEqualTo("nombre", nombreSala)
                    .whereEqualTo("pisoId", idPiso)
                    .get()
                    .await()

                val salaDoc = salaSnapshot.documents.firstOrNull()
                if (salaDoc == null) {
                    Toast.makeText(this@Activity_empleados, "No se encontró la sala para reservar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val idSala = salaDoc.id

                // Obtener reservas para la fechaHora
                val reservasSnapshot = firestore.collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get()
                    .await()

                val reservas = reservasSnapshot.documents.map { doc ->
                    mapOf(
                        "nombreSala" to (doc.getString("nombreSala") ?: ""),
                        "piso" to (doc.getString("piso") ?: ""),
                        "idusuario" to (doc.getString("idusuario") ?: ""),
                        "nombreUsuario" to (doc.getString("nombreUsuario") ?: ""),
                        "id" to doc.id
                    )
                }

                // Buscar reserva existente para esta sala y piso
                val reservaExistente = reservas.find {
                    it["nombreSala"]!!.equals(nombreSala.trim(), ignoreCase = true) &&
                            it["piso"]!!.equals(nombrePiso.trim(), ignoreCase = true)
                }

                // Buscar reserva del usuario a esa hora (cualquier sala)
                val reservaUsuarioMismaHora = reservas.find {
                    it["idusuario"] == idUsuario
                }

                if (reservaExistente != null) {
                    val nombreReserva = reservaExistente["nombreUsuario"] ?: ""

                    if (reservaExistente["idusuario"] == idUsuario) {
                        // Dialogo para cancelar reserva propia
                        val dialog = AlertDialog.Builder(this@Activity_empleados)
                            .setTitle("Cancelar reserva")
                            .setMessage("'$nombreUsuario', ¿Deseas cancelar tu reserva para '$nombreSala' en '$fechaHora'?")
                            .setPositiveButton("Sí") { _, _ ->
                                lifecycleScope.launch {
                                    try {
                                        firestore.collection("reservas")
                                            .document(reservaExistente["id"] as String)
                                            .delete()
                                            .await()
                                        verificarDisponibilidad(fechaHora)
                                        Toast.makeText(this@Activity_empleados, "Reserva cancelada para $nombreSala", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@Activity_empleados, "Error al cancelar reserva: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("No", null)
                            .create()

                        dialog.setOnShowListener {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))
                        }
                        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                        dialog.show()
                    } else {
                        Toast.makeText(
                            this@Activity_empleados,
                            "Sala ya reservada por $nombreReserva en este piso",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                if (reservaUsuarioMismaHora != null) {
                    Toast.makeText(
                        this@Activity_empleados,
                        "Ya tienes una reserva a esa hora en '${reservaUsuarioMismaHora["nombreSala"]}' (${reservaUsuarioMismaHora["piso"]})",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Confirmar reserva
                val dialog = AlertDialog.Builder(this@Activity_empleados)
                    .setTitle("Confirmar reserva")
                    .setMessage("¿Deseas reservar '$nombreSala' en el '$nombrePiso' para '$fechaHora'?")
                    .setPositiveButton("Sí") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                val reservaMap = hashMapOf(
                                    "nombreSala" to nombreSala,
                                    "fechaHora" to fechaHora,
                                    "nombreUsuario" to nombreUsuario,
                                    "idusuario" to idUsuario,
                                    "piso" to nombrePiso,
                                    "idSala" to idSala
                                )
                                firestore.collection("reservas")
                                    .add(reservaMap)
                                    .await()

                                verificarDisponibilidad(fechaHora)
                                Toast.makeText(this@Activity_empleados, "Reserva realizada para $nombreSala en el $nombrePiso", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@Activity_empleados, "Error al realizar reserva: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .create()

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                }
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()

            } catch (e: Exception) {
                Toast.makeText(this@Activity_empleados, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}