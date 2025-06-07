package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    val db = FirebaseFirestore.getInstance()
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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Accedemos a datos desde Sesion.datos
        if (sesion == null) {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
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


        if (idUsuario === null || correoUsuario.isEmpty() || cifUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        container = findViewById(R.id.contentLayout)

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



        val nombreEmpresa = sesion.empresa.nombre
        db.collection("empresas")
            .document(nombreEmpresa)
            .collection("pisos")
            .get()
            .addOnSuccessListener { documents ->
                listaPisos = documents.mapNotNull { it.toObject(Piso::class.java).apply { id = it.id } }
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

                cargarSalas()
                lifecycleScope.launch {
                    cargarImagenFondo(nombrePiso)
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

    private fun cargarSalas() {
        container.removeAllViews()

        val empresaId = sesion?.empresa?.nombre ?: ""
        val pisoId = pisoSeleccionado.toString()

        // Referencia a la subcolección salas dentro del piso
        val salasRef = db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(pisoId)
            .collection("salas")

        salasRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val salas = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val datos = doc.data ?: return@mapNotNull null
                            Salas(
                                id = doc.id,
                                nombre = datos["nombre"] as? String ?: "",
                                tamaño = datos["tamaño"] as? String ?: "",
                                x = (datos["x"] as? Number)?.toFloat() ?: 0f,
                                y = (datos["y"] as? Number)?.toFloat() ?: 0f,
                                ancho = (datos["ancho"] as? Number)?.toFloat() ?: 100f,
                                alto = (datos["alto"] as? Number)?.toFloat() ?: 100f,
                                extras = datos["extras"] as? List<String> ?: emptyList()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    for (sala in salas) {
                        val salaButton = Button(this@Activity_empleados).apply {
                            text = formatearTextoSala(sala)
                            tag = sala

                            setOnClickListener {
                                if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                                    snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                                        .setAction("Seleccionar fecha") { mostrarDialogoFecha() }
                                    snackbarActivo?.show()
                                } else {
                                    mostrarDialogoDetallesSala(
                                        sala,
                                        pisoNombre = pisoId
                                    )
                                }
                            }

                            background = GradientDrawable().apply {
                                setColor(Color.GRAY)
                                cornerRadius = 50f
                            }
                            snackbarActivo?.dismiss()

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
                        verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada",
                            pisoSeleccionado.toString()
                        )
                    }
                } else {
                    Toast.makeText(this, "No hay salas definidas para este piso", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarImagenFondo(nombrePiso: String) {
        val contentLayout = findViewById<ConstraintLayout>(R.id.contentLayout)

        val empresaId = sesion?.empresa?.nombre ?: ""

        if (empresaId.isEmpty()) {
            contentLayout.setBackgroundResource(R.drawable.wallpaper)
            Toast.makeText(this, "no se carga la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(nombrePiso)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imagenUrl = document.getString("imagenUrl")

                    if (imagenUrl.isNullOrEmpty()) {
                        Toast.makeText(this, "No hay imagen de fondo para este piso", Toast.LENGTH_SHORT).show()
                    } else {
                        Glide.with(this@Activity_empleados)
                            .load(imagenUrl)
                            .centerCrop()
                            .into(object : CustomTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    contentLayout.background = resource
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // No se requiere acción
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    contentLayout.setBackgroundResource(R.drawable.wallpaper)
                                }
                            })
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

        val nombreEmpresa = sesion?.empresa?.nombre
        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (uidUsuario.isBlank() || nombreEmpresa.isNullOrBlank()) {
            Toast.makeText(this, "Usuario o empresa no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val empresaId = nombreEmpresa

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .get()
                    .await()

                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

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
                                                        db.collection("empresas")
                                                            .document(empresaId)
                                                            .collection("reservas")
                                                            .document(idDoc)
                                                            .delete()
                                                            .await()
                                                    }
                                                    dialog.dismiss()
                                                    mostrarDialogoReservas()
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
                                        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                                        confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
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

        val nombreEmpresa = sesion?.empresa?.nombre
        if (nombreEmpresa.isNullOrBlank()) return

        val empresaId = nombreEmpresa

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .get()
                    .await()

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
                        db.collection("empresas")
                            .document(empresaId)
                            .collection("reservas")
                            .document(idDoc)
                            .delete()
                            .await()
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

        val empresaId = sesion?.empresa?.nombre ?: ""
        if (empresaId.isEmpty()) {
            Toast.makeText(this, "Empresa no identificada.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val franjasOriginales = withContext(Dispatchers.IO) {
                    val snapshot = db.collection("empresas")
                        .document(empresaId)
                        .collection("franjasHorarias")
                        .get()
                        .await()

                    snapshot.documents.map { it.id } // ✅ usamos el ID como hora
                }

                if (franjasOriginales.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val hoy = sdfFecha.format(Date())

                val calendario = Calendar.getInstance()
                val horaActual = String.format("%02d:%02d", calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE))

                val horasDisponibles = if (fechaSeleccionada == hoy) {
                    franjasOriginales.filter { it.split("-").first() >= horaActual }
                } else {
                    franjasOriginales
                }

                if (horasDisponibles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val horasArray = horasDisponibles.toTypedArray()

                    val dialog = AlertDialog.Builder(this@Activity_empleados)
                        .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
                        .setItems(horasArray) { _, which ->
                            horaSeleccionada = horasArray[which]
                            textViewHora.text = "Hora: $horaSeleccionada"
                            verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada",
                                pisoSeleccionado.toString()
                            )
                        }
                        .setNegativeButton("Cerrar", null)
                        .create()

                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al cargar franjas horarias: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun formatearTextoSala(sala: Salas): String {
        val builder = StringBuilder().append(sala.nombre)
        if (sala.extras.isNotEmpty()) {
            builder.append("\n")
            sala.extras.forEach { extra ->
                val icono = when (extra) {
                    "WiFi" -> "📶 "
                    "Proyector" -> "📽️ "
                    "Pizarra" -> "🖍️ "
                    else -> ""
                }
                builder.append(icono)
            }
        }
        return builder.toString()
    }

    private fun verificarDisponibilidad(fechaHora: String, nombrePiso: String) {
        val nombreEmpresa = sesion?.empresa?.nombre ?: return

        lifecycleScope.launch {
            try {
                val reservasSnapshot = db.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get()
                    .await()

                val reservas = reservasSnapshot.documents.mapNotNull { doc ->
                    val idSala = doc.getString("idSala") ?: return@mapNotNull null
                    val piso = doc.getString("piso") ?: return@mapNotNull null
                    Pair(idSala.trim(), piso.trim())
                }

                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view.tag is Salas) {
                        val sala = view.tag as Salas
                        val estaReservada = reservas.any { (idSalaRes, pisoRes) ->
                            idSalaRes.equals(sala.id?.trim(), ignoreCase = true) &&
                                    pisoRes.equals(nombrePiso.trim(), ignoreCase = true)
                        }

                        view.backgroundTintList = if (estaReservada) {
                            ColorStateList.valueOf(ContextCompat.getColor(this@Activity_empleados, R.color.red))
                        } else {
                            ColorStateList.valueOf(ContextCompat.getColor(this@Activity_empleados, R.color.green))
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@Activity_empleados, "Error al verificar disponibilidad: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoDetallesSala(sala: Salas, pisoNombre: String) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val nombreEmpresa = sesion?.empresa?.nombre ?: ""
        val nombreUsuario = sesion?.usuario?.nombre ?: ""
        val currentUser = FirebaseAuth.getInstance().currentUser
        val idUsuario = currentUser?.uid ?: ""

        if (nombreUsuario == "") {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val pisoSnapshot = db.collection("empresas").document(nombreEmpresa)
                    .collection("pisos").document(pisoNombre).get().await()

                val pisoNombreReal = pisoSnapshot.getString("nombre") ?: pisoNombre

                val reservasSnapshot = db.collection("empresas").document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get().await()

                val reservas = reservasSnapshot.documents.map { doc ->
                    Reserva(
                        id = doc.id,
                        nombreSala = doc.getString("nombreSala") ?: "",
                        idSala = doc.getString("idSala") ?: "",
                        fechaHora = doc.getString("fechaHora") ?: "",
                        nombreUsuario = doc.getString("nombreUsuario") ?: "",
                        idusuario = doc.get("idusuario")?.toString() ?: "",
                        piso = doc.getString("piso") ?: ""
                    )
                }

                val reservaUsuarioMismaHora = reservas.find { it.idusuario == idUsuario }
                val reservaExistente = reservas.find {
                    it.idSala.trim().equals(sala.id?.trim(), ignoreCase = true) &&
                            it.piso.trim().equals(pisoNombreReal.trim(), ignoreCase = true)
                }

                val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)
                dialogView.findViewById<TextView>(R.id.tvValorPiso).text = pisoNombreReal
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
                    // Puedes cancelar tu propia reserva
                    reservaExistente != null && reservaExistente.idusuario == idUsuario -> {
                        builder.setPositiveButton("Cancelar reserva") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    db.collection("empresas").document(nombreEmpresa)
                                        .collection("reservas").document(reservaExistente.id.toString())
                                        .delete().await()
                                    verificarDisponibilidad(fechaHora, pisoNombreReal)
                                    Toast.makeText(this@Activity_empleados, "Reserva cancelada", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@Activity_empleados, "Error al cancelar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    // Ya tienes una reserva a esa hora, en otra sala
                    reservaUsuarioMismaHora != null -> {
                        builder.setPositiveButton("Ya tienes una reserva", null)
                    }

                    // No hay ninguna reserva para esta sala → puedes reservar
                    reservaExistente == null -> {
                        builder.setPositiveButton("Reservar") { _, _ ->
                            reservarSala(sala, pisoNombreReal)
                        }
                    }

                    // Sala reservada por otro usuario → no se puede reservar ni cancelar
                    reservaExistente?.idusuario != idUsuario -> {
                        builder.setPositiveButton("Reservada", null)
                    }
                }


                val dialog = builder.create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                dialog.setOnShowListener {
                    val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positive?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                    negative?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))

                    if (reservaExistente != null && reservaExistente.idusuario != idUsuario) {
                        positive?.isEnabled = false
                        positive?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.grey))
                        Toast.makeText(this@Activity_empleados, "Sala reservada por ${reservaExistente.nombreUsuario}", Toast.LENGTH_SHORT).show()
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

    private fun reservarSala(sala: Salas, pisoNombreReal: String) {
        val sesion = Sesion.datos
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val nombreUsuario = sesion?.usuario?.nombre ?: ""
        val nombreEmpresa = sesion?.empresa?.nombre ?: ""
        val currentUser = FirebaseAuth.getInstance().currentUser
        val idUsuario = currentUser?.uid ?: ""

        if (nombreUsuario.isEmpty() || nombreEmpresa.isEmpty()) {
            Toast.makeText(this, "Información de usuario o empresa incompleta", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {

                val reservasSnapshot = db.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .whereEqualTo("idusuario", idUsuario)
                    .get().await()

                if (!reservasSnapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        val reserva = reservasSnapshot.documents[0]
                        Toast.makeText(
                            this@Activity_empleados,
                            "Ya tienes una reserva a esta hora en: ${reserva.getString("nombreSala")} ${reserva.getString("piso")}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Si no hay conflicto, hacer reserva
                val nuevaReserva = hashMapOf(
                    "nombreSala" to sala.nombre,
                    "idSala" to (sala.id ?: ""),
                    "fechaHora" to fechaHora,
                    "nombreUsuario" to nombreUsuario,
                    "idusuario" to idUsuario,
                    "piso" to pisoNombreReal
                )

                db.collection("empresas").document(nombreEmpresa)
                    .collection("reservas")
                    .add(nuevaReserva)
                    .await()

                withContext(Dispatchers.Main) {
                    verificarDisponibilidad(fechaHora, pisoNombreReal)
                    Toast.makeText(this@Activity_empleados, "Reserva confirmada", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al reservar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}