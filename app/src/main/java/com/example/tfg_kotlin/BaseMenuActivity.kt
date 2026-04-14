package com.example.tfg_kotlin

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

abstract class BaseMenuActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseMenuActivity"
        const val EXTRA_EDIT_RESERVA_ID = "edit_reserva_id"
        const val EXTRA_EDIT_DATE = "edit_date"
        const val EXTRA_EDIT_ROOM_ID = "edit_room_id"
        const val EXTRA_EDIT_START_TIME = "edit_start_time"
        const val EXTRA_EDIT_END_TIME = "edit_end_time"
        const val EXTRA_EDIT_PISO_NAME = "edit_piso_name"
        const val EXTRA_EDIT_SALA_NOMBRE = "edit_sala_nombre"
        const val EXTRA_EDIT_USER_NOMBRE = "edit_user_nombre"
        const val EXTRA_EDIT_USER_ID = "edit_user_id"
        const val EXTRA_EDIT_TIPO = "edit_tipo"
    }

    protected val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    protected val firestoreRepo: FirestoreRepository by lazy { FirestoreRepository(firestore) }

    abstract val menuViewModel: MenuViewModel

    override fun onResume() {
        super.onResume()
        menuViewModel.loadNextReserva()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val anchorView = findViewById<View>(item.itemId)
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_notifications -> {
                mostrarDropdownNotificaciones(anchorView)
                true
            }
            R.id.action_theme -> {
                mostrarMenuTema(anchorView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarMenuTema(anchor: View?) {
        val view = layoutInflater.inflate(R.layout.dropdown_tema, findViewById(android.R.id.content), false)
        val rg = view.findViewById<RadioGroup>(R.id.radioGroupTema)

        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
        val currentTheme = prefs.getInt("tema_app", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> rg.check(R.id.radioClaro)
            AppCompatDelegate.MODE_NIGHT_YES -> rg.check(R.id.radioOscuro)
            else -> rg.check(R.id.radioSistema)
        }

        val popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 30f
            showAsDropDown(anchor ?: return, 0, 0)
        }

        rg.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radioClaro -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioOscuro -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.edit { putInt("tema_app", newTheme) }
            AppCompatDelegate.setDefaultNightMode(newTheme)
            popupWindow.dismiss()
        }
    }

    private fun mostrarDropdownNotificaciones(anchor: View?) {
        val view = layoutInflater.inflate(R.layout.dropdown_notificaciones, findViewById(android.R.id.content), false)
        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)

        // Salas
        val swSalas = view.findViewById<MaterialSwitch>(R.id.switchNotifSalas)
        val pkHSalas = view.findViewById<NumberPicker>(R.id.pickerHorasSalas).apply { minValue = 0; maxValue = 23 }
        val pkMSalas = view.findViewById<NumberPicker>(R.id.pickerMinutosSalas).apply { minValue = 0; maxValue = 59 }

        // Puestos
        val swPuestos = view.findViewById<MaterialSwitch>(R.id.switchNotifPuestos)
        val pkHPuestos = view.findViewById<NumberPicker>(R.id.pickerHorasPuestos).apply { minValue = 0; maxValue = 23 }
        val pkMPuestos = view.findViewById<NumberPicker>(R.id.pickerMinutosPuestos).apply { minValue = 0; maxValue = 59 }

        // Load values
        swSalas.isChecked = prefs.getBoolean("notif_salas_activadas", true)
        pkHSalas.value = prefs.getInt("notif_salas_horas", 0)
        pkMSalas.value = prefs.getInt("notif_salas_mins", 10)

        swPuestos.isChecked = prefs.getBoolean("notif_puestos_activadas", true)
        pkHPuestos.value = prefs.getInt("notif_puestos_horas", 0)
        pkMPuestos.value = prefs.getInt("notif_puestos_mins", 10)

        val popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 30f
            showAsDropDown(anchor ?: return, 0, 0)
        }

        popupWindow.setOnDismissListener {
            prefs.edit {
                putBoolean("notif_salas_activadas", swSalas.isChecked)
                putInt("notif_salas_horas", pkHSalas.value)
                putInt("notif_salas_mins", pkMSalas.value)
                
                putBoolean("notif_puestos_activadas", swPuestos.isChecked)
                putInt("notif_puestos_horas", pkHPuestos.value)
                putInt("notif_puestos_mins", pkMPuestos.value)
            }
        }
    }

    private fun formatearFechaHora(fechaHora: String): String {
        return fechaHora.replace(" (Puestos de trabajo)", "")
    }

    protected fun setupBaseObservers(
        textSala: TextView?, 
        cardSala: View?, 
        textPuesto: TextView?, 
        cardPuesto: View?,
        cardContenedor: View? = null
    ) {
        menuViewModel.nextSalaReserva.observe(this) { reserva ->
            val hasSala = reserva != null
            val hasPuesto = menuViewModel.nextPuestoReserva.value != null
            
            cardContenedor?.visibility = if (hasSala || hasPuesto) View.VISIBLE else View.GONE
            val divider1 = cardContenedor?.findViewById<View>(R.id.dividerAlertas1)
            divider1?.visibility = if (hasSala && hasPuesto) View.VISIBLE else View.GONE

            if (reserva != null) {
                cardSala?.visibility = View.VISIBLE
                val fhFormateada = formatearFechaHora(reserva.fechaHora)
                val originalText = getString(R.string.msg_proxima_sala, reserva.nombreSala, fhFormateada)
                val suffix = getString(R.string.label_inexistente)
                textSala?.text = if (reserva.lugarEliminado) "$originalText $suffix" else originalText
                
                if (reserva.lugarEliminado) {
                    textSala?.paintFlags = textSala!!.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textSala?.setTextColor(Color.RED)
                } else {
                    textSala?.paintFlags = textSala!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textSala?.setTextColor(ContextCompat.getColor(this, R.color.aura_on_surface))
                }

                cardSala?.setOnClickListener {
                    if (reserva.lugarEliminado) {
                        mostrarConfirmacionEliminarReservaEliminada(reserva)
                    } else {
                        mostrarMenuSala(reserva)
                    }
                }
            } else {
                cardSala?.visibility = View.GONE
            }
        }

        menuViewModel.nextPuestoReserva.observe(this) { reserva ->
            val hasPuesto = reserva != null
            val hasSala = menuViewModel.nextSalaReserva.value != null

            cardContenedor?.visibility = if (hasSala || hasPuesto) View.VISIBLE else View.GONE
            val divider1 = cardContenedor?.findViewById<View>(R.id.dividerAlertas1)
            divider1?.visibility = if (hasSala && hasPuesto) View.VISIBLE else View.GONE

            if (reserva != null) {
                cardPuesto?.visibility = View.VISIBLE
                val fhFormateada = formatearFechaHora(reserva.fechaHora)
                val originalText = getString(R.string.msg_proximo_puesto, reserva.nombreSala, fhFormateada)
                val suffix = getString(R.string.label_inexistente)
                textPuesto?.text = if (reserva.lugarEliminado) "$originalText $suffix" else originalText

                if (reserva.lugarEliminado) {
                    textPuesto?.paintFlags = textPuesto!!.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textPuesto?.setTextColor(Color.RED)
                } else {
                    textPuesto?.paintFlags = textPuesto!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textPuesto?.setTextColor(ContextCompat.getColor(this, R.color.aura_on_surface))
                }

                cardPuesto?.setOnClickListener {
                    if (reserva.lugarEliminado) {
                        mostrarConfirmacionEliminarReservaEliminada(reserva)
                    } else {
                        mostrarDialogoCancelarReservaSimple(reserva)
                    }
                }
            } else {
                cardPuesto?.visibility = View.GONE
            }
        }
    }

    private fun mostrarMenuSala(reserva: Reserva) {
        val options = arrayOf(getString(R.string.opt_editar), getString(R.string.opt_eliminar))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_opciones_reserva)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editarReservaSala(reserva)
                    1 -> mostrarDialogoCancelarReservaSimple(reserva)
                }
            }
            .show()
    }

    private fun editarReservaSala(reserva: Reserva) {
        val text = reserva.fechaHora.trim()
        val firstSpace = text.indexOf(' ')
        if (firstSpace == -1) return
        
        val fecha = text.substring(0, firstSpace)
        val rest = text.substring(firstSpace + 1)
        val times = rest.split("-")
        if (times.size != 2) return
        
        val startStr = times[0].trim()
        val endStr = times[1].trim()

        val intent = android.content.Intent(this, EmpleadosActivity::class.java).apply {
            putExtra(EXTRA_EDIT_RESERVA_ID, reserva.id)
            putExtra(EXTRA_EDIT_DATE, fecha)
            putExtra(EXTRA_EDIT_ROOM_ID, reserva.idSala)
            putExtra(EXTRA_EDIT_START_TIME, startStr)
            putExtra(EXTRA_EDIT_END_TIME, endStr)
            putExtra(EXTRA_EDIT_PISO_NAME, reserva.piso)
            putExtra(EXTRA_EDIT_SALA_NOMBRE, reserva.nombreSala)
            putExtra(EXTRA_EDIT_USER_NOMBRE, reserva.nombreUsuario)
            putExtra(EXTRA_EDIT_USER_ID, reserva.idUsuario)
            putExtra(EXTRA_EDIT_TIPO, reserva.tipo)
        }
        startActivity(intent)
    }



    private fun mostrarDialogoCancelarReservaSimple(reserva: Reserva) {
        val fhFormateada = formatearFechaHora(reserva.fechaHora)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_cancelar_reserva)
            .setMessage(getString(R.string.msg_confirmar_cancelar_reserva, reserva.nombreSala, fhFormateada))
            .setPositiveButton(R.string.btn_si) { _, _ ->
                menuViewModel.cancelReserva(reserva.id ?: "")
            }
            .setNegativeButton(R.string.btn_no, null)
            .show()
    }

    private fun mostrarConfirmacionEliminarReservaEliminada(reserva: Reserva) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_limpiar_registro)
            .setMessage(R.string.msg_reserva_lugar_eliminado_pregunta)
            .setPositiveButton(R.string.opt_eliminar) { _, _ ->
                menuViewModel.cancelReserva(reserva.id ?: "")
            }
            .setNegativeButton(R.string.btn_no, null)
            .show()
    }




    protected fun mostrarDialogoReservas() {
        val view = layoutInflater.inflate(R.layout.dialog_reservas, findViewById(android.R.id.content), false)
        val layoutReservas = view.findViewById<LinearLayout>(R.id.contenedor_reservas)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_tus_reservas)
            .setView(view)
            .setPositiveButton(R.string.btn_cerrar, null)
            .create()

        val observer = androidx.lifecycle.Observer<List<Reserva>> { reservas ->
            layoutReservas.removeAllViews()
            
            if (reservas.isEmpty()) {
                layoutReservas.addView(TextView(this).apply {
                    text = getString(R.string.msg_no_reservas_activas)
                    setPadding(16, 64, 16, 64)
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)
                })
                // Opcional: Cerrar diálogo si se vacía por completo (después de una pequeña pausa o directamente)
                // dialog.dismiss() 
                return@Observer
            }

            val types = listOf(TipoElemento.SALA, TipoElemento.PUESTO)
            types.forEach { type ->
                val resInType = reservas.filter { it.tipo == type.valor }
                if (resInType.isNotEmpty()) {
                    layoutReservas.addView(TextView(this).apply {
                        text = if (type == TipoElemento.SALA) getString(R.string.label_salas_reunion) else getString(R.string.label_puestos_trabajo)
                        textSize = 20f
                        setPadding(0, 32, 0, 8)
                        setTextColor(ContextCompat.getColor(context, R.color.enlace))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    })

                    resInType.groupBy { it.piso }.forEach { (piso, list) ->
                        layoutReservas.addView(TextView(this).apply {
                            text = getString(R.string.label_planta_formato, piso)
                            textSize = 16f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setPadding(16, 16, 0, 8)
                        })

                        list.forEach { res ->
                            layoutReservas.addView(TextView(this).apply {
                                val fhFormateada = formatearFechaHora(res.fechaHora)
                                val originalText = getString(R.string.reserva_item_format, res.nombreSala, fhFormateada)
                                val suffix = getString(R.string.label_sitio_inexistente)
                                text = if (res.lugarEliminado) "$originalText $suffix" else originalText
                                
                                // Estilo de ítem clicable
                                setPadding(32, 24, 32, 24)
                                val outValue = TypedValue()
                                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                                setBackgroundResource(outValue.resourceId)
                                isClickable = true
                                isFocusable = true

                                if (res.lugarEliminado) {
                                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                                    setTextColor(Color.RED)
                                } else {
                                    setTextColor(ContextCompat.getColor(context, R.color.aura_on_surface))
                                }

                                setOnClickListener {
                                    if (res.lugarEliminado) {
                                        mostrarConfirmacionEliminarReservaEliminada(res)
                                    } else {
                                        mostrarMenuSala(res)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }

        menuViewModel.userReservas.observe(this, observer)
        dialog.setOnDismissListener { menuViewModel.userReservas.removeObserver(observer) }
        
        menuViewModel.loadNextReserva()
        dialog.show()
    }

    protected fun cargarFranjas() {
        startActivity(Intent(this, GestionHorariosActivity::class.java))
    }

    protected fun handleNuevaReserva() {
        lifecycleScope.launch {
            try {
                val sesion = Sesion.datos ?: return@launch
                val empresaId = sesion.empresa.nombre

                if (empresaId.isEmpty()) {
                    Toast.makeText(this@BaseMenuActivity, getString(R.string.err_empresa_no_definida), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val empresaActualizada = firestoreRepo.getEmpresaByNombre(empresaId)
                if (empresaActualizada == null) {
                    Toast.makeText(this@BaseMenuActivity, getString(R.string.err_empresa_no_encontrada), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                sesion.empresa = empresaActualizada

                val pisos = firestoreRepo.getPisosByEmpresa(empresaId)
                if (pisos.isNotEmpty()) {
                    sesion.pisos = listOf(pisos.last())
                    startActivity(Intent(this@BaseMenuActivity, EmpleadosActivity::class.java))
                } else {
                    Toast.makeText(this@BaseMenuActivity, getString(R.string.msg_no_piso_creado), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pisos for new reservation", e)
                Toast.makeText(this@BaseMenuActivity, getString(R.string.err_cargar_pisos), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
