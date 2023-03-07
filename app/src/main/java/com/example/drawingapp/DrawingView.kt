package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context : Context,attrs : AttributeSet) : View(context,attrs) {
     private var drawPath : CustomPath? = null
     private var canvasBitmap : Bitmap? = null
     private var drawPaint : Paint? = null
    private var canvasPaint : Paint? = null
    private var brushSize : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas : Canvas? = null

    private var paths = ArrayList<CustomPath>()
    private var undoPaths = ArrayList<CustomPath>()


    init {
        setUpDrawing()
    }
    fun onClickUndo(){
        if(paths.size > 0){
            undoPaths.add(paths.removeAt(paths.size-1))
            //it means redraw on the screen
            invalidate() // calls overriden methods
        }
    }

    fun onClickRedo() {
        if(paths.size > 0){
            paths.add(undoPaths.get(undoPaths.size - 1))
            invalidate()
        }
    }
    private fun setUpDrawing(){
        drawPaint = Paint()
        drawPath = CustomPath(color,brushSize)
        drawPaint!!.color = color //non null asserted call
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        //brushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        canvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!,0f,0f,canvasPaint)
        for(path in paths){
            drawPaint!!.strokeWidth = path.brushThickness
            drawPaint!!.color = path.color
            canvas.drawPath(path,drawPaint!!)
        }
        if(!drawPath!!.isEmpty){
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color
            canvas.drawPath(drawPath!!,drawPaint!!)

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = color
                drawPath!!.brushThickness = brushSize

                drawPath!!.reset()
                drawPath!!.moveTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath!!.lineTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_UP ->{
                paths.add(drawPath!!)
                drawPath = CustomPath(color,brushSize)
            }
            else -> return false
        }
        invalidate()
        return true

    }
    fun setSizeForBrush(newSize : Float){
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        drawPaint!!.strokeWidth = brushSize
    }

    fun setColorForBrush(newcolor : String){
        color = Color.parseColor(newcolor)
        drawPaint!!.color = color
    }



    internal inner class CustomPath(var color : Int, var brushThickness : Float) : Path(){}
}