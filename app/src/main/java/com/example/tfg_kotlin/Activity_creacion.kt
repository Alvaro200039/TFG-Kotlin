package com.example.tfg_kotlin

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.Utils.naturalOrderKey
import com.example.tfg_kotlin.Utils.compareNaturalKeys
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Salas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.example.tfg_kotlin.BBDD_Global.Entities.UsuarioSesion
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage


class Activity_creacion : AppCompatActivity() {

         // Creación de variables globales
        private lateinit var container: ConstraintLayout
        private var pisoActual: Piso? = null
        private var cif: String = ""
        private lateinit var firestore: FirebaseFirestore
        private val storage = FirebaseStorage.getInstance()
        private lateinit var auth: FirebaseAuth
        private var imagen: ByteArray? = null
        private val salasEnMemoria = mutableListOf<Salas>() // Lista temporal en memoria


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creacion)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.creacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Comprobamos que el usuario actual está autentificado
        val currentUser = auth.currentUser
        // De no ser así, saldrá el siguiente mensaje
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener el CIF desde Sesion.datos, en caso de no encontrarlo, muestra un mensaje
        cif = Sesion.datos?.empresa?.cif ?: ""
        if (cif.isEmpty()) {
            Toast.makeText(this, "CIF no disponible en sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar UI con CIF obtenido de sesión
        inicializarUI()
    }


    private fun inicializarUI() {
        // Obtener el nombre de la empresa desde la sesión
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Inicializa el textview de forma provisional
        val titleView = findViewById<TextView>(R.id.toolbar_title)
        titleView.text = "Piso nº"
        titleView.setOnClickListener {
            showChangeTitleDialog()
        }

        // Cargar pisos desde Firestore de forma ascendente
        firestore.collection("empresas")
            .document(nombreEmpresa)
            .collection("pisos")
            .orderBy("nombre", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Se consultan los pisos como objetos de la dataClass correspondiente
                val pisos = querySnapshot.toObjects(Piso::class.java)
                // En casos de que haya pisos, el actual se posiciona como el último
                if (pisos.isNotEmpty()) {
                    pisoActual = pisos.last()
                    titleView.text = pisoActual?.nombre ?: "Sin nombre"

                    // Guardar el nombre del piso si necesitas usarlo después
                    getSharedPreferences("mi_preferencia", MODE_PRIVATE).edit().apply {
                        putString("numero_piso", pisoActual?.nombre)
                        apply()
                    }
                // EN caso de que no haya pisos creados, sale este mensaje
                } else {
                    Toast.makeText(this, "No hay pisos creados", Toast.LENGTH_SHORT).show()
                }
            }
            // Usa excepción para controlar errores
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando pisos", Toast.LENGTH_SHORT).show()
            }

        // Botones
        val btnHoras = findViewById<LinearLayout>(R.id.btn_horas)
        val btnPlano = findViewById<LinearLayout>(R.id.btn_plano)
        val btnSala = findViewById<LinearLayout>(R.id.btn_sala)
        val btnPisos = findViewById<LinearLayout>(R.id.btn_pisos)

        // Asignamos funcinonalidad a los botones
        btnHoras.setOnClickListener { cargarFranjas() }
        btnPisos.setOnClickListener { mostrarDialogoEliminarPisos(nombreEmpresa) }
        btnPlano.setOnClickListener { openGallery() }
        btnSala.setOnClickListener { addMovableButton() }

        // Utilizamos el contentedor
        container = findViewById(R.id.container)
    }

    // Inflamos el menú de la toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_drag, menu)
        return true
    }

    // Opciones a realizar con la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Volver a la pantalla anterior cuando se da al icono
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            // Si se selecciona guardar
            R.id.action_save -> {
                // Obtiene título de la toolbar
                val toolbarTitle = findViewById<TextView>(R.id.toolbar_title).text.toString()

                // Usa corrutina para realizar acción
                lifecycleScope.launch {
                    try {
                        // Aquí extraes la imagen de fondo desde el fondo del layout, si es necesario
                        val imageView = findViewById<ImageView>(R.id.image_fondo)
                        val fondoBitmap = (imageView.drawable as? BitmapDrawable)?.bitmap
                        // Guarda el fondo en un mapa de bytes (para posicionamiento de salas)
                        val fondoBytes = fondoBitmap?.let { bitmap ->
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.toByteArray()
                        }
                        // Usa la función de guardar pasándole los siguientes datos
                        guardarDistribucion(
                            pisoNombre = toolbarTitle,
                            imagen = fondoBytes,
                            container = findViewById(R.id.container),
                            sesion = Sesion.datos
                        )
                        // Crea una exepción para controlar errores y muestra el mensaje en el hilo principal
                    } catch (e: Exception) {
                        Log.e("Activity_creacion", "Error guardando: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Activity_creacion, "Error guardando: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                true
            }
            // comprueba que se selecciona otro botón no tenido en cuenta
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Funcion para cargar y gestionar franjas horarias
    private fun cargarFranjas() {
        // Infla layout para el usar el diálogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_franjas_horas, null)
        val layoutFranjas = dialogView.findViewById<LinearLayout>(R.id.layoutFranjas)
        val botonAgregar = dialogView.findViewById<Button>(R.id.btnAddFranja)

        // Referencia a campos de editText
        val editHoraInicio = dialogView.findViewById<EditText>(R.id.etHoraInicio)
        val editMinutoInicio = dialogView.findViewById<EditText>(R.id.etMinInicio)
        val editHoraFin = dialogView.findViewById<EditText>(R.id.etHoraFin)
        val editMinutoFin = dialogView.findViewById<EditText>(R.id.etMinFin)

        // Cinfiguraciño de avance autometico tra rellenas cada campo
        editHoraInicio.autoAdvanceTo(editMinutoInicio)
        editMinutoInicio.autoAdvanceTo(editHoraFin)
        editHoraFin.autoAdvanceTo(editMinutoFin)
        editMinutoFin.autoAdvanceTo(null)

        // Crea constructor para el diálogo y crea el siálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle("Añadir franjas horarias")
            .setView(dialogView)
            .setNegativeButton("Cerrar", null)
            .create()

        // Personaliza el formato del la pantall diálogo
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)

        // Comprueba que el cif está bien introducido
        val cifNormalizado = cif.trim().uppercase()
        // En caso de no ser así saldrá el siguiente mensaje
        if (cifNormalizado.isBlank()) {
            Toast.makeText(this, "Error: CIF de empresa no definido", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        // Utiliza corrutina para scceder a firestore
        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // Buscar empresa por CIF (ya que el ID del documento es el nombre)
                val empresaSnapshot = firestore.collection("empresas")
                    .whereEqualTo("cif", cifNormalizado)
                    .get()
                    .await()

                // Si no se encuentra, muestra mensaje en el hilo principal y cierra el diálogo
                if (empresaSnapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_creacion, "Empresa no encontrada", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    return@launch
                }

                // Obtener el ID del documento (nombre de la empresa)
                val nombreEmpresa = empresaSnapshot.documents[0].id

                // Cargar franjas existentes
                val snapshotFranjas = firestore.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("franjasHorarias")
                    .get()
                    .await()

                // Guarda las franjas en un mapa por el id
                val franjas = snapshotFranjas.documents.mapNotNull { it.id }
                // Muestra frnajas en el layout
                actualizarListaFranjas(franjas, layoutFranjas, nombreEmpresa)

                // Botón Agregar franjas
                botonAgregar.setOnClickListener {
                    // Obtiene valores introducidos
                    val hInicio = editHoraInicio.text.toString().padStart(2, '0')
                    val mInicio = editMinutoInicio.text.toString().padStart(2, '0')
                    val hFin = editHoraFin.text.toString().padStart(2, '0')
                    val mFin = editMinutoFin.text.toString().padStart(2, '0')

                    // Verifica que no haya ningún vampo vacío
                    if (hInicio.isBlank() || mInicio.isBlank() || hFin.isBlank() || mFin.isBlank()) {
                        Toast.makeText(this@Activity_creacion, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Convierte los datos a valores numéricos
                    val hInicioInt = hInicio.toIntOrNull()
                    val mInicioInt = mInicio.toIntOrNull()
                    val hFinInt = hFin.toIntOrNull()
                    val mFinInt = mFin.toIntOrNull()

                    // Validar que sean números válidos
                    if (hInicioInt == null || mInicioInt == null || hFinInt == null || mFinInt == null) {
                        Toast.makeText(this@Activity_creacion, "Introduce solo números válidos", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Validación de rangos de horas
                    if (hInicioInt !in 0..23 || hFinInt !in 0..23) {
                        Toast.makeText(this@Activity_creacion, "La hora debe estar entre 0 y 23", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Validación de rangos de minutos
                    if (mInicioInt !in 0..59 || mFinInt !in 0..59) {
                        Toast.makeText(this@Activity_creacion, "Los minutos deben estar entre 0 y 59", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Crear hora en formato string para comparar
                    val horaInicio = "$hInicio:$mInicio"
                    val horaFin = "$hFin:$mFin"

                    // En caso de que la hora de inicio sea mayor a la de final, dará el siguiente mensaje
                    if (horaInicio >= horaFin) {
                        Toast.makeText(this@Activity_creacion, "La hora de inicio debe ser menor que la de fin", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Guarda la franja
                    val franja = "$horaInicio-$horaFin"

                    // Usa una corrutina para acceder a firebase y guardar la franja horaria
                    lifecycleScope.launch {
                        try {
                            firestore.collection("empresas")
                                .document(nombreEmpresa)
                                .collection("franjasHorarias")
                                .document(franja)
                                .set(mapOf("activo" to true)) // Id de la franja
                                .await()

                            // Recargar lista de franjas horarias disponibles
                            val nuevoSnapshot = firestore.collection("empresas")
                                .document(nombreEmpresa)
                                .collection("franjasHorarias")
                                .get()
                                .await()

                            val nuevasFranjas = nuevoSnapshot.documents.mapNotNull { it.id }
                            actualizarListaFranjas(nuevasFranjas, layoutFranjas, nombreEmpresa)

                            // Limpiar campos
                            editHoraInicio.text.clear()
                            editMinutoInicio.text.clear()
                            editHoraFin.text.clear()
                            editMinutoFin.text.clear()

                            // Crea exceoción en caso de haber fallo al intentar guardar la franja horaria, en el hilo principal mostrará el siguiente mensaje
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Activity_creacion, "Error al guardar franja: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            // Crea excepción en caso de hacer fallo a la hora de cargar horarios, en el hilo principal mostrará el siguiente mensaje
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Error cargando franjas: ${e.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
    }

    // Función para actualizar la lista de franajas horarias, tipo de dato, layout a mostrar y el nombre de la empresa(ID)
    private fun actualizarListaFranjas(
        franjas: List<String>,
        layoutFranjas: LinearLayout,
        nombreEmpresa: String
    ) {
        // Limpia la vista para que no haya valores duplicados
        layoutFranjas.removeAllViews()

        // Instancia firestores
        val firestore = FirebaseFirestore.getInstance()

        // Recorre la lista de feranjas ordenadas
        franjas.sorted().forEach { franja ->
            // Layout que almacenará cada franja, se defie su formato, posicionamiento, márgenes y background
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                background = ContextCompat.getDrawable(this@Activity_creacion, R.drawable.dialog_background)
            }

            // Crea textViev con formato para amlacenar los datos de la franja c
            val textView = TextView(this).apply {
                text = franja
                textSize = 16f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Crea botón para eliminar franjas con formato definido
            val botonEliminar = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.RED)
                setPadding(24, 0, 0, 0)
                // Al clickar el botón, accederá a las franas horairas de firestore mediante una corrutina y eliminará la franja
                setOnClickListener {
                    lifecycleScope.launch {
                        try {
                            firestore.collection("empresas")
                                .document(nombreEmpresa)
                                .collection("franjasHorarias")
                                .document(franja)
                                .delete()
                                .await()

                            // Vuelve a cargar las franjas de firestore
                            val nuevoSnapshot = firestore.collection("empresas")
                                .document(nombreEmpresa)
                                .collection("franjasHorarias")
                                .get()
                                .await()

                            val nuevasFranjas = nuevoSnapshot.documents.mapNotNull { it.id }

                            // Refresca la lista de franjas
                            actualizarListaFranjas(nuevasFranjas, layoutFranjas, nombreEmpresa)

                        // Definició de una excepción en casdo de dar error al eliminar franjas; mostrará menseje en hilo principal
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Activity_creacion, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            // Agreda el botón y el textView al layout
            itemLayout.addView(textView)
            itemLayout.addView(botonEliminar)
            layoutFranjas.addView(itemLayout)
        }
    }

    // Función para abrir galeria del teléfono y cargar una imagen
    private fun openGallery() {
        getImage.launch("image/*")
    }

    // Función para cear un botón movible (salas)
    private fun addMovableButton() {
        // No compruebo pisoActual porque aún no existe piso guardado

        // Instanciación de la clase sala con datos iniciales
        val sala = Salas(
            nombre = "Sala",
            tamaño = "pequeña", // o "grande"
            x = 100f,
            y = 100f,
            ancho = 300f,
            alto = 200f,
            extras = emptyList(),
            id = null
        )


        // Botón con el formato-medidads definidas anteriormente, aplica nombre, background, padding...
        val button = Button(this).apply {
            text = sala.nombre
            background = GradientDrawable().apply {
                setColor("#BEBEBE".toColorInt())
                cornerRadius = 50f
            }
            setPadding(50, 20, 50, 20)
            // Permite mover el botón
            setOnTouchListener(MovableTouchListener())
            // Al clickar muestra las opciones internas del botón
            setOnClickListener {
                showButtonOptions(this)
            }
            // Asocia el objeto sala a una etiqueta
            tag = sala
        }

        // Agrega el botón al contenedor
        container.addView(button)

        // Posicionamiento del botón en el layout y cuanto ocupará
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = sala.y.toInt()
            leftMargin = sala.x.toInt()
        }
        // Aplica corrdenadas definiadas al botón
        button.layoutParams = layoutParams

        // Guarda la sala
        salasEnMemoria.add(sala)
    }

    // Función para mostrar diálogo de cambio del nombre de los pisos
    private fun showChangeTitleDialog() {
        // Obtiene el piso actual
        val piso = pisoActual

        // Crea editText con el nombre del piso actual, en caso de no encontrarlo crea un nombre por defecto
        val editText = EditText(this).apply {
            setText(piso?.nombre ?: "Piso nº ")
            setSelection(text.length)
        }

        // Crea layout con formato que contendrá el editText
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        // Creación de mensaje en caso de que se encuentre o no un piso
        val title = if (piso == null) "Crear nuevo piso" else "Editar nombre del piso"

        // Constructor para el diálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            // En caso de dar al botón de "Guardar", per,mire un máximo de 11 caracteres a introducir
            .setPositiveButton("Guardar") { _, _ ->
                val maxTitleLength = 11
                val nuevoTitulo = editText.text.toString().trim().take(maxTitleLength)

                // Valida que el editText esté vacio o tenga el nombre por defecto
                if (nuevoTitulo.isEmpty() || nuevoTitulo.equals("Piso nº", ignoreCase = true) || nuevoTitulo.equals("Piso nº ", ignoreCase = true)) {
                    showToast("Por favor, cambie el nombre del piso antes de guardar")
                } else {
                    // En caso de introducir un titulo nuevo, se crea un nuevo piso con los datos correspondientes
                    pisoActual = if (piso == null) {
                        Piso(
                            id = null,                      // Todavía no tiene ID Firestore
                            nombre = nuevoTitulo,           // Ingresado por el usuario
                            empresaCif = cif,               // Usa la variable que tengas con el CIF actual
                            imagenUrl = null                // O alguna URL por defecto si tienes
                        )
                    } else {
                        // En caso de existir, solo actualiza el nombre
                        piso.copy(nombre = nuevoTitulo)
                    }
                    // Actualiza el título en la toolbar
                    findViewById<TextView>(R.id.toolbar_title).text = nuevoTitulo
                    // Muestra el siguiente mensaje
                    showToast("Nombre del piso modificado (pendiente de guardar)")
                }
            }
            // En asi de seleccionar el botón de "Cancelar no se realizará ninguna acción"
            .setNegativeButton("Cancelar", null)
            .create() // Crea el diálogo

        // Personaliza el aspecto del diálogo
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
        }
        // Muestra el diálogo
        dialog.show()
    }

    // Función para definir un toast y hacer uso de él más adelante
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // Funciónque muestra las opciopnes del boton (Sala)
    private fun showButtonOptions(button: Button) {
        // Lista de opciones a mostrar
        val options = arrayOf("Editar", "Eliminar", "Cambiar tamaño")

        // Crea un constructor para el diálogo y le da título al diálogo
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones de la sala")

        // Defina ecciones dependiendo de la opción seleccionada
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditButtonDialog(button) // Editar texto
                1 -> { // Eliminar sala, coge el objeto asociado a la etiqueta del botón y lo elimina de la memoria
                    val sala = button.tag as? Salas
                    if (sala != null) {
                        salasEnMemoria.remove(sala)
                    }
                    // Eliminae el botón del layout (contenedor)
                    container.removeView(button)
                }
                2 -> { // Cambir tamaño de sala,  coge el objeto asociado a la etiqueta del botón y abre el diálogo de cambio de tamaño
                    val sala = button.tag as? Salas
                    if (sala != null) {
                        mostrarDialogoCambiarTamanio(button, sala)
                    } else {
                        // En caso de no poder obtener la sala, saldrá el siquiente mensaje
                        Toast.makeText(this, "Modifica priemero la sala", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Crea el diálogo con las opciones
        val dialog = builder.create()

        // Cambiar fondo de el dialog
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background) // Aquí se aplica el fondo

        // Personalización del diálogo
        dialog.setOnShowListener {
            // Opcional: personalizar las opciones dentro del diálogo (si lo deseas)
            val listView = dialog.listView
            for (i in 0 until listView.count) {
                val itemView = listView.getChildAt(i)
                itemView?.let {
                    val textView = it.findViewById<TextView>(android.R.id.text1)
                    textView?.setTextColor(Color.BLACK)  // Cambiar el color del texto de las opciones
                }
            }
        }
        // Muestra el diálogo
        dialog.show()
    }

    // Fiunción para mostrar el diálogo de cambio de tamaño
    private fun mostrarDialogoCambiarTamanio(salaButton: Button, sala: Salas) {
        // Crea layout con formaro para contener el diálogo
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)

        }

        // Barra deslizante con apariencia especificada para elgir el ancho de la sala
        val anchoSeekBar = SeekBar(this).apply {
            max = 1070
            progress = salaButton.width // Valor iniacial, ancho actual
            thumbTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        // Guarda el ancho en una variable tipo string
        val anchoValue = TextView(this).apply {
            text = "Ancho: ${anchoSeekBar.progress}px"
        }

        // Barra deslizante con apariencia especificada para elgir el alto de la sala
        val altoSeekBar = SeekBar(this).apply {
            max = 1750
            progress = salaButton.height
            thumbTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        // Guarda el alto en una variable tipo string
        val altoValue = TextView(this).apply {
            text = "Alto: ${altoSeekBar.progress}px"
        }

        // Listener para ver en tiempo real los cambios en el ancho del botón(salsa)
        anchoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                anchoValue.text = "Ancho: ${seekBar?.progress}px"
                val params = salaButton.layoutParams
                params.width = seekBar?.progress ?: salaButton.width
                salaButton.layoutParams = params
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Listener para ver en tiempo real los cambios en el alto del botón(salsa)
        altoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                altoValue.text = "Alto: ${seekBar?.progress}px"
                val params = salaButton.layoutParams
                params.height = seekBar?.progress ?: salaButton.height
                salaButton.layoutParams = params
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Agrega los elementos de barra de selección y los lstener al layout
        layout.addView(anchoValue)
        layout.addView(anchoSeekBar)
        layout.addView(altoValue)
        layout.addView(altoSeekBar)

        // Crea un constructor para el diálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle("Cambiar tamaño de ${sala.nombre}")
            .setView(layout) // Añade layout
            // Botón aceptar con acciones, guarda el nuevo alto y ancho y se aplicaría al botón
            .setPositiveButton("Aplicar") { _, _ ->
                val nuevoAncho = anchoSeekBar.progress
                val nuevoAlto = altoSeekBar.progress
                val params = salaButton.layoutParams
                params.width = nuevoAncho
                params.height = nuevoAlto
                salaButton.layoutParams = params
                actualizarTamanioSalaGuardada(sala.nombre, nuevoAncho, nuevoAlto) // Guarda los valores en memoria
            }
            // Botón de cancelar que anula todos los cambios anteriores
            .setNegativeButton("Cancelar", null)
            .create()// Creación del diálogo

        // Configuración adicional del diálogo
        dialog.setOnShowListener {
            // Calculamos posición del botón
            val location = IntArray(2)
            salaButton.getLocationOnScreen(location)
            val botonY = location[1]
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels

            // Ajusta la posición del diálogo
            val layoutParams = dialog.window?.attributes
            layoutParams?.gravity = if (botonY > screenHeight / 2) Gravity.TOP else Gravity.BOTTOM
            dialog.window?.attributes = layoutParams

            // Personaliza el background del diálogo
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
        }
        // Muestra el diálogo
        dialog.show()
    }

    // Función que actualiza el tamaño de las salas en firestore
    private fun actualizarTamanioSalaGuardada(nombreSala: String, nuevoAncho: Int, nuevoAlto: Int) {
            // Optene el cif y el id del piso actuales
            val empresaCif = pisoActual?.empresaCif ?: return
            val pisoId = pisoActual?.id ?: return

    // En una corrutina reliza las siguientes acciones
        lifecycleScope.launch {
            try {
                // Accede a la colección de salas
                val salasRef = firestore.collection("empresas")
                    .document(empresaCif)
                    .collection("pisos")
                    .document(pisoId)
                    .collection("salas")

                // Buscar sala por nombre
                val salaQuery = salasRef
                    .whereEqualTo("nombre", nombreSala)
                    .get()
                    .await()

                // En caso de que no se encuentre la sala por nombre,
                if (!salaQuery.isEmpty) {
                    // Crea un objeto sala
                    val doc = salaQuery.documents.first()
                    val sala = doc.toObject(Salas::class.java)

                    // Verifica si se ha cambiado el ancho y alto
                    if (sala != null && (sala.ancho != nuevoAncho.toFloat() || sala.alto != nuevoAlto.toFloat())) {
                        // Crea una copis de una sala con el tamaño cambiado
                        val salaActualizada = sala.copy(
                            ancho = nuevoAncho.toFloat(),
                            alto = nuevoAlto.toFloat()
                        )

                        // Actualizar el documento en Firestore
                        salasRef.document(doc.id).set(salaActualizada).await()
                    }
                }
            // Crea una excepción en caso de error, mostrará el siguiente mensaje
            } catch (e: Exception) {
                Log.e("Firestore", "Error al actualizar tamaño de sala: ${e.message}", e)
            }
        }
    }

    // Función que muestra diálogo para editar popiedades de una sala
    private fun showEditButtonDialog(button: Button) {
        // Referencia la sala seleccionada al tag
        val sala = button.tag as? Salas
        // En caso de no encontrar la sela saldrá el siguiente mensaje
        if (sala == null) {
            Toast.makeText(this, "No se encontró la sala asociada al botón", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar que haya un piso actual con CIF
       val empresaCif = pisoActual?.empresaCif ?: run {
            Toast.makeText(this, "Primero asigna un nombre al piso antes de editar salas", Toast.LENGTH_LONG).show()
            return
        }
        // Verificar que el piso tenga un nombre
        val pisoId = pisoActual?.nombre ?: run {
            Toast.makeText(this, "No se encontró el piso asociado", Toast.LENGTH_SHORT).show()
            return
        }

        // Creación del layout para el diálogo
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        // Editext con formato para cambiar el nombre de la sala (max 20 caracteres)
        val editTextNombre = EditText(this).apply {
            hint = "Nuevo nombre"
            setText(sala.nombre)
            filters = arrayOf(InputFilter.LengthFilter(20))
        }

        // TextView para mostrar el número de caracteres introducidos
        val charCountTextView = TextView(this).apply {
            text = "${sala.nombre.length}/20"
            setTextColor("#000000".toColorInt())
        }

        // Listener que muestra los cambios según se escriben
        editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                charCountTextView.text = "${s?.length ?: 0}/20"
            }
        })

        // Definición de lista con tamaños disponibles
        val tamanios = listOf("Pequeño", "Grande")
        // Pinner con aspecto perconalizado para seleccionar el tamaño deseado
        val spinnerTamanio = Spinner(this).apply {
            adapter = ArrayAdapter(this@Activity_creacion, android.R.layout.simple_spinner_dropdown_item, tamanios)
            setSelection(tamanios.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
            background = ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16; bottomMargin = 16 }
        }

        // Checkbox para  extras disponibles (WiFI)
        val checkWifi = CheckBox(this).apply {
            text = "WiFi"
            isChecked = sala.extras.contains("WiFi")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        // Checkbox para  extras disponibles (Proyector)
        val checkProyector = CheckBox(this).apply {
            text = "Proyector"
            isChecked = sala.extras.contains("Proyector")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        // Checkbox para  extras disponibles (Pizarra)
        val checkPizarra = CheckBox(this).apply {
            text = "Pizarra"
            isChecked = sala.extras.contains("Pizarra")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }

        // Agregan todos los elemantos anteriores al layout
        layout.apply {
            addView(editTextNombre)
            addView(charCountTextView)
            addView(spinnerTamanio)
            addView(checkWifi)
            addView(checkProyector)
            addView(checkPizarra)
        }

        // Creación del builder para el diálogo, con título y un botón de confirmación y negación
        val builder = AlertDialog.Builder(this)
            .setTitle("Editar sala")
            .setView(layout)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)

        // Creación del diálogo
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Configuración al mostrar el dialogo
        dialog.setOnShowListener {
            // Personalización de aspecto
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)

            // Acción al clickar el botón de afirmación
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val nuevoNombre = editTextNombre.text.toString().trim()

                // Valida que el nombre no esté vacío
                if (nuevoNombre.isEmpty()) {
                    editTextNombre.error = "El nombre no puede estar vacío"
                    return@setOnClickListener
                }

                // Comprueba si ya existe una sala con el mismo nombre
                val nombreRepetido = container.children
                    .filterIsInstance<Button>()
                    .filter { it != button }
                    .mapNotNull { (it.tag as? Salas)?.nombre }
                    .any { it.equals(nuevoNombre, ignoreCase = true) }

                // En caso de que exista, saldrá el siguiente mensaje
                if (nombreRepetido) {
                    editTextNombre.error = "Ese nombre ya está en uso"
                    return@setOnClickListener
                }

                // Crear la sala editada (nueva instancia)
                val salaEditada = sala.copy(
                    nombre = nuevoNombre,
                    id = nuevoNombre,
                    tamaño = spinnerTamanio.selectedItem?.toString().orEmpty(),
                    extras = mutableListOf<String>().apply {
                        if (checkWifi.isChecked) add("WiFi")
                        if (checkProyector.isChecked) add("Proyector")
                        if (checkPizarra.isChecked) add("Pizarra")
                    }
                )

                // Usa una corrutina para relizar las siguinetes acciones
                lifecycleScope.launch {
                    try {
                        // Solo actualizar lista en memoria
                        val index = salasEnMemoria.indexOfFirst { it.id == salaEditada.id }
                        if (index >= 0) {
                            salasEnMemoria[index] = salaEditada
                        }

                        // Actualizar el botón visualmente y su etiqueta
                        actualizarBotonConSala(button, salaEditada)
                        button.tag = salaEditada
                        // Mensaje de confirmación
                        Toast.makeText(this@Activity_creacion, "Sala actualizada en memoria", Toast.LENGTH_SHORT).show()
                        dialog.dismiss() // Cierra el diálogo
                    // Crea una excepción en caso de que haya algún error, mostrará el siguiente mensaje
                    } catch (e: Exception) {
                        Toast.makeText(this@Activity_creacion, "Error al actualizar sala en memoria", Toast.LENGTH_SHORT).show()
                        Log.e("Memoria", "Error: ${e.message}", e)
                    }
                }
            }
        }
        // Muestra el diálogo
        dialog.show()
    }

    // Función para actualizar el botón de una sala según la información que esta tenga
    private fun actualizarBotonConSala(button: Button, sala: Salas) {
        // Constructor para el texto de la sala con el nombre de esta
        val builder = StringBuilder()
        builder.append(sala.nombre)

        // Si la sala tiene condiene extras
        if (sala.extras.isNotEmpty()) {
            builder.append("\n") // Mete Salto de línea
            sala.extras.forEach { extra ->
                // Dependiendo de cada tipo de extra que tenga la sala, se le añade el icono correspondiente
                when (extra) {
                    "WiFi" -> builder.append("📶 ")
                    "Proyector" -> builder.append("📽️ ")
                    "Pizarra" -> builder.append("🖍️ ")
                }
            }
        }
        // Se asigna el constructor al texto del botón
        button.text = builder.toString()
    }

    // Función para guardar la distribución de las salas en firestore con la imagen de fondo
    suspend fun guardarDistribucion(
        pisoNombre: String,
        imagen: ByteArray?,
        container: ViewGroup, // Contenedor de las salas posicionadas
        sesion: UsuarioSesion? // Datos de sesión del usuario actual
    ) {
        // INstanciación de firebase
        val db = FirebaseFirestore.getInstance()

        //  Se recorre la lista de elemenotos del contenedor hijo
        val salasGuardadas = (0 until container.childCount).mapNotNull { i ->
            val view = container.getChildAt(i)
            // Filtra los botones por etiqueta de tipo sala
            val sala = (view as? Button)?.tag as? Salas ?: return@mapNotNull null
            // Copia las coordenadas y dimensiones de cada sala
            sala.copy(
                x = view.x,
                y = view.y,
                ancho = view.width.toFloat(),
                alto = view.height.toFloat(),
                id = sala.nombre
            )
        }

        //  En caso de no haber salas guardadas o el piso esté vacío, en el hilo principal mostrará el siguiente mensaje
        if (salasGuardadas.isEmpty() || pisoNombre.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(container.context, "Debes colocar una sala y asignar nombre al piso", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Comprueba si el nombre de la empresa se puede obtener de la sesión acrual, en caso de no poder, mostrará el siguiente mensaje
        val empresaId = sesion?.empresa?.nombre
        if (empresaId.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(container.context, "Empresa en sesión no definida", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Accede al nombre de la empresa en firebase
        val empresaDoc = db.collection("empresas").document(empresaId).get().await()
        // En caso de que la empresa no exista, en el hilo principal aparecerá el siguiente mensaje
        if (!empresaDoc.exists()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(container.context, "Empresa no encontrada", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Subir la imagen a Firebase Storage y obtener URL
        val urlImagen = imagen?.let {
            subirImagenAFirebaseStorage(it, empresaId, pisoNombre)
        }

        // Se obtiene el nombre del piso
        val pisoRef = db.collection("empresas")
            .document(empresaId)
            .collection("pisos")
            .document(pisoNombre)

        // Se usa un mapa para guardar los datos del piso
        val pisoData = mutableMapOf(
            "nombre" to pisoNombre,
            "nombreEmpresa" to empresaId
        )
        // Guardar URL de la imagen en Firestore solo si no es nula
        if (urlImagen != null) {
            pisoData["imagenUrl"] = urlImagen
        }

        // Se guarda el piso en firestore
        pisoRef.set(pisoData).await()

        // Accede a la colección de las salas y se guarda cada sala
        val salasCollection = pisoRef.collection("salas")
        for (sala in salasGuardadas) {
            salasCollection.document(sala.nombre).set(sala).await()
        }

        // En el hilo principal aparecerá el siguiente mensaje de afirmación
        withContext(Dispatchers.Main) {
            Toast.makeText(container.context, "Distribución guardada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para guardar la imagen en firebase, usa una array de bytes(imagen), el nombre de la empresa y el nombre del piso, devuelve la URL
    suspend fun subirImagenAFirebaseStorage(
        imagenBytes: ByteArray,
        empresaId: String,
        pisoNombre: String
    ): String? {
        // Indica el lugar de elamcenamiento de la imagen
        val storageRef = storage.reference
            .child("empresas")
            .child(empresaId)
            .child("pisos")
            .child("$pisoNombre.png")

        return try {
            // Guarde la imagen como bytes al firestore storage
            storageRef.putBytes(imagenBytes).await()
            // Obtiene la URL como String
            storageRef.downloadUrl.await().toString()
        // Creación de excepciones en caso de que haya algún problema, mostrará el siguiente mensaje
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Error subiendo imagen: ${e.message}")
            null
        }
    }

    // Función para eliminar pisos por su nombre
    private fun eliminarPisoPorNombre(nombrePiso: String, nombreEmpresa: String) {
        // Acceso a firebase
        val db = Firebase.firestore

        // Acciones a realizar en una corrutina
        lifecycleScope.launch {
            try {
                // Obtiene el nombre de la emoresa(ID) y el nombre de los pisos(ID)
                val empresaDoc = db.collection("empresas").document(nombreEmpresa)
                val pisoRef = empresaDoc.collection("pisos").document(nombrePiso)

                // 1. Eliminar todas las salas del piso
                val salasSnapshot = pisoRef.collection("salas").get().await()
                for (doc in salasSnapshot.documents) {
                    doc.reference.delete().await()
                }

                // 2. Eliminar el piso
                pisoRef.delete().await()

                // 3. Verifica que el piso eliminado corresponde con el de SharedPreferences
                val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
                val pisoActual = prefNumeroPiso.getString("numero_piso", null)

                // En caso de que sea el piso actual
                if (pisoActual == nombrePiso) {
                    // Obtiene la lista de pisos restantes de la empresa
                    val pisosRestantes = empresaDoc.collection("pisos").get().await().documents
                    val nombresPisos = pisosRestantes.mapNotNull { it.getString("nombre") }

                    // Actulaización o eliminación de las SharedPreferences
                    prefNumeroPiso.edit().apply {
                        // Si quedan pisos pasa al siguiente
                        if (nombresPisos.isNotEmpty()) {
                            putString("numero_piso", nombresPisos.first())
                        } else {
                            // No quedan pisos
                            remove("numero_piso")
                        }
                        apply() // Aplican los cambios
                    }
                }
                // Muestra mensaje de confirmación en el hilo principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Piso eliminado correctamente", Toast.LENGTH_SHORT).show()
                    mostrarDialogoEliminarPisos(nombreEmpresa)
                }

            // Crea una excepción en caso de que haya algún priblema al realizar las acciones, muestra el siguiente mensaje en el hilo principal
            } catch (e: Exception) {
                Log.e("Firestore", "Error al eliminar piso: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Error eliminando el piso", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Función para mostrar el diálogo de eliminación de pisos
    private fun mostrarDialogoEliminarPisos(nombreEmpresa: String) {
        // Acceso a la colección de pisos de firestore
        val db = Firebase.firestore
        val empresaDoc = db.collection("empresas").document(nombreEmpresa)
        val pisosCollection = empresaDoc.collection("pisos")

        // Acciones que se realizan en una corrutina
        lifecycleScope.launch {
            try {
                // Obtine la lista de pisos de firestore por nombre
                val snapshot = pisosCollection.get().await()
                val nombresPisos = snapshot.documents.mapNotNull { it.getString("nombre") }.toMutableList()

                // En caso de que no se encuentren pisos, apaerecerá el siguiente mensaje en el hilo principal
                if (nombresPisos.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_creacion, "No hay pisos guardados.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Ordena los pisos
                nombresPisos.sortWith { p1, p2 ->
                    val k1 = naturalOrderKey(p1)
                    val k2 = naturalOrderKey(p2)
                    compareNaturalKeys(k1, k2)
                }

                // Acción que se realiza en el hilo principal
                withContext(Dispatchers.Main) {
                    // Guarda los pisos orgenados
                    val pisosArray = nombresPisos.toTypedArray()

                    // Creación de diálogo para eliminar pisos
                    val dialogPrincipal = AlertDialog.Builder(this@Activity_creacion)
                        .setTitle("Eliminar piso")
                        .setItems(pisosArray) { dialogInterface, which ->
                            val pisoSeleccionado = pisosArray[which]

                            // Creación del diálogo de confirmación con botón de eliminar que borrará el piso seleccionado
                            val dialogConfirmacion = AlertDialog.Builder(this@Activity_creacion)
                                .setTitle("¿Eliminar '$pisoSeleccionado'?")
                                .setMessage("Esta acción eliminará el piso y todas sus salas.")
                                .setPositiveButton("Eliminar") { dialogConfirm, _ ->
                                    eliminarPisoPorNombre(pisoSeleccionado, nombreEmpresa)
                                    // Cierra diálogos
                                    dialogConfirm.dismiss()
                                    dialogInterface.dismiss()
                                } // Botón de negecvión, cancela las acciones a realizar
                                .setNegativeButton("Cancelar", null)
                                .create() // crea diálogo

                            // Personalización de la apariencia de los diálogos
                            dialogConfirmacion.setOnShowListener {
                                dialogConfirmacion.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                dialogConfirmacion.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                                dialogConfirmacion.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                            }
                            // Muestra el diálogo de confirmación
                            dialogConfirmacion.show()
                        }
                        //En caso de darle a cancelar, no se borrarán los pisos y cancelará
                        .setNegativeButton("Cancelar", null)
                        .create() // Crea diiálogo

                    // Personalización de aspecto de piálogo principal
                    dialogPrincipal.setOnShowListener {
                        dialogPrincipal.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                        dialogPrincipal.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                    }
                    // Muesra diálogo principal
                    dialogPrincipal.show()
                }

            // Crea excepción en caso de que haya algún error, muestra el siguiente mensaje en el hilo principal
            } catch (e: Exception) {
                Log.e("Firestore", "Error al obtener pisos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Error obteniendo pisos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Lanzador de actividad para obtener la imagen de la galaría
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Carga la imagen como array de bytes
            imagen = contentResolver.openInputStream(uri)?.use { it.readBytes() } // Esto está bien

            // Carga la imagen de forma visual con glide
            Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(findViewById(R.id.image_fondo))
        }
    }

    // Extensión para que cuando se compele campo de un editText pase al siguiente
    private fun EditText.autoAdvanceTo(next: EditText?) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 2) {
                    if (next != null) {
                        next.requestFocus()
                    } else {
                        // Ocultar teclado si es el último campo
                        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)
                        clearFocus()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Clase para arraster botones (salas) por la pantalla
    inner class MovableTouchListener : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var startX = 0f
        private var startY = 0f
        private val CLICK_THRESHOLD = 10  // Distancia máxima para considerar click

        // comprueba dónde se ha pulsado en el botón
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                }
                // Mueve el botón a medida que se desplaza el dedo
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.rawX
                    val endY = event.rawY

                    val deltaX = Math.abs(endX - startX)
                    val deltaY = Math.abs(endY - startY)

                    // Si el movimiento fue muy ligero
                    if (deltaX < CLICK_THRESHOLD && deltaY < CLICK_THRESHOLD) {
                        // Se considera click
                        if (view is Button) {
                            showButtonOptions(view)
                        }
                    }
                }
            }
            return true
        }
    }
}

