package com.example.tfg_kotlin.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.data.model.Muro
import com.example.tfg_kotlin.data.model.Sala
import kotlin.math.*
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withSave

class PlanoReservasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var transformationMatrix = Matrix()
    private var inverseMatrix = Matrix()
    private var zoomScale = 1f
    
    // Objetos reutilizables para evitar asignaciones en onDraw/interacción
    private val reusableRect = RectF()
    private val reusablePath = Path()

    private val muroPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val itemPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var muros = listOf<Muro>()
    private var elementos = listOf<Sala>()
    private val itemStatusColors = mutableMapOf<String, Int>()

    private var onElementClicked: ((Sala) -> Unit)? = null

    private var lastFocusX = 0f
    private var lastFocusY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector) = true.also { lastFocusX = detector.focusX; lastFocusY = detector.focusY }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val newScale = zoomScale * factor
            if (newScale in 0.1f..10.0f) {
                zoomScale = newScale
                transformationMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                transformationMatrix.postTranslate(detector.focusX - lastFocusX, detector.focusY - lastFocusY)
                lastFocusX = detector.focusX; lastFocusY = detector.focusY
                invalidate()
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            transformationMatrix.postTranslate(-distanceX, -distanceY)
            invalidate()
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val world = screenToWorld(e.x, e.y)
            val clicked = elementos.reversed().find { 
                if (it.vertices.isNotEmpty()) {
                    isPointInPolygon(world[0], world[1], it)
                } else {
                    world[0] in it.x..it.x + it.ancho && 
                    world[1] in it.y..it.y + it.alto
                }
            }
            clicked?.let { onElementClicked?.invoke(it) }
            return true
        }
    })

    fun setMuros(l: List<Muro>) { muros = l; invalidate() }
    fun setElementos(l: List<Sala>) { elementos = l; invalidate() }
    fun setStatusColor(idSala: String, color: Int) { itemStatusColors[idSala] = color; invalidate() }
    fun setOnElementClickedListener(l: (Sala) -> Unit) { onElementClicked = l }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
        
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withMatrix(transformationMatrix) {
            muroPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
            muros.forEach { m ->
                muroPaint.strokeWidth = m.grosor
                canvas.drawLine(m.x1, m.y1, m.x2, m.y2, muroPaint)
            }

            elementos.forEach { sala ->
                val color = itemStatusColors[sala.id] ?: Color.WHITE
                drawElement(canvas, sala, color)
            }
        }
    }

    private fun drawElement(canvas: Canvas, sala: Sala, color: Int) {
        canvas.withSave {
            if (sala.tipo == "PUESTO") {
                canvas.rotate(sala.rotacion, sala.x + sala.ancho / 2f, sala.y + sala.alto / 2f)
                drawPuesto(canvas, sala, color)
            } else if (sala.vertices.isNotEmpty()) {
                drawPolygonSala(canvas, sala, color)
            } else {
                drawRectSala(canvas, sala, color)
            }
        }
    }

    private fun drawRectSala(canvas: Canvas, sala: Sala, color: Int) {
        reusableRect.set(sala.x, sala.y, sala.x + sala.ancho, sala.y + sala.alto)
        itemPaint.style = Paint.Style.FILL
        itemPaint.color = color
        canvas.drawRect(reusableRect, itemPaint)

        itemPaint.style = Paint.Style.STROKE
        itemPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
        itemPaint.strokeWidth = 3f
        canvas.drawRect(reusableRect, itemPaint)

        drawSalaText(canvas, sala)
    }

    private fun drawPolygonSala(canvas: Canvas, sala: Sala, color: Int) {
        reusablePath.reset()
        reusablePath.moveTo(sala.x + sala.vertices[0].x, sala.y + sala.vertices[0].y)
        for (i in 1 until sala.vertices.size) {
            reusablePath.lineTo(sala.x + sala.vertices[i].x, sala.y + sala.vertices[i].y)
        }
        reusablePath.close()

        itemPaint.style = Paint.Style.FILL
        itemPaint.color = color
        canvas.drawPath(reusablePath, itemPaint)

        itemPaint.style = Paint.Style.STROKE
        itemPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
        itemPaint.strokeWidth = 3f
        canvas.drawPath(reusablePath, itemPaint)

        drawSalaText(canvas, sala)
    }

    private fun drawPuesto(canvas: Canvas, sala: Sala, color: Int) {
        val mesaH = sala.alto * 0.6f
        reusableRect.set(sala.x, sala.y, sala.x + sala.ancho, sala.y + mesaH)

        itemPaint.style = Paint.Style.FILL
        itemPaint.color = color
        canvas.drawRect(reusableRect, itemPaint)

        itemPaint.style = Paint.Style.STROKE
        itemPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
        itemPaint.strokeWidth = 3f
        canvas.drawRect(reusableRect, itemPaint)

        val sillaW = sala.ancho * 0.5f
        val sillaH = sala.alto * 0.4f
        reusableRect.set(
            sala.x + sala.ancho/2f - sillaW/2f,
            sala.y + mesaH - sillaH/2f,
            sala.x + sala.ancho/2f + sillaW/2f,
            sala.y + mesaH + sillaH/2f
        )
        itemPaint.style = Paint.Style.FILL
        itemPaint.color = color
        canvas.drawArc(reusableRect, 0f, 180f, false, itemPaint)

        itemPaint.style = Paint.Style.STROKE
        itemPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
        canvas.drawArc(reusableRect, 0f, 180f, false, itemPaint)

        // Texto del puesto (contra-rotado para que esté recto)
        canvas.withSave {
            val tx = sala.x + sala.ancho / 2f
            val ty = sala.y + mesaH / 2f + 8f
            canvas.rotate(-sala.rotacion, tx, ty)
            textPaint.color = Color.BLACK
            textPaint.textSize = 20f
            canvas.drawText(sala.nombre.replace("Puesto ", ""), tx, ty, textPaint)
        }
    }

    private fun drawSalaText(canvas: Canvas, sala: Sala) {
        textPaint.color = Color.BLACK
        textPaint.textSize = 24f
        val tx = sala.x + sala.ancho/2f
        val ty = sala.y + sala.alto/2f
        
        val lines = sala.nombre.split("\n")
        val fm = textPaint.fontMetrics
        val h = fm.descent - fm.ascent
        var y = ty - (h * (lines.size - 1) / 2f) - fm.ascent
        lines.forEach { 
            canvas.drawText(it, tx, y, textPaint)
            y += h
        }
    }

    fun fitToScreen() {
        if (elementos.isEmpty() && muros.isEmpty()) return
        
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

        elementos.forEach {
            minX = min(minX, it.x); minY = min(minY, it.y)
            maxX = max(maxX, it.x + it.ancho); maxY = max(maxY, it.y + it.alto)
        }
        muros.forEach {
            minX = min(minX, min(it.x1, it.x2)); minY = min(minY, min(it.y1, it.y2))
            maxX = max(maxX, max(it.x1, it.x2)); maxY = max(maxY, max(it.y1, it.y2))
        }

        val padding = 100f
        val contentW = (maxX - minX) + padding * 2
        val contentH = (maxY - minY) + padding * 2
        
        post {
            val viewW = width.toFloat()
            val viewH = height.toFloat()
            if (viewW == 0f || viewH == 0f) return@post

            val scale = min(viewW / contentW, viewH / contentH).coerceIn(0.1f, 5f)
            zoomScale = scale

            transformationMatrix.reset()
            transformationMatrix.postTranslate(-minX + padding, -minY + padding)
            transformationMatrix.postScale(scale, scale, 0f, 0f)
            
            // Centrar
            val scaledW = contentW * scale
            val scaledH = contentH * scale
            val dx = (viewW - scaledW) / 2f
            val dy = (viewH - scaledH) / 2f
            transformationMatrix.postTranslate(dx, dy)
            
            invalidate()
        }
    }

    private fun screenToWorld(sx: Float, sy: Float): FloatArray {
        transformationMatrix.invert(inverseMatrix)
        val pts = floatArrayOf(sx, sy)
        inverseMatrix.mapPoints(pts)
        return pts
    }

    private fun isPointInPolygon(x: Float, y: Float, sala: Sala): Boolean {
        var intersectCount = 0
        val n = sala.vertices.size
        for (i in 0 until n) {
            val v1 = sala.vertices[i]
            val v2 = sala.vertices[(i + 1) % n]
            val x1 = sala.x + v1.x; val y1 = sala.y + v1.y
            val x2 = sala.x + v2.x; val y2 = sala.y + v2.y
            if (((y in y1..<y2) || (y in y2..<y1)) &&
                (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)) {
                intersectCount++
            }
        }
        return intersectCount % 2 != 0
    }
}
