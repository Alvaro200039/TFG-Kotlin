package com.example.tfg_kotlin

import android.graphics.Color
import android.graphics.Paint
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
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseMenuActivity : AppCompatActivity() {

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
            Toast.makeText(this, getString(R.string.msg_notif_actualizadas), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatearFechaHora(fechaHora: String): String {
        return fechaHora.replace(" (Puestos de trabajo)", "")
    }

    protected fun setupBaseObservers(textSala: TextView?, cardSala: View?, textPuesto: TextView?, cardPuesto: View?) {
        menuViewModel.nextSalaReserva.observe(this) { reserva ->
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
            } else {
                cardSala?.visibility = View.GONE
            }
        }

        menuViewModel.nextPuestoReserva.observe(this) { reserva ->
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
            } else {
                cardPuesto?.visibility = View.GONE
            }
        }
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

            val types = listOf("SALA", "PUESTO")
            types.forEach { type ->
                val resInType = reservas.filter { it.tipo == type }
                if (resInType.isNotEmpty()) {
                    layoutReservas.addView(TextView(this).apply {
                        text = if (type == "SALA") getString(R.string.label_salas_reunion) else getString(R.string.label_puestos_trabajo)
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
                                
                                if (res.lugarEliminado) {
                                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                                    setTextColor(Color.RED)
                                }
                                
                                setPadding(32, 8, 16, 8)
                                isClickable = true
                                val outValue = TypedValue()
                                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                                setBackgroundResource(outValue.resourceId)
                                
                                setOnClickListener {
                                    val fhFormateada = formatearFechaHora(res.fechaHora)
                                    val title = if (res.lugarEliminado) getString(R.string.btn_limpiar_registro) else getString(R.string.title_cancelar_reserva)
                                    val msg = if (res.lugarEliminado) 
                                        getString(R.string.msg_reserva_lugar_eliminado_pregunta) 
                                        else getString(R.string.msg_confirmar_cancelar_reserva, res.nombreSala, fhFormateada)

                                    val posBtn = if (res.lugarEliminado) getString(R.string.opt_eliminar) else getString(R.string.btn_si)

                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(title)
                                        .setMessage(msg)
                                        .setPositiveButton(posBtn) { _, _ -> 
                                            menuViewModel.cancelReserva(res.id ?: "")
                                        }
                                        .setNegativeButton(R.string.btn_no, null)
                                        .show()
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
        val intent = android.content.Intent(this, GestionHorariosActivity::class.java)
        startActivity(intent)
    }
}
