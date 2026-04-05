package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.databinding.ActivityEmpleadosBinding
import com.example.tfg_kotlin.ui.viewmodel.EmpleadosViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class EmpleadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadosBinding
    private val viewModel: EmpleadosViewModel by viewModels()

    private var snackbarActivo: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.empleados) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()

        viewModel.loadPisos()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_adagora)
        }
    }

    private fun setupObservers() {
        viewModel.pisos.observe(this) { pisos ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pisos.map { it.nombre })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPisos.adapter = adapter
        }

        viewModel.salas.observe(this) { salas ->
            actualizarVistaSalas(salas)
        }

        viewModel.reservas.observe(this) {
            actualizarColoresSalas()
        }

        viewModel.fechaSeleccionada.observe(this) { fecha ->
            binding.btnFecha.text = if (fecha.isEmpty()) getString(R.string.btn_seleccionar_fecha) else getString(R.string.label_fecha_con_valor, fecha)
        }

        viewModel.horaSeleccionada.observe(this) { hora ->
            binding.btnHora.text = if (hora.isEmpty()) getString(R.string.btn_cambiar_hora) else getString(R.string.label_hora_con_valor, hora)
        }

        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        binding.spinnerPisos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val piso = viewModel.pisos.value?.get(pos)
                piso?.let {
                    viewModel.loadSalas(it.id ?: "")
                    val url = it.imagenUrl
                    if (!url.isNullOrEmpty()) {
                        cargarImagenFondo(url)
                    } else {
                        binding.contentLayout.background = null
                    }
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.btnFecha.setOnClickListener { mostrarDialogoFecha() }
        binding.btnHora.setOnClickListener { mostrarDialogoHoras() }
    }

    private fun actualizarVistaSalas(salas: List<Sala>) {
        binding.contentLayout.removeAllViews()
        salas.forEach { sala ->
            val button = crearBotonSala(sala)
            binding.contentLayout.addView(button)
        }
        actualizarColoresSalas()
    }

    private fun crearBotonSala(sala: Sala): Button {
        return Button(this).apply {
            text = formatearTextoSala(sala)
            tag = sala
            
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(2, Color.GRAY)
                cornerRadius = 50f
            }
            
            layoutParams = ConstraintLayout.LayoutParams(sala.ancho.toInt(), sala.alto.toInt()).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = sala.y.toInt()
                leftMargin = sala.x.toInt()
            }
            
            setOnClickListener {
                if (viewModel.fechaSeleccionada.value.isNullOrEmpty() || viewModel.horaSeleccionada.value.isNullOrEmpty()) {
                    mostrarSnackbarFecha()
                } else {
                    mostrarDialogoDetallesSala(sala)
                }
            }
        }
    }

    private fun actualizarColoresSalas() {
        val reservas = viewModel.reservas.value ?: return
        val uid = Sesion.datos?.usuario?.uid ?: ""
        
        for (i in 0 until binding.contentLayout.childCount) {
            val view = binding.contentLayout.getChildAt(i)
            if (view is Button && view.tag is Sala) {
                val sala = view.tag as Sala
                val reserva = reservas.find { it.idSala == sala.id }
                
                val colorId = when {
                    reserva == null -> android.R.color.holo_green_light
                    reserva.idusuario == uid -> android.R.color.holo_orange_light
                    else -> android.R.color.holo_red_light
                }
                view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorId))
            }
        }
    }

    private fun mostrarSnackbarFecha() {
        snackbarActivo?.dismiss()
        snackbarActivo = Snackbar.make(binding.contentLayout, getString(R.string.msg_selecciona_fecha_primero), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.btn_seleccionar_fecha)) { mostrarDialogoFecha() }
        snackbarActivo?.show()
    }

    private fun mostrarDialogoFecha() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, android.R.style.Theme_Material_Dialog_MinWidth, 
            { _, y, m, d ->
                val f = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)
                viewModel.updateFecha(f)
                mostrarDialogoHoras()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = cal.timeInMillis
            show()
        }
    }

    private fun mostrarDialogoHoras() {
        val franjas = viewModel.franjas.value ?: emptyList()
        if (franjas.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_franjas_disponibles), Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_selecciona_franja_para, viewModel.fechaSeleccionada.value))
            .setItems(franjas.toTypedArray()) { _, which ->
                viewModel.updateHora(franjas[which])
            }
            .show()
    }

    private fun cargarImagenFondo(url: String) {
        Glide.with(this).load(url).centerCrop().into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                binding.contentLayout.background = resource
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun formatearTextoSala(sala: Sala): String {
        val b = StringBuilder(sala.nombre)
        if (sala.extras.isNotEmpty()) {
            b.append("\n")
            sala.extras.forEach { e ->
                b.append(when(e) { "WiFi" -> "📶 "; "Proyector" -> "📽️ "; "Pizarra" -> "🖍️ "; else -> "" })
            }
        }
        return b.toString()
    }

    private fun mostrarDialogoDetallesSala(sala: Sala) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombreSalaDetalle)
        val tvEstado = dialogView.findViewById<TextView>(R.id.tvEstadoSalaDetalle)
        val tvEquipamiento = dialogView.findViewById<TextView>(R.id.tvEquipamientoDetalle)
        val btnAccion = dialogView.findViewById<Button>(R.id.btnAccionReserva)

        tvNombre.text = sala.nombre
        tvEquipamiento.text = if (sala.extras.isEmpty()) getString(R.string.label_ninguno) else sala.extras.joinToString(", ")
        
        val reservaActual = viewModel.reservas.value?.find { it.idSala == sala.id }
        val uid = Sesion.datos?.usuario?.uid ?: ""

        if (reservaActual == null) {
            tvEstado.text = getString(R.string.label_libre)
            tvEstado.setTextColor(Color.GREEN)
            btnAccion.text = getString(R.string.btn_reservar_mayus)
            btnAccion.setOnClickListener {
                viewModel.reservarSala(sala, viewModel.pisos.value?.find { p -> p.id == sala.idPiso }?.nombre ?: "")
            }
        } else {
            tvEstado.text = getString(R.string.label_ocupada_por, reservaActual.nombreUsuario)
            tvEstado.setTextColor(Color.RED)
            if (reservaActual.idusuario == uid) {
                btnAccion.text = getString(R.string.btn_cancelar_mi_reserva)
                btnAccion.setOnClickListener {
                    viewModel.cancelReserva(reservaActual.id ?: "")
                }
            } else {
                btnAccion.visibility = View.GONE
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        viewModel.reservaStatus.observe(this) { success ->
            if (success) {
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.msg_operacion_exito), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}