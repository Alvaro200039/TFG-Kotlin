package com.example.tfg_kotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.Locale

class GestionHorariosActivity : AppCompatActivity() {

    private lateinit var menuViewModel: MenuViewModel
    private val blockedDates = mutableSetOf<String>()
    private val selectedWeeklyDays = mutableSetOf<Int>()
    private var currentCalendarView = Calendar.getInstance()
    private val calendarDates = mutableListOf<Calendar?>()
    private lateinit var calendarAdapter: RecyclerView.Adapter<*>
    private var currentMaxDuration = 2
    private var currentStepSize = 0.5f

    private var isBlockedExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_gestion_horarios)

            menuViewModel = ViewModelProvider(this)[MenuViewModel::class.java]

            setupToolbar()
            setupPickers()
            setupWeeklyDays()
            setupReservationParams()
            setupCalendar()
            setupSaveButton()
            setupObservers()

            menuViewModel.loadEmpresaSettings()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al abrir gestión de horarios: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupObservers() {
        menuViewModel.empresa.observe(this) { empresa ->
            empresa?.let {
                // Load Apertura
                val apParts = it.apertura.split(":")
                if (apParts.size == 2) {
                    findViewById<NumberPicker>(R.id.npAperturaHora).value = apParts[0].toInt()
                    findViewById<NumberPicker>(R.id.npAperturaMin).value = apParts[1].toInt()
                }
                // Load Cierre
                val ciParts = it.cierre.split(":")
                if (ciParts.size == 2) {
                    findViewById<NumberPicker>(R.id.npCierreHora).value = ciParts[0].toInt()
                    findViewById<NumberPicker>(R.id.npCierreMin).value = ciParts[1].toInt()
                }

                // Load Weekly Days
                selectedWeeklyDays.clear()
                selectedWeeklyDays.addAll(it.diasApertura)
                findViewById<LinearLayout>(R.id.layout_weekly_days)?.let { container ->
                    container.children.forEachIndexed { index, view ->
                        if (view is ToggleButton) {
                            view.isChecked = it.diasApertura.contains(index)
                        }
                    }
                }

                // Load Blocked Dates
                blockedDates.clear()
                blockedDates.addAll(it.diasBloqueados)
                findViewById<ViewGroup>(R.id.layoutBlockedDates)?.let { container ->
                    updateBlockedDatesList(container)
                }

                // Load Reservation Params
                currentStepSize = it.stepSize
                val b15 = findViewById<MaterialButton>(R.id.btn15m)
                val b30 = findViewById<MaterialButton>(R.id.btn30m)
                val b60 = findViewById<MaterialButton>(R.id.btn60m)
                
                b15.isChecked = it.stepSize == 0.25f
                b30.isChecked = it.stepSize == 0.5f
                b60.isChecked = it.stepSize == 1.0f

                currentMaxDuration = it.maxDuration
                findViewById<TextView>(R.id.tvMaxDuration).text = getString(R.string.label_x_horas, it.maxDuration)
            }
        }

        menuViewModel.loading.observe(this) { isLoading ->
            findViewById<Button>(R.id.btnSaveAll)?.isEnabled = !isLoading
        }

        menuViewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        menuViewModel.settingsSaveStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
                menuViewModel.clearSettingsSaveStatus()
                finish()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_gestion_horarios)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupPickers() {
        val formatter = NumberPicker.Formatter { i -> String.format(Locale.getDefault(), "%02d", i) }
        
        val pickers = listOf(
            R.id.npAperturaHora, R.id.npAperturaMin,
            R.id.npCierreHora, R.id.npCierreMin
        )

        pickers.forEach { id ->
            val picker = findViewById<NumberPicker>(id)
            picker.setFormatter(formatter)
            val isHora = resources.getResourceEntryName(id).contains("Hora", true)
            picker.minValue = 0
            picker.maxValue = if (isHora) 23 else 59
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupWeeklyDays() {
        val container = findViewById<LinearLayout>(R.id.layout_weekly_days)
        val days = listOf("L", "M", "X", "J", "V", "S", "D")

        val dp42 = (42 * resources.displayMetrics.density).toInt()
        days.forEachIndexed { index, day ->
            val button = ToggleButton(this).apply {
                text = day
                textOn = day
                textOff = day
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(dp42, dp42).apply {
                    setMargins(4, 4, 4, 4)
                }
                setBackgroundResource(R.drawable.aura_toggle_bg)
                isChecked = selectedWeeklyDays.contains(index)
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedWeeklyDays.add(index)
                    else selectedWeeklyDays.remove(index)
                    // Inform the adapter to refresh without recreating everything
                    if (::calendarAdapter.isInitialized) {
                        calendarAdapter.notifyDataSetChanged()
                    }
                }
            }
            container?.addView(button)
        }
    }

    private fun setupReservationParams() {
        val b15 = findViewById<MaterialButton>(R.id.btn15m)
        val b30 = findViewById<MaterialButton>(R.id.btn30m)
        val b60 = findViewById<MaterialButton>(R.id.btn60m)
        val tvMax = findViewById<TextView>(R.id.tvMaxDuration)
        val btnMinus = findViewById<MaterialButton>(R.id.btnMinusDuration)
        val btnPlus = findViewById<MaterialButton>(R.id.btnPlusDuration)

        val precisionButtons = listOf(b15, b30, b60)

        precisionButtons.forEach { btn ->
            btn.setOnClickListener {
                precisionButtons.forEach { it.isChecked = false }
                btn.isChecked = true
                currentStepSize = when (btn.id) {
                    R.id.btn15m -> 0.25f
                    R.id.btn30m -> 0.5f
                    R.id.btn60m -> 1.0f
                    else -> 0.5f
                }
            }
        }

        btnMinus.setOnClickListener {
            if (currentMaxDuration > 1) {
                currentMaxDuration--
                tvMax.text = getString(R.string.label_x_horas, currentMaxDuration)
            }
        }

        btnPlus.setOnClickListener {
            if (currentMaxDuration < 12) {
                currentMaxDuration++
                tvMax.text = getString(R.string.label_x_horas, currentMaxDuration)
            }
        }
    }

    private fun setupCalendar() {
        val rvCalendar = findViewById<RecyclerView>(R.id.rvCalendar)
        val tvMonthName = findViewById<TextView>(R.id.tvMonthName)
        val btnPrev = findViewById<MaterialButton>(R.id.btnPrevMonth)
        val btnNext = findViewById<MaterialButton>(R.id.btnNextMonth)

        calendarAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val date = calendarDates[position]
                val tvDay = holder.itemView.findViewById<TextView>(R.id.tvDay)
                val viewHighlight = holder.itemView.findViewById<View>(R.id.viewHighlight)

                if (date == null) {
                    tvDay.text = ""
                    viewHighlight.visibility = View.GONE
                } else {
                    val dayNum = date.get(Calendar.DAY_OF_MONTH)
                    tvDay.text = dayNum.toString()
                    
                    val firstDayOfWeek = date.get(Calendar.DAY_OF_WEEK)
                    val dayIndex = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
                    
                    val isNonLaborable = !selectedWeeklyDays.contains(dayIndex)
                    val dateKey = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date.time)
                    val isFestivo = blockedDates.contains(dateKey)

                    when {
                        isNonLaborable -> {
                            viewHighlight.visibility = View.VISIBLE
                            viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot_blue)
                            holder.itemView.setOnClickListener(null)
                        }
                        isFestivo -> {
                            viewHighlight.visibility = View.VISIBLE
                            viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot)
                            holder.itemView.setOnClickListener {
                                blockedDates.remove(dateKey)
                                notifyItemChanged(position)
                                findViewById<ViewGroup>(R.id.layoutBlockedDates)?.let { updateBlockedDatesList(it) }
                            }
                        }
                        else -> {
                            viewHighlight.visibility = View.GONE
                            holder.itemView.setOnClickListener {
                                blockedDates.add(dateKey)
                                notifyItemChanged(position)
                                findViewById<ViewGroup>(R.id.layoutBlockedDates)?.let { updateBlockedDatesList(it) }
                            }
                        }
                    }
                }
            }

            override fun getItemCount() = calendarDates.size
        }
        rvCalendar.adapter = calendarAdapter

        @SuppressLint("NotifyDataSetChanged")
        fun refreshCalendar() {
            val monthYearSDF = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            tvMonthName.text = monthYearSDF.format(currentCalendarView.time).replaceFirstChar { it.uppercase(Locale.getDefault()) }

            calendarDates.clear()
            val tempCal = currentCalendarView.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_MONTH, 1)

            val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
            val blanks = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
            for (i in 0 until blanks) calendarDates.add(null)

            val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDays) {
                val dayCal = tempCal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, i)
                calendarDates.add(dayCal)
            }
            calendarAdapter.notifyDataSetChanged()
        }

        btnPrev.setOnClickListener {
            currentCalendarView.add(Calendar.MONTH, -1)
            refreshCalendar()
        }
        btnNext.setOnClickListener {
            currentCalendarView.add(Calendar.MONTH, 1)
            refreshCalendar()
        }

        refreshCalendar()
    }

    private fun updateBlockedDatesList(container: ViewGroup) {
        container.removeAllViews()
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleBlocked)
        
        val sortedList = blockedDates.sortedBy { 
            java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it)?.time ?: 0L 
        }

        val displayList = if (isBlockedExpanded) sortedList else sortedList.take(4)

        displayList.forEach { date ->
            val chip = Chip(this).apply {
                text = date
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    blockedDates.remove(date)
                    val index = calendarDates.indexOfFirst { 
                        it != null && java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.time) == date 
                    }
                    if (index != -1) calendarAdapter.notifyItemChanged(index)
                    updateBlockedDatesList(container)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 4, 16, 4)
                }
            }
            container.addView(chip)
        }

        if (sortedList.size > 4) {
            btnToggle.visibility = View.VISIBLE
            btnToggle.rotation = if (isBlockedExpanded) 90f else -90f
            btnToggle.setOnClickListener {
                isBlockedExpanded = !isBlockedExpanded
                updateBlockedDatesList(container)
            }
        } else {
            btnToggle.visibility = View.GONE
        }
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.btnSaveAll).setOnClickListener {
            val apH = findViewById<NumberPicker>(R.id.npAperturaHora).value
            val apM = findViewById<NumberPicker>(R.id.npAperturaMin).value
            val ciH = findViewById<NumberPicker>(R.id.npCierreHora).value
            val ciM = findViewById<NumberPicker>(R.id.npCierreMin).value

            val apertura = String.format(Locale.getDefault(), "%02d:%02d", apH, apM)
            val cierre = String.format(Locale.getDefault(), "%02d:%02d", ciH, ciM)

            menuViewModel.saveScheduleSettings(
                apertura = apertura,
                cierre = cierre,
                diasApertura = selectedWeeklyDays.toList().sorted(),
                diasBloqueados = blockedDates.toList().sorted(),
                stepSize = currentStepSize,
                maxDuration = currentMaxDuration
            )
        }
    }
}
