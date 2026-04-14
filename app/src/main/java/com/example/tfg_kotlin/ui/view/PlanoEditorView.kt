@file:Suppress("UnusedExpression")

package com.example.tfg_kotlin.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.tfg_kotlin.data.model.Muro
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.data.model.Vertex
import androidx.core.graphics.withClip
import androidx.core.graphics.withSave
import kotlin.math.*
import android.widget.Toast
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withMatrix
import com.example.tfg_kotlin.R

@Suppress("UnusedExpression")
class PlanoEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var transformationMatrix = Matrix()
    private var inverseMatrix = Matrix()

    private val history = mutableListOf<PlanoState>()
    private data class PlanoState(val muros: List<Muro>, val elementos: List<Sala>)

    enum class EditorMode { SELECT, DRAW_WALL, DELETE, ADD_SALA, ADD_PUESTO }
    private var mode = EditorMode.SELECT

    private val gridPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }
    private val elementPaint = Paint().apply { isAntiAlias = true }
    private val muroPaint = Paint().apply { 
        color = Color.BLACK
        strokeWidth = 15f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        alpha = 255
    }

    private var gridSize = 50f
    private var zoomScale = 1f
    private val reusableRect = RectF()
    private val reusablePath = Path()
    private val reusableMatrix = Matrix()
    private val reusablePts = FloatArray(2)
    private val gridValues = FloatArray(9)
    private var elementos = mutableListOf<Sala>()
    private var muros = mutableListOf<Muro>()
    private var currentMuro: Muro? = null
    private var selectedMuro: Muro? = null
    private var draggingMuroEnd: Int = -1 // -1: none, 0: start (x1,y1), 1: end (x2,y2)
    private var selectedElement: Sala? = null

    private var isResizing = false
    private var isRotating = false
    private var isDraggingVertex = false // Keep for compat if needed, but logic removed
    private var touchedVertexIndex = -1
    private var isMultitouch = false 

    private var onElementSelected: ((Sala?, Float, Float) -> Unit)? = null
    private var onElementMovedOrResized: ((Sala) -> Unit)? = null
    private var onElementScreenPositionChanged: ((Sala, Float, Float) -> Unit)? = null

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialElementX = 0f
    private var initialElementY = 0f

    private var lastFocusX = 0f
    private var lastFocusY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector) = true.also { lastFocusX = detector.focusX; lastFocusY = detector.focusY }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val newScale = zoomScale * factor
            if (newScale in 0.2f..5.0f) {
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
            if (mode == EditorMode.SELECT && selectedElement == null) {
                transformationMatrix.postTranslate(-distanceX, -distanceY); invalidate()
            }
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val world = screenToWorld(e.x, e.y)
            selectAt(world[0], world[1], e.x, e.y)
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withMatrix(transformationMatrix) {
            drawGrid(canvas)
            drawElements(canvas)
            drawMuros(canvas)
        }

        selectedElement?.let { element ->
            reusableRect.set(element.x, element.y, element.x + element.ancho, element.y + element.alto)
            
            // FAB de Edición (Top-Right)
            reusablePts[0] = element.x + element.ancho
            reusablePts[1] = element.y
            reusableMatrix.setRotate(element.rotacion, reusableRect.centerX(), reusableRect.centerY())
            reusableMatrix.postConcat(transformationMatrix)
            reusableMatrix.mapPoints(reusablePts)
            onElementScreenPositionChanged?.invoke(element, reusablePts[0], reusablePts[1])
        }
    }

    private fun drawGrid(canvas: Canvas) {
        transformationMatrix.getValues(gridValues)
        val s = gridValues[Matrix.MSCALE_X]; val tx = gridValues[Matrix.MTRANS_X]; val ty = gridValues[Matrix.MTRANS_Y]
        val left = -tx/s; val top = -ty/s; val right = (width-tx)/s; val bottom = (height-ty)/s
        val step = gridSize 
        gridPaint.alpha = 255
        gridPaint.strokeWidth = 1f / s // Mantener grosor constante en pantalla
        
        var sx = floor(left.toDouble() / step).toFloat() * step
        while (sx <= right) { canvas.drawLine(sx, top, sx, bottom, gridPaint); sx += step }
        var sy = floor(top.toDouble() / step).toFloat() * step
        while (sy <= bottom) { canvas.drawLine(left, sy, right, sy, gridPaint); sy += step }
    }

    private fun drawMuros(canvas: Canvas) {
        val onSurface = ContextCompat.getColor(context, R.color.aura_on_surface)
        muroPaint.color = onSurface
        muroPaint.alpha = 255
        muroPaint.strokeWidth = 12f
        muroPaint.style = Paint.Style.STROKE
        
        muros.forEach { muro ->
            canvas.drawLine(muro.x1, muro.y1, muro.x2, muro.y2, muroPaint)
            
            // Dibujar tiradores si el muro está seleccionado
            if (muro == selectedMuro) {
                val r = 20f / zoomScale.coerceAtMost(1f)
                elementPaint.style = Paint.Style.STROKE
                elementPaint.strokeWidth = 3f / zoomScale.coerceAtMost(1f)
                elementPaint.color = "#FFD600".toColorInt() // Amarillo Aura
                canvas.drawCircle(muro.x1, muro.y1, r, elementPaint)
                canvas.drawCircle(muro.x2, muro.y2, r, elementPaint)
            }
        }
        currentMuro?.let { canvas.drawLine(it.x1, it.y1, it.x2, it.y2, muroPaint) }
    }

    private fun drawElements(canvas: Canvas) {
        elementos.forEach { sala ->
            reusableRect.set(sala.x, sala.y, sala.x + sala.ancho, sala.y + sala.alto)
            val isSelected = sala == selectedElement
            val isOpen = !isSalaEnclosed(sala)
            
            elementPaint.style = Paint.Style.FILL
            elementPaint.color = when {
                isOpen -> Color.argb(120, 255, 0, 0) // Rojo transparente para salas abiertas
                isSelected -> Color.argb(100, 255, 255, 0)
                sala.tipo == TipoElemento.PUESTO.valor -> Color.argb(100, 0, 150, 255)
                else -> Color.argb(80, 200, 200, 200)
            }
            
            canvas.withSave {
                if (sala.tipo == TipoElemento.SALA.valor) {
                    reusablePath.reset()
                    if (sala.vertices.isNotEmpty()) {
                        reusablePath.moveTo(sala.x + sala.vertices[0].x, sala.y + sala.vertices[0].y)
                        for (i in 1 until sala.vertices.size) reusablePath.lineTo(sala.x + sala.vertices[i].x, sala.y + sala.vertices[i].y)
                        reusablePath.close()
                    } else {
                        reusablePath.addRect(reusableRect, Path.Direction.CW)
                    }

                    drawPath(reusablePath, elementPaint)
                    
                    elementPaint.color = ContextCompat.getColor(context, R.color.aura_on_surface)
                    elementPaint.style = Paint.Style.STROKE
                    elementPaint.strokeWidth = 2f
                    drawPath(reusablePath, elementPaint)
                    
                    elementPaint.style = Paint.Style.FILL
                    elementPaint.textSize = 24f
                    val textWidth = elementPaint.measureText(sala.nombre)
                    
                    withClip(reusablePath) {
                        elementPaint.color = Color.BLACK
                        drawText(sala.nombre, reusableRect.centerX() - textWidth / 2, reusableRect.centerY() + 10f, elementPaint)
                    }
                } else {
                    rotate(sala.rotacion, reusableRect.centerX(), reusableRect.centerY())
                    drawPuesto(canvas, reusableRect, sala, isSelected)
                    if (isSelected) {
                        val r = 25f / zoomScale.coerceAtMost(1f)
                        val dist = 60f / zoomScale.coerceAtMost(1f)
                        elementPaint.color = "#FFD600".toColorInt()
                        drawCircle(reusableRect.centerX(), reusableRect.top - dist, r, elementPaint)
                        elementPaint.strokeWidth = 3f
                        drawLine(reusableRect.centerX(), reusableRect.top, reusableRect.centerX(), reusableRect.top - dist, elementPaint)
                    }
                }
            }
        }
    }

    private fun drawPuesto(canvas: Canvas, rect: RectF, element: Sala, selected: Boolean) {
        val onSurface = ContextCompat.getColor(context, R.color.aura_on_surface)
        elementPaint.color = if (selected) Color.BLUE else onSurface
        elementPaint.style = Paint.Style.STROKE
        elementPaint.strokeWidth = 2f
        
        // Dibujar la Mesa (Rectángulo superior)
        val mesaH = rect.height() * 0.6f
        reusableRect.set(rect.left, rect.top, rect.right, rect.top + mesaH)
        canvas.drawRect(reusableRect, elementPaint)
        
        // Dibujar la Silla (Semicírculo inferior)
        val sillaW = rect.width() * 0.5f
        val sillaH = rect.height() * 0.4f
        reusableRect.set(
            rect.centerX() - sillaW / 2f, 
            rect.top + mesaH - sillaH / 2f, 
            rect.centerX() + sillaW / 2f, 
            rect.top + mesaH + sillaH / 2f
        )
        canvas.drawArc(reusableRect, 0f, 180f, false, elementPaint)
        
        // 5. Dibujar el identificador (Número) siempre recto
        elementPaint.style = Paint.Style.FILL
        elementPaint.color = Color.BLACK
        elementPaint.textSize = 20f
        elementPaint.textAlign = Paint.Align.CENTER
        val idText = element.nombre.replace("Puesto ", "")
        
        canvas.withSave {
            val tx = rect.centerX()
            val ty = rect.top + mesaH / 2f + 8f
            rotate(-element.rotacion, tx, ty) // Contra-rotación para que el texto esté recto
            drawText(idText, tx, ty, elementPaint)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
        }

        if (event.pointerCount > 1 || scaleDetector.isInProgress) {
            isMultitouch = true; currentMuro = null; invalidate(); return true
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) isMultitouch = false

        val world = screenToWorld(event.x, event.y)
        
        // Determinar qué estamos tocando para decidir si interactuar o añadir
        val touchedElement = elementos.reversed().find { 
            world[0] in it.x..it.x + it.ancho && 
            world[1] in it.y..it.y + it.alto 
        }
        val touchedMuro = if (touchedElement == null) muros.find { isPointNearLine(world[0], world[1], it) } else null
        var hittingHandle = false

        // Check wall handles
        selectedMuro?.let { muro ->
            val hr = 40f / zoomScale
            val d1 = (world[0]-muro.x1)*(world[0]-muro.x1) + (world[1]-muro.y1)*(world[1]-muro.y1)
            val d2 = (world[0]-muro.x2)*(world[0]-muro.x2) + (world[1]-muro.y2)*(world[1]-muro.y2)
            if (d1 < hr*hr || d2 < hr*hr) hittingHandle = true
        }

        // Check rotation handle
        if (!hittingHandle) {
            selectedElement?.let { if (isHittingRotationHandle(world[0], world[1], it)) hittingHandle = true }
        }

        // 0. Gestión de Deselección
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !isMultitouch) {
            if (touchedElement == null && touchedMuro == null && !hittingHandle) {
                selectedElement = null; selectedMuro = null; onElementSelected?.invoke(null, 0f, 0f); invalidate()
            }
        }

        // 1. Gestión GLOBAL de arrastre de extremos de muro
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !isMultitouch) {
            selectedMuro?.let { muro ->
                val hr = 40f / zoomScale
                if (world[0] in (muro.x1-hr)..(muro.x1+hr) && world[1] in (muro.y1-hr)..(muro.y1+hr)) {
                    draggingMuroEnd = 0; return true
                } else if (world[0] in (muro.x2-hr)..(muro.x2+hr) && world[1] in (muro.y2-hr)..(muro.y2+hr)) {
                    draggingMuroEnd = 1; return true
                }
            }
        }
        if (draggingMuroEnd != -1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val sx = world[0].snapToGrid(gridSize / 2f)
                    val sy = world[1].snapToGrid(gridSize / 2f)
                    selectedMuro?.let { if (draggingMuroEnd == 0) { it.x1 = sx; it.y1 = sy } else { it.x2 = sx; it.y2 = sy } }
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    mergeCollinearMuros()
                    saveState(); draggingMuroEnd = -1; onElementMovedOrResized?.invoke(Sala(idPiso = "WALL-SYNC")); invalidate()
                }
            }
            return true
        }

        // 2. Usar detector de gestos (Scroll y Tap) para deselección/selección en TODOS los modos
        gestureDetector.onTouchEvent(event)

        // MODO DIBUJO DE MUROS: Prioridad sobre scroll de un solo dedo
        if (mode == EditorMode.DRAW_WALL && !isMultitouch) {
            handleDrawWall(event)
            return true
        }

        if ((mode == EditorMode.SELECT) && !isMultitouch) {
            handleInteraction(event, world)
            return true
        }

        if (mode == EditorMode.DELETE) { handleDelete(event); return true }
        
        // Si tocamos algo existente o un tirador, permitimos interacción (mover/rotar/seleccionar) 
        // incluso en modo "Añadir"
        if (touchedElement != null || hittingHandle || isRotating || isResizing) {
            handleInteraction(event, world)
            return true
        }

        if (mode == EditorMode.ADD_SALA || mode == EditorMode.ADD_PUESTO) { handleAdd(event, world); return true }
        
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findClosedSpace(px: Float, py: Float): List<Vertex>? {
        if (muros.isEmpty()) return null

        val snap = gridSize / 2f
        fun snapVal(v: Float) = (v / snap).roundToInt() * snap

        data class P(val x: Float, val y: Float)
        val allPoints = mutableSetOf<P>()
        muros.forEach { m ->
            allPoints.add(P(snapVal(m.x1), snapVal(m.y1)))
            allPoints.add(P(snapVal(m.x2), snapVal(m.y2)))
        }

        val graph = mutableMapOf<P, MutableList<P>>()
        
        muros.forEach { m ->
            val p1 = P(snapVal(m.x1), snapVal(m.y1))
            val p2 = P(snapVal(m.x2), snapVal(m.y2))
            
            // Encontrar puntos que caen sobre este muro para dividirlo en el grafo
            val pointsOnMuro = allPoints.filter { p ->
                val dAP = sqrt(distSq(p.x, p.y, p1.x, p1.y))
                val dPB = sqrt(distSq(p.x, p.y, p2.x, p2.y))
                val dAB = sqrt(distSq(p1.x, p1.y, p2.x, p2.y))
                abs(dAP + dPB - dAB) < 1.0f
            }.sortedBy { distSq(it.x, it.y, p1.x, p1.y) }

            for (i in 0 until pointsOnMuro.size - 1) {
                val pt1 = pointsOnMuro[i]
                val pt2 = pointsOnMuro[i+1]
                if (pt1 != pt2) {
                    graph.getOrPut(pt1) { mutableListOf() }.add(pt2)
                    graph.getOrPut(pt2) { mutableListOf() }.add(pt1)
                }
            }
        }

        // Raycasting hacia la derecha para encontrar el muro más cercano
        var closestMuro: Pair<P, P>? = null
        var minXDist = Float.MAX_VALUE

        graph.forEach { (p1, neighbors) ->
            neighbors.forEach { p2 ->
                if (p1.x < p2.x || (p1.x == p2.x && p1.y < p2.y)) {
                    val yMin = minOf(p1.y, p2.y)
                    val yMax = maxOf(p1.y, p2.y)
                    if (py in yMin..yMax && abs(p1.y - p2.y) > 0.1f) {
                        val intersectX = p1.x + (p2.x - p1.x) * (py - p1.y) / (p2.y - p1.y)
                        if (intersectX > px) {
                            val dist = intersectX - px
                            if (dist < minXDist) {
                                minXDist = dist
                                closestMuro = Pair(p1, p2)
                            }
                        }
                    }
                }
            }
        }

        if (closestMuro == null) return null

        val m = closestMuro!!
        var startNode: P
        var nextNode: P
        
        // El punto está a la izquierda. Si vamos de "abajo" a "arriba" por el muro, el punto queda a la izquierda.
        if (m.first.y > m.second.y) {
            startNode = m.first; nextNode = m.second
        } else {
            startNode = m.second; nextNode = m.first
        }
        
        val polygon = mutableListOf<Vertex>()
        var current = nextNode
        var prev = startNode
        polygon.add(Vertex(startNode.x, startNode.y))
        
        val maxSteps = muros.size * 2
        var steps = 0
        while (current != startNode && steps < maxSteps) {
            polygon.add(Vertex(current.x, current.y))
            val neighbors = graph[current] ?: break
            val inAngle = atan2(current.y - prev.y, current.x - prev.x)
            
            var bestNext: P? = null
            var bestAngleDiff = Double.MAX_VALUE 
            
            for (next in neighbors) {
                if (next == prev) continue
                val outAngle = atan2(next.y - current.y, next.x - current.x)
                var diff = (outAngle - inAngle).toDouble()
                while (diff <= -PI) diff += 2 * PI
                while (diff > PI) diff -= 2 * PI
                
                // Seguir el hueco: giro más a la derecha posible
                if (diff < bestAngleDiff) {
                    bestAngleDiff = diff
                    bestNext = next
                }
            }
            if (bestNext == null) break
            prev = current; current = bestNext; steps++
        }
        
        if (current == startNode && polygon.size >= 3) {
            if (isPointInPolygon(px, py, polygon)) return polygon
        }
        return null
    }

    private fun isPointInPolygon(px: Float, py: Float, polygon: List<Vertex>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            if (((polygon[i].y > py) != (polygon[j].y > py)) &&
                (px < (polygon[j].x - polygon[i].x) * (py - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun isHittingRotationHandle(wx: Float, wy: Float, element: Sala): Boolean {
        if (element.tipo != TipoElemento.PUESTO.valor) return false
        val rect = RectF(element.x, element.y, element.x + element.ancho, element.y + element.alto)
        val rDist = 60f / zoomScale.coerceAtMost(1f)
        val ptsRotateHandle = floatArrayOf(rect.centerX(), rect.top - rDist)
        val mat = Matrix()
        mat.postRotate(element.rotacion, rect.centerX(), rect.centerY())
        mat.mapPoints(ptsRotateHandle)
        
        // Área de toque algo más generosa (mínimo de px en pantalla)
        val hr = maxOf(120f / zoomScale, 50f) 
        return wx in (ptsRotateHandle[0]-hr)..(ptsRotateHandle[0]+hr) && 
               wy in (ptsRotateHandle[1]-hr)..(ptsRotateHandle[1]+hr)
    }

    private fun handleInteraction(event: MotionEvent, world: FloatArray) {
        120f / zoomScale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                var handled = false
                selectedElement?.let { element ->
                    if (isHittingRotationHandle(world[0], world[1], element)) {
                        isRotating = true; handled = true
                    }
                }
                
                // 2. Detectar Tiradores de Muro
                if (!handled) {
                    selectedMuro?.let { muro ->
                        val hrMuro = 40f / zoomScale
                        if (world[0] in (muro.x1-hrMuro)..(muro.x1+hrMuro) && world[1] in (muro.y1-hrMuro)..(muro.y1+hrMuro)) {
                            draggingMuroEnd = 0; handled = true
                        } else if (world[0] in (muro.x2-hrMuro)..(muro.x2+hrMuro) && world[1] in (muro.y2-hrMuro)..(muro.y2+hrMuro)) {
                            draggingMuroEnd = 1; handled = true
                        }
                    }
                }

                // 3. Seleccionar nuevo elemento si nada fue manejado
                if (!handled) {
                    val touched = elementos.reversed().find { 
                        world[0] in it.x..it.x + it.ancho && 
                        world[1] in it.y..it.y + it.alto 
                    }
                    if (touched != null) {
                        selectedElement = touched
                        onElementSelected?.invoke(touched, event.x, event.y)
                        initialTouchX = world[0]; initialTouchY = world[1]
                        initialElementX = touched.x; initialElementY = touched.y
                        selectedMuro = null
                    } else {
                        selectedElement = null
                        selectedMuro = null
                        onElementSelected?.invoke(null, 0f, 0f)
                        mode = EditorMode.SELECT
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                selectedElement?.let {
                    if (isRotating) {
                        val angle = atan2(
                            (world[1] - (it.y + it.alto / 2)).toDouble(),
                            (world[0] - (it.x + it.ancho / 2)).toDouble()
                        )
                        var deg = (Math.toDegrees(angle).toFloat() + 90f) % 360f
                        if (deg < 0) deg += 360f
                        val snapPoint = when {
                            abs(deg - 0f) < 7f || abs(deg - 360f) < 7f -> 0f
                            abs(deg - 90f) < 7f -> 90f
                            abs(deg - 180f) < 7f -> 180f
                            abs(deg - 270f) < 7f -> 270f
                            else -> deg
                        }
                        it.rotacion = snapPoint
                    } else {
                        // REGLA: Si la sala está cerrada por muros, no se puede MOVER, solo editar (FAB)
                        if (it.tipo == TipoElemento.SALA.valor && isSalaEnclosed(it)) {
                            // No hace nada para arrastrar si es una sala cerrada
                        } else {
                            val nx = (initialElementX + world[0] - initialTouchX).snapToGrid()
                            val ny = (initialElementY + world[1] - initialTouchY).snapToGrid()
                            val oldX = it.x; val oldY = it.y
                            it.x = nx; it.y = ny
                            if (elementos.any { other -> it != other && isOverlapping(it, other) }) {
                                it.x = oldX; it.y = oldY
                            }
                        }
                    }
                }
                
                if (draggingMuroEnd != -1) {
                    selectedMuro?.let { muro ->
                        val sx = world[0].snapToGrid(gridSize / 2f)
                        val sy = world[1].snapToGrid(gridSize / 2f)
                        if (draggingMuroEnd == 0) { muro.x1 = sx; muro.y1 = sy } else { muro.x2 = sx; muro.y2 = sy }
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (isRotating || selectedElement != null || draggingMuroEnd != -1) {
                    saveState()
                    selectedElement?.let { onElementMovedOrResized?.invoke(it) }
                    if (draggingMuroEnd != -1) onElementMovedOrResized?.invoke(Sala(idPiso = "WALL-SYNC"))
                }
                isResizing = false; isRotating = false; isDraggingVertex = false; touchedVertexIndex = -1; draggingMuroEnd = -1
            }
        }
    }

    private fun isOverlapping(s1: Sala, s2: Sala): Boolean {
        val r1 = RectF(s1.x, s1.y, s1.x + s1.ancho, s1.y + s1.alto)
        val r2 = RectF(s2.x, s2.y, s2.x + s2.ancho, s2.y + s2.alto)
        return r1.left < r2.right && r1.right > r2.left && r1.top < r2.bottom && r1.bottom > r2.top
    }

    private fun handleAdd(event: MotionEvent, world: FloatArray) {
        if (event.actionMasked == MotionEvent.ACTION_UP && !isMultitouch) {
            // 1. Prioridad: Seleccionar si hay algo debajo
            val existing = elementos.reversed().find {
                world[0] >= it.x && world[0] <= it.x + it.ancho &&
                world[1] >= it.y && world[1] <= it.y + it.alto
            }
            if (existing != null) {
                selectedElement = existing
                onElementSelected?.invoke(selectedElement, event.x, event.y)
                invalidate()
                return
            }

            // 2. Si es espacio vacío, añadir nuevo elemento
            val element = if (mode == EditorMode.ADD_SALA) {
                val poly = findClosedSpace(world[0], world[1])
                if (poly == null) {
                    Toast.makeText(context, context.getString(R.string.msg_pulsa_recinto_cerrado), Toast.LENGTH_SHORT).show()
                    return
                }

                // REGLA: No se puede crear una SALA si ya hay PUESTOS en ese recinto
                val hayPuestos = elementos.any { 
                    it.tipo == TipoElemento.PUESTO.valor && isPointInPolygon(it.x + it.ancho/2f, it.y + it.alto/2f, poly) 
                }
                if (hayPuestos) {
                    Toast.makeText(context, context.getString(R.string.msg_no_sala_en_area_puestos), Toast.LENGTH_SHORT).show()
                    return
                }
                val minX = poly.minOf { it.x }; val minY = poly.minOf { it.y }
                val maxX = poly.maxOf { it.x }; val maxY = poly.maxOf { it.y }

                val nextSalaNum = (elementos.filter { it.tipo == TipoElemento.SALA.valor }
                    .mapNotNull { it.nombre.substringAfter("Sala ", "").toIntOrNull() }
                    .maxOrNull() ?: 0) + 1

                Sala(
                    nombre = "Sala $nextSalaNum",
                    x = minX, y = minY,
                    ancho = maxX - minX, alto = maxY - minY,
                    vertices = poly.map { Vertex(it.x - minX, it.y - minY) }.toMutableList(),
                    tipo = TipoElemento.SALA.valor
                )
            } else {
                val nextPuestoNum = (elementos.filter { it.tipo == TipoElemento.PUESTO.valor }
                    .mapNotNull { it.nombre.toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                Sala(nombre = "$nextPuestoNum", x = world[0].snapToGrid()-50, y = world[1].snapToGrid()-50, ancho = 100f, alto = 100f, tipo = TipoElemento.PUESTO.valor)
            }
            addElement(element)
            onElementMovedOrResized?.invoke(element)
            // seleccionar el nuevo elemento para permitir rotación inmediata
            selectedElement = element
            onElementSelected?.invoke(element, event.x, event.y)
            invalidate()
        }
    }

    private fun handleDelete(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val world = screenToWorld(event.x, event.y)
            val sala = elementos.find { 
                world[0] in it.x..it.x + it.ancho && 
                world[1] in it.y..it.y + it.alto 
            }
            if (sala != null) { 
                saveState(); elementos.remove(sala)
                if (selectedElement == sala) { selectedElement = null; onElementSelected?.invoke(null, 0f, 0f) } 
            } else { 
                muros.find { isPointNearLine(world[0], world[1], it) }?.let { muro ->
                    saveState(); muros.remove(muro)
                    if (selectedMuro == muro) selectedMuro = null
                } 
            }
            invalidate()
        }
    }

    private fun isPointNearLine(px: Float, py: Float, muro: Muro): Boolean {
        val dx = muro.x2-muro.x1; val dy = muro.y2-muro.y1; val len = dx*dx + dy*dy
        if (len == 0f) return (px-muro.x1)*(px-muro.x1) + (py-muro.y1)*(py-muro.y1) < 400
        var t = ((px-muro.x1)*dx + (py-muro.y1)*dy) / len; t = t.coerceIn(0f, 1f)
        val projX = muro.x1 + t*dx; val projY = muro.y1 + t*dy
        return (px-projX)*(px-projX) + (py-projY)*(py-projY) < (900/(zoomScale*zoomScale))
    }

    private fun handleDrawWall(event: MotionEvent) {
        val world = screenToWorld(event.x, event.y)
        val sx = world[0].snapToGrid(gridSize / 2f)
        val sy = world[1].snapToGrid(gridSize / 2f)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentMuro = Muro(x1 = sx, y1 = sy, x2 = sx, y2 = sy)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                currentMuro?.apply {
                    x2 = sx
                    y2 = sy
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentMuro?.let {
                    if (it.x1 != it.x2 || it.y1 != it.y2) {
                        saveState()
                        muros.add(it)
                        mergeCollinearMuros()
                        onElementMovedOrResized?.invoke(Sala(idPiso = "WALL-SYNC")) // Forzar sync
                    }
                }
                currentMuro = null
                invalidate()
            }
        }
    }

    private fun mergeCollinearMuros() {
        if (muros.size < 2) return
        var changed = true
        while (changed) {
            changed = false
            var i = 0
            while (i < muros.size) {
                var j = i + 1
                while (j < muros.size) {
                    val m1 = muros[i]
                    val m2 = muros[j]
                    
                    val shared = when {
                        isSamePoint(m1.x1, m1.y1, m2.x1, m2.y1) -> true
                        isSamePoint(m1.x1, m1.y1, m2.x2, m2.y2) -> true
                        isSamePoint(m1.x2, m1.y2, m2.x1, m2.y1) -> true
                        isSamePoint(m1.x2, m1.y2, m2.x2, m2.y2) -> true
                        else -> false
                    }
                    
                    if (shared) {
                        val s1 = atan2(m1.y2 - m1.y1, m1.x2 - m1.x1)
                        val s2 = atan2(m2.y2 - m2.y1, m2.x2 - m2.x1)
                        if (areSlopesSame(s1, s2)) {
                            val pts = listOf(PointF(m1.x1, m1.y1), PointF(m1.x2, m1.y2), PointF(m2.x1, m2.y1), PointF(m2.x2, m2.y2))
                            1f
                            var pA: PointF? = null
                            var pB: PointF? = null
                            for (idx1 in 0..3) {
                                for (idx2 in idx1+1..3) {
                                    distSq(pts[idx1].x, pts[idx1].y, pts[idx2].x, pts[idx2].y)
                            pA = pts[idx1]
                            pB = pts[idx2]
                                }
                            }
                            if (pA != null && pB != null) {
                                m1.x1 = pA.x; m1.y1 = pA.y; m1.x2 = pB.x; m1.y2 = pB.y
                                muros.removeAt(j)
                                changed = true
                                continue
                            }
                        }
                    }
                    j++
                }
                i++
            }
        }
    }

    private fun isSamePoint(ax: Float, ay: Float, bx: Float, by: Float) = abs(ax - bx) < 1f && abs(ay - by) < 1f
    private fun areSlopesSame(s1: Float, s2: Float): Boolean {
        var d = abs(s1 - s2)
        while (d > PI) d -= PI.toFloat()
        return d < 0.01f || abs(d - PI.toFloat()) < 0.01f
    }
    private fun distSq(ax: Float, ay: Float, bx: Float, by: Float) = (ax-bx)*(ax-bx) + (ay-by)*(ay-by)

    private fun Float.snapToGrid(size: Float = gridSize): Float = (this / size).roundToInt() * size
    private fun screenToWorld(sx: Float, sy: Float): FloatArray {
        transformationMatrix.invert(inverseMatrix); val pts = floatArrayOf(sx, sy); inverseMatrix.mapPoints(pts); return pts
    }

    private fun selectAt(wx: Float, wy: Float, sx: Float, sy: Float) {
        selectedElement = elementos.reversed().find { 
            wx in it.x..it.x + it.ancho && 
            wy in it.y..it.y + it.alto 
        }
        selectedMuro = if (selectedElement != null) {
            null
        } else {
            muros.find { isPointNearLine(wx, wy, it) }
        }
        onElementSelected?.invoke(selectedElement, sx, sy); invalidate()
    }

    fun setElementos(l: List<Sala>) { elementos = l.toMutableList(); invalidate() }
    fun setMuros(l: List<Muro>) { 
        muros = l.toMutableList()
        mergeCollinearMuros()
        invalidate() 
    }

    fun setMode(m: EditorMode) { 
        mode = m
        if (m == EditorMode.DRAW_WALL) { 
            selectedElement = null
            onElementSelected?.invoke(null, 0f, 0f) 
        }
        invalidate() 
    }
    fun setOnElementSelectedListener(l: (Sala?, Float, Float) -> Unit) { onElementSelected = l }
    fun setOnElementMovedOrResizedListener(l: (Sala) -> Unit) { onElementMovedOrResized = l }
    fun setOnElementScreenPositionChangedListener(l: (Sala, Float, Float) -> Unit) { onElementScreenPositionChanged = l }

    private fun saveState() {
        if (history.size >= 50) history.removeAt(0)
        history.add(PlanoState(muros.map { it.copy() }, elementos.map { it.copy() }))
    }
    fun undo() {
        if (history.isNotEmpty()) {
            val last = history.removeAt(history.size - 1); muros.clear(); muros.addAll(last.muros); elementos.clear(); elementos.addAll(last.elementos)
            selectedElement = null; onElementSelected?.invoke(null,0f,0f); mode = EditorMode.SELECT; invalidate()
        }
    }
    fun addElement(e: Sala) { saveState(); elementos.add(e); selectedElement = e; invalidate() }

    fun syncToViewModel(viewModel: com.example.tfg_kotlin.ui.viewmodel.CreacionViewModel) {
        viewModel.syncSalas(elementos)
        viewModel.pisoActual.value?.let { piso ->
            piso.muros = muros.toMutableList()
        }
    }

    fun hasOpenSalas(): Boolean = elementos.any { it.tipo == TipoElemento.SALA.valor && !isSalaEnclosed(it) }

    private fun isSalaEnclosed(sala: Sala): Boolean {
        if (sala.tipo == TipoElemento.PUESTO.valor) return true
        if (sala.vertices.isEmpty()) return true 
        
        for (i in sala.vertices.indices) {
            val v1 = sala.vertices[i]
            val v2 = sala.vertices[(i + 1) % sala.vertices.size]
            val x1 = (sala.x + v1.x).snapToGrid(gridSize/2f)
            val y1 = (sala.y + v1.y).snapToGrid(gridSize/2f)
            val x2 = (sala.x + v2.x).snapToGrid(gridSize/2f)
            val y2 = (sala.y + v2.y).snapToGrid(gridSize/2f)
            
            val segmentEnclosed = muros.any { m ->
                // Check if segment (x1,y1)-(x2,y2) is part of muro m
                isSegmentOnMuro(x1, y1, x2, y2, m)
            }
            if (!segmentEnclosed) return false
        }
        return true
    }

    private fun isSegmentOnMuro(x1: Float, y1: Float, x2: Float, y2: Float, m: Muro): Boolean {
        if (!isPointOnMuroInternal(x1, y1, m)) return false
        if (!isPointOnMuroInternal(x2, y2, m)) return false
        
        // Collinear check
        val sSegment = atan2(y2 - y1, x2 - x1)
        val sMuro = atan2(m.y2 - m.y1, m.x2 - m.x1)
        return areSlopesSame(sSegment, sMuro)
    }

    private fun isPointOnMuroInternal(px: Float, py: Float, m: Muro): Boolean {
        val dAP = sqrt(distSq(px, py, m.x1, m.y1))
        val dPB = sqrt(distSq(px, py, m.x2, m.y2))
        val dAB = sqrt(distSq(m.x1, m.y1, m.x2, m.y2))
        return abs(dAP + dPB - dAB) < 5f 
    }
}
