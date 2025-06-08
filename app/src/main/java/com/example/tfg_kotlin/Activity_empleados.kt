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

    // Creación de variables globales
    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView
    private lateinit var spinnerPisos: Spinner
    private var pisoSeleccionado: String? = null
    private lateinit var textViewHora: TextView
    private var snackbarActivo: Snackbar? = null
    private lateinit var listaPisos: List<Piso>
    // Variables para el acceso al firabase e inicio de sesiones
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

        // Toolbar con funcionalidad e iconos personalizados
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

        // Extracción de datos necesarios de BD Firabase -> coleccion -> dato
        val cifUsuario = sesion.empresa.cif
        val correoUsuario = sesion.usuario.email
        val idUsuario = sesion.usuario.id


        // En caso de no existir nungún dato saldrá el siguiente mensaje
        if (idUsuario === null || correoUsuario.isEmpty() || cifUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Contenedor de un campo xml
        container = findViewById(R.id.contentLayout)


        // Crea spinner para seleccionar pisos
        spinnerPisos = Spinner(this).apply {
            // Formato en el que se mostrará el mení desplegable
            setPopupBackgroundDrawable(
                ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
            )

            // Define el tamaño y posicionamiento
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START // Posicionamiento en la toolbar
                marginStart = 0         // Sin margen
            }
        }
        // Agrega el spinner a la toolbar
        toolbar.addView(spinnerPisos)

        // obtine el nombre de la empresda del usuario logueado
        val nombreEmpresa = sesion.empresa.nombre
        // extrae la lista de pisos
        db.collection("empresas")
            .document(nombreEmpresa)
            .collection("pisos")
            .get()
            .addOnSuccessListener { documents ->
                // Guarda los pisos como un objeto de la dataClass correspondiente, asigna el id
                listaPisos = documents.mapNotNull { it.toObject(Piso::class.java).apply { id = it.id } }
                // Mapea la lista de piscos y toma el nombre
                val nombresPisos = listaPisos.map { it.nombre }

                // Adapta la lista de pisos con una array y los mestra en el spinner
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresPisos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // introduce los pisos al spinner
                spinnerPisos.adapter = adapter
            }
            // Creación de una excepción en caso de no poder cargar los pisos
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar pisos", Toast.LENGTH_SHORT).show()
            }

        // Al seleccionar un objeto (piso del spinner)
        spinnerPisos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Coje la posición, el id y el nombre
                val piso = listaPisos[position]
                pisoSeleccionado = piso.id
                val nombrePiso = piso.nombre

                // llamada a la función de cargar salas
                cargarSalas()
                // con una corrutina carga la función para el findo del piso (imagen)
                lifecycleScope.launch {
                    cargarImagenFondo(nombrePiso)
                }
            }

            // En caso de no elegir ningún piso, no realiza ninguna acción
            override fun onNothingSelected(parent: AdapterView<*>) {
                pisoSeleccionado = null
                container.removeAllViews()
            }
        }

        // Definición del botón reervas y carga de su función
        val btnReservas = findViewById<LinearLayout>(R.id.btn_reservas)
        btnReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        // definición del botón franjas horarias
        val btnFranja = findViewById<LinearLayout>(R.id.btn_franja)
        btnFranja.setOnClickListener {
            // Si no se ha seleccionado franja horaria
            if (fechaSeleccionada.isEmpty()) {
                // Oculta el snackbar si ya hay uno (superposición)
                snackbarActivo?.dismiss()
                // Crea un snackbar informando de las acciones a realizar
                snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                    // botón de acción del snackbar
                    .setAction("Seleccionar fecha") { mostrarDialogoFecha() }
                snackbarActivo?.show() // muestra el snackbar
            } else {
                // Si hay fecha seleccionada, se eecuta el metodo indicado
                mostrarDialogoHoras()
            }
        }

        // definición de botón con imagen, carga la imagen, pone el background transparente y se le da función al clickarle
        val botonSeleccionarFechaHora = ImageButton(this).apply {
            setImageResource(R.drawable.ic_calendar)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarDialogoFecha() }
            // Se añade la imagen a la toolbar y se le da formato en el posicionamiento
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginEnd = 20
                marginStart = 20
            }
        }
        toolbar.addView(botonSeleccionarFechaHora) // Añade funcionalidad a la toolbar

        // Crea un contenedor para las fechas y horas, se le da formato y posicionamiento
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

        // Añaden textView con formato
        textViewFecha = TextView(this).apply {
            text = "Fecha:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        // Añaden textView con formato
        textViewHora = TextView(this).apply {
            text = "Hora:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        // añade al contenedor los textView creados anteriormete
        contenedorFechaHora.addView(textViewFecha)
        contenedorFechaHora.addView(textViewHora)
        // añade el contenedor a la toolbar
        toolbar.addView(contenedorFechaHora)
    }

    // Opciones a realizar con la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Volver a la pantalla anterior
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Función para cargar las salas del piso seleccionado
    private fun cargarSalas() {
        // Quita rodas las vistas del contenedor
        container.removeAllViews()

        // extraemos los datos de la sesión del usuario logueado
        val empresaId = sesion?.empresa?.nombre ?: ""
        val pisoId = pisoSeleccionado.toString()

        // Referencia a la subcolección salas dentro del piso
        val salasRef = db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(pisoId)
            .collection("salas")

        // Obtenemos las salas registradas en el piso
        salasRef.get()
            .addOnSuccessListener { querySnapshot ->
                // Comporbamos que existan salas
                if (!querySnapshot.isEmpty) {
                    // Transforma los datos de la Bd en Objetos de la dataClass salas
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
                        // Se ignoran las excepciones, errores
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Recorre las salas
                    for (sala in salas) {
                        // se crea un botón sala
                        val salaButton = Button(this@Activity_empleados).apply {
                            // Muestra información de salas
                            text = formatearTextoSala(sala)
                            // guarda las salas como etiquetas
                            tag = sala

                            // Al clicar en una sala
                            setOnClickListener {
                                // Si no hay fecha y hora seleccionada
                                if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                                    // muestra una snackbar con texto y llamada a una función
                                    snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                                        .setAction("Seleccionar fecha") { mostrarDialogoFecha() }
                                    // muestra el snackbar
                                    snackbarActivo?.show()
                                } else {
                                    // En caso de que haya fecha y hora muestra diálogo para los detalles de la sala
                                    mostrarDialogoDetallesSala(
                                        sala,
                                        pisoNombre = pisoId
                                    )
                                }
                            }

                            // Definición del estilo del botón
                            background = GradientDrawable().apply {
                                setColor(Color.GRAY)
                                cornerRadius = 50f
                            }

                            // Cierra cualquier snackbar activo
                            snackbarActivo?.dismiss()

                            // Definición del posicionamiento y tamaño del botón de las salas
                            layoutParams = ConstraintLayout.LayoutParams(
                                sala.ancho.toInt(),
                                sala.alto.toInt()
                            ).apply {
                                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                                setMargins(sala.x.toInt(), sala.y.toInt(), 0, 0)
                            }
                        }

                        // Añade el botón al contenedor principal
                        container.addView(salaButton)
                    }

                    // En caso de existir fecha y hora, verifica disponiblidad de salas
                    if (fechaSeleccionada.isNotEmpty() && horaSeleccionada.isNotEmpty()) {
                        verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada",
                            pisoSeleccionado.toString()
                        )
                    }
                    // En caso de no existir salas, muestra el siguiente mensaje
                } else {
                    Toast.makeText(this, "No hay salas definidas para este piso", Toast.LENGTH_SHORT).show()
                }
            }
            // Creación de una excepción con mensaje en caso de no poder cargar salas
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para cargar la imagen de fondo
    private fun cargarImagenFondo(nombrePiso: String) {
        // variable para contener el layout
        val contentLayout = findViewById<ConstraintLayout>(R.id.contentLayout)

        // Guarda el nombre de la empresa en una variable
        val empresaId = sesion?.empresa?.nombre ?: ""

        // Si el nombre de la empresa (Id en BD) no existe
        if (empresaId.isEmpty()) {
            // Carga el findo de pantalla vacío
            contentLayout.setBackgroundResource(R.drawable.wallpaper)
            // Salta este mensaje de error
            Toast.makeText(this, "no se carga la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // En caso de existir la empresa, accedemos a la colección del pisos y extraemos el nombre
        db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(nombrePiso)
            .get()
            .addOnSuccessListener { document ->
                // En caso de que exista la sala
                if (document.exists()) {
                    // Carga la imagen
                    val imagenUrl = document.getString("imagenUrl")

                    // En caso de que no exista una empresa saltará el siguiente mensaje
                    if (imagenUrl.isNullOrEmpty()) {
                        Toast.makeText(this, "No hay imagen de fondo para este piso", Toast.LENGTH_SHORT).show()
                    // En caso de existir imagen
                    } else {
                        //Guarda la imagen en un objeto, indicando actividad de referencia, imagen a cargar y dónde se quiere guardar
                        Glide.with(this@Activity_empleados)
                            .load(imagenUrl)
                            .centerCrop()
                            // guardamos la imagen como drawable
                            .into(object : CustomTarget<Drawable>() {
                                // Carga la imagen para usarla
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    contentLayout.background = resource
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // No se requiere acción
                                }
                                //Usa el fondo de la aplicaicón en caso de fallar
                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    contentLayout.setBackgroundResource(R.drawable.wallpaper)
                                }
                            })
                    }
                    // En caso de que la sala no exista se carga el fiondo de pantalla de la app
                } else {
                    contentLayout.setBackgroundResource(R.drawable.wallpaper)
                }

            }
            // en caso de saltar alguna excepciópn a la hora de caragar la sala, también cargará el fondo de pantalla de la app
            .addOnFailureListener {
                contentLayout.setBackgroundResource(R.drawable.wallpaper)
            }
    }

    // Función que mustra diálogo para seleccionar fechas
    private fun mostrarDialogoFecha() {
        // Descativa cualquier snackbar que esté activo
        snackbarActivo?.dismiss()
        // instanciamos el calendario y lo guardamos en una variable
        val calendario = Calendar.getInstance()

        // Sadrá un diálogo para elegir una fecha con estilo
        val datePickerDialog = DatePickerDialog(
            this,
            android.R.style.Theme_Material_Dialog_MinWidth,
            // almacenará la fecha con el siguiente formato
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                textViewFecha.text = "Fecha: $fechaSeleccionada" // muestra fecha seleccionada
                mostrarDialogoHoras()
            },
            // Cargamos los valores actuales del calendario
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        // Establece la fecha mínima (actual)
        datePickerDialog.datePicker.minDate = calendario.timeInMillis

        // Cambio del fondo del diálogo
        datePickerDialog.setOnShowListener {
            datePickerDialog.window?.setBackgroundDrawableResource(R.drawable.datepicker_background)
        }

        // Muestra el diálogo
        datePickerDialog.show()
    }

    // función para mostrar el diálogo de reservas
    private fun mostrarDialogoReservas() {
        // En caso de que existan reservas anteriores a la fecha actual, se eliminan
        limpiarReservasPasadas()
        // Accede a la sesión de firebase, al nombre de la empresa y a id(correo) de firebase
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
                        Toast.makeText(
                            this@Activity_empleados,
                            "No tienes reservas activas.",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        val pisoText = TextView(this@Activity_empleados).apply {
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
                            // Recorre la lista de pisos de ese piso
                            val reservaText = TextView(this@Activity_empleados).apply {
                                // Nombre de sala + fecha y hora
                                text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                                setPadding(16, 4, 0, 4)
                                setTextColor(Color.DKGRAY)
                                // Al hacer click en la reserva abre un mensaje de confirmación de conaclación con título, mensaje y dos botones
                                setOnClickListener {
                                    val confirmDialog = AlertDialog.Builder(this@Activity_empleados)
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
                                                    // guarda la información en una función
                                                    verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada",
                                                        pisoSeleccionado.toString())
                                                    // Cración de una excepción que mostrará el mensaje de error en el hilo pricipal
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
                                        // En caso de darla al botón no se realiza ninguna acción
                                        .setNegativeButton("No", null)
                                        .create()

                                    // Personalización del mensaje de los diálogos
                                    confirmDialog.setOnShowListener {
                                        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                                        confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                                    }
                                    confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                    confirmDialog.show()
                                }
                            }
                            // Añade la reserva al piso
                            contenedor.addView(reservaText)
                        }

                        // Añade una línea divisoria entre los grupos de pisos
                        val divider = View(this@Activity_empleados).apply {
                            setBackgroundColor(Color.LTGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2
                            ).apply { setMargins(0, 16, 0, 16) }
                        }
                        contenedor.addView(divider) // Añade la línea al contenedor
                    }

                    // Crea la el constructor para pantalla de diálogo para ver reservas activas

                    dialog = AlertDialog.Builder(this@Activity_empleados)
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
                    Toast.makeText(this@Activity_empleados, "Error cargando reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Función para eliminar reservas pasadas
    private fun limpiarReservasPasadas() {
        // Variable que guarda el formato de fecha-hora
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Accede al nombre de la empresa en firebase
        val nombreEmpresa = sesion?.empresa?.nombre

        // En caso de no encontra el nombre de la empresa
        if (nombreEmpresa.isNullOrBlank()) return
        // Se guarda el nombre de la empresa
        val empresaId = nombreEmpresa

        // Acciones que se realizan en una corrutina
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Acceso a la coleccion de reservas de firestore
                val snapshot = db.collection("empresas")
                    .document(empresaId)
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
                        db.collection("empresas")
                            .document(empresaId)
                            .collection("reservas")
                            .document(idDoc)
                            .delete()
                            .await() // Espera hasta que realiza la acción
                    }
                }
                // Crea una excepción
            } catch (e: Exception) {
                // Opcional: Log.e("limpiarReservasPasadas", "Error al limpiar reservas", e)
            }
        }
    }

    // Creación variable para mostrar el diálogo de selección de horas
    private fun mostrarDialogoHoras() {
        // En caso de no haber seleccionado una fecha, muestra el siguiente mensaje
        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Primero selecciona una fecha.", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtiene el nombre de la empresa desde la sisión
        val empresaId = sesion?.empresa?.nombre ?: ""
        // Si no se encuentra la empresa, muestra el siguiente mesnaje
        if (empresaId.isEmpty()) {
            Toast.makeText(this, "Empresa no identificada.", Toast.LENGTH_SHORT).show()
            return
        }

        // Uso de corrutina para ejectuar acciones
        lifecycleScope.launch {
            try {
                // recupera las franjas horarias esde firestore
                val franjasOriginales = withContext(Dispatchers.IO) {
                    val snapshot = db.collection("empresas")
                        .document(empresaId)
                        .collection("franjasHorarias")
                        .get()
                        .await()

                    // Usa el id para obtener las franjas horarias
                    snapshot.documents.map { it.id } // ✅ usamos el ID como hora
                }

                // Si no hay franjas horarias, en el hilo pricipal mostrará el siguiente mensaje
                if (franjasOriginales.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Formatea la fecha y se comprueba si se seleccionó la fecha actual
                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val hoy = sdfFecha.format(Date())

                // Obtiene la hora actual con formato específico
                val calendario = Calendar.getInstance()
                val horaActual = String.format("%02d:%02d", calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE))

                // Filtra franjas horarias del día actual, en ser cierto, filtra los horario a partir de la hora actual
                val horasDisponibles = if (fechaSeleccionada == hoy) {
                    franjasOriginales.filter { it.split("-").first() >= horaActual }
                // EN caso de no ser hoy, muestra todas las franjas horarias
                } else {
                    franjasOriginales
                }

                // En caso de no existir franjas horarias, en el hilo principal mostrará el siguiente mensaje
                if (horasDisponibles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Acciones se ejecutarán en hilo principal
                withContext(Dispatchers.Main) {
                    // Variable que guarda horas disponibles
                    val horasArray = horasDisponibles.toTypedArray()

                    // crea un constructor de diálogos con título
                    val dialog = AlertDialog.Builder(this@Activity_empleados)
                        .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
                        .setItems(horasArray) { _, which ->
                            // Al seleccionar una hora, se guarda y muestra mensje
                            horaSeleccionada = horasArray[which]
                            textViewHora.text = "Hora: $horaSeleccionada"
                            // verifica si se puede seleccionar la sala en fecha y hora seleccionada
                            verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada",
                                pisoSeleccionado.toString()
                            )
                        }
                        // Botón para cerrar el diálogo, cancela las accions
                        .setNegativeButton("Cerrar", null)
                        .create()

                    // Personaliza el botón
                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
                }

                // Crea excepción en caso de que dé algún error
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al cargar franjas horarias: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    // Función para formatear el texto de una sala
    private fun formatearTextoSala(sala: Salas): String {
        // Crea un constuctor que haga referencia al nombre de una sala
        val builder = StringBuilder().append(sala.nombre)
        // En caso de que el apartado de extras en la sala esté relleno, se les da formato deseado
        if (sala.extras.isNotEmpty()) {
            builder.append("\n")
            sala.extras.forEach { extra ->
                val icono = when (extra) {
                    "WiFi" -> "📶 "
                    "Proyector" -> "📽️ "
                    "Pizarra" -> "🖍️ "
                    else -> ""
                }
                // añade el icono al contructor
                builder.append(icono)
            }
        }
        // pada el contructor a string
        return builder.toString()
    }

    // Función para comprobar disponibilidad de salas
    private fun verificarDisponibilidad(fechaHora: String, nombrePiso: String) {
        // Obtenemos el nombre de la empresa de la sesión actual
        val nombreEmpresa = sesion?.empresa?.nombre ?: return
        // Obtenemos el id (correo) del usuario logueado
        val idUsuarioActual = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Acciones que se realizan una corrutina
        lifecycleScope.launch {
            try {
                // Consulta reservas en firestore con fecha y hora seleccionadas
                val reservasSnapshot = db.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get()
                    .await()

                // Mapeamos todas las reservas con idSala, piso y idusuario
                val reservas = reservasSnapshot.documents.mapNotNull { doc ->
                    val idSala = doc.getString("idSala")?.trim() ?: return@mapNotNull null
                    val piso = doc.getString("piso")?.trim() ?: return@mapNotNull null
                    val idusuario = doc.getString("idusuario") ?: return@mapNotNull null
                    Triple(idSala, piso, idusuario)
                }

                // Se recorre la lista de las salas y se muestran
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    // Si la sala tiene está etiquietada como sala
                    if (view.tag is Salas) {
                        val sala = view.tag as Salas

                        // Buscamos si está reservada
                        val reserva = reservas.find { (idSalaRes, pisoRes, _) ->
                            idSalaRes.equals(sala.id?.trim(), ignoreCase = true) &&
                                    pisoRes.equals(nombrePiso.trim(), ignoreCase = true)
                        }

                        // Creamos formato de colores según sea el estado de la sala
                        val color = when {
                            reserva == null -> R.color.green // libre
                            reserva.third == idUsuarioActual -> R.color.orange // ocupada por mí
                            else -> R.color.red // ocupada por otro
                        }

                        // Cambia el background de la sala según su estado
                        view.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this@Activity_empleados, color)
                        )
                    }
                }
            // Se crea una excepción en caso de que haya algún error
            } catch (e: Exception) {
                Toast.makeText(
                    this@Activity_empleados,
                    "Error al verificar disponibilidad: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Función para mostrar diálogo con los detalles de la sala
    private fun mostrarDialogoDetallesSala(sala: Salas, pisoNombre: String) {
        // Guarda la fecha y hora en una variable
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        // Obtiene datos de sesión
        val nombreEmpresa = sesion?.empresa?.nombre ?: ""
        val nombreUsuario = sesion?.usuario?.nombre ?: ""
        val currentUser = FirebaseAuth.getInstance().currentUser
        val idUsuario = currentUser?.uid ?: ""

        // En caso de no encontrar un nombre de usuario, muestra el siguiente mensaje
        if (nombreUsuario == "") {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Se realizan acciones en una corrutina
        lifecycleScope.launch {
            try {
                // Obtenemos el nombre del piso desde firestore
                val pisoSnapshot = db.collection("empresas").document(nombreEmpresa)
                    .collection("pisos").document(pisoNombre).get().await()

                // Se almacena el nombre en una variable
                val pisoNombreReal = pisoSnapshot.getString("nombre") ?: pisoNombre

                // Cargan reservas que coincidan con fecha y hora seleccionadas
                val reservasSnapshot = db.collection("empresas").document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .get().await()

                // Guardamos los datos como objetods de la dataClass Reservas
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

                // Comprobación si el usuario tiene ya una reserva en esa franja horaria
                val reservaUsuarioMismaHora = reservas.find { it.idusuario == idUsuario }
                // Buscamos si la sala está teservada en el piso
                val reservaExistente = reservas.find {
                    it.idSala.trim().equals(sala.id?.trim(), ignoreCase = true) &&
                            it.piso.trim().equals(pisoNombreReal.trim(), ignoreCase = true)
                }

                // Carga de layout con el diñalogo personalizado
                val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)
                dialogView.findViewById<TextView>(R.id.tvValorPiso).text = pisoNombreReal
                dialogView.findViewById<TextView>(R.id.tvValorTamano).text = sala.tamaño
                dialogView.findViewById<TextView>(R.id.tvValorExtras).text =
                    if (sala.extras.isNotEmpty()) sala.extras.joinToString(", ") else "Ninguno"
                dialogView.findViewById<TextView>(R.id.tvValorFecha).text = fechaSeleccionada
                dialogView.findViewById<TextView>(R.id.tvValorHora).text = horaSeleccionada

                // Creamos un constructor para el diálogo
                val builder = AlertDialog.Builder(this@Activity_empleados)
                    .setTitle("Detalles de ${sala.nombre}")
                    .setView(dialogView)
                    .setNegativeButton("Cerrar") { dialog, _ -> dialog.dismiss() }

                // Cuando
                when {
                    // Usuario tiene reservada la sala, puede cancelar la reserva
                    reservaExistente != null && reservaExistente.idusuario == idUsuario -> {
                        builder.setPositiveButton("Cancelar reserva") { _, _ ->
                            // Acción ejecutada en una corrutina
                            lifecycleScope.launch {
                                try {
                                    // accede a los datos de fitrestore y elimina la reserva
                                    db.collection("empresas").document(nombreEmpresa)
                                        .collection("reservas").document(reservaExistente.id.toString())
                                        .delete().await()
                                    verificarDisponibilidad(fechaHora, pisoNombreReal)
                                    // Muestra mensaje de cancelación de reserva
                                    Toast.makeText(this@Activity_empleados, "Reserva cancelada", Toast.LENGTH_SHORT).show()
                                // Crea una excepción en caso de no poder cancelar la reserva
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

                    // Sala reservada por otro usuario → no se puede reservar ni cancelar, muestra el estado
                    reservaExistente?.idusuario != idUsuario -> {
                        builder.setPositiveButton("Reservada", null)
                    }
                }

                // Personaliza el formato el diálogo
                val dialog = builder.create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                // Se ajusta el color de los botones del diálogo
                dialog.setOnShowListener {
                    val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positive?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                    negative?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))

                    // Si la sala está reservada por otra pesrsona, el botñon se descativa
                    if (reservaExistente != null && reservaExistente.idusuario != idUsuario) {
                        positive?.isEnabled = false
                        positive?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.grey))
                        Toast.makeText(this@Activity_empleados, "Sala reservada por ${reservaExistente.nombreUsuario}", Toast.LENGTH_SHORT).show()
                    }
                }
                // Muestra el diálogoa
                dialog.show()

                //Crea una execpión en caso de haver algún error
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al cargar detalles: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Función para reservar salas
    private fun reservarSala(sala: Salas, pisoNombreReal: String) {
        // Obtiene datos de sesión
        val sesion = Sesion.datos
        // Almacenamiento ede fecha y hora
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        // AGuardan datos de usuario y empresa
        val nombreUsuario = sesion?.usuario?.nombre ?: ""
        val nombreEmpresa = sesion?.empresa?.nombre ?: ""
        val currentUser = FirebaseAuth.getInstance().currentUser
        val idUsuario = currentUser?.uid ?: ""

        // En caso de no encontrar usuario o empresa mostrará el siguiente mensaje
        if (nombreUsuario.isEmpty() || nombreEmpresa.isEmpty()) {
            Toast.makeText(this, "Información de usuario o empresa incompleta", Toast.LENGTH_SHORT).show()
            return
        }

        // Acciones a realizar en una corrutina
        lifecycleScope.launch {
            try {
                // Busca en las reservas aquellas que estén seleccionadas por el usaurio y la hora seleccionadas
                val reservasSnapshot = db.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("fechaHora", fechaHora)
                    .whereEqualTo("idusuario", idUsuario)
                    .get().await()

                // En caso de existir se muestra el siguiente mensaje en el hilo principal
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

                // Si no hay conflicto, se crea la reserva
                val nuevaReserva = hashMapOf(
                    "nombreSala" to sala.nombre,
                    "idSala" to (sala.id ?: ""),
                    "fechaHora" to fechaHora,
                    "nombreUsuario" to nombreUsuario,
                    "idusuario" to idUsuario,
                    "piso" to pisoNombreReal
                )

                // Se almacena la reserva en la BD de firebase en la emporesa correspondiente
                db.collection("empresas").document(nombreEmpresa)
                    .collection("reservas")
                    .add(nuevaReserva)
                    .await()

                // Una vez guardado, en el hilo principal, motrará el siguiente mensaje
                withContext(Dispatchers.Main) {
                    verificarDisponibilidad(fechaHora, pisoNombreReal)
                    Toast.makeText(this@Activity_empleados, "Reserva confirmada", Toast.LENGTH_SHORT).show()
                }
            // Se crea una excepción en caso de que haya algún fayo a la hora de reservar
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al reservar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}