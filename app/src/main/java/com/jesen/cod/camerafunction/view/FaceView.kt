package com.jesen.cod.camerafunction.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class FaceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    lateinit var mPaint: Paint
    private var mColor = "#de4e5f"
    private var mFaces: ArrayList<RectF>? = null

    init {
     init()
    }

    private fun init() {
        mPaint = Paint()
        mPaint.color = Color.parseColor(mColor)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)
        mPaint.isAntiAlias = true
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mFaces?.let {
            for (face in it) {
                canvas.drawRect(face, mPaint)
            }
        }
    }

    fun setFaces(faces: ArrayList<RectF>) {
        this.mFaces = faces
        invalidate()
    }

}