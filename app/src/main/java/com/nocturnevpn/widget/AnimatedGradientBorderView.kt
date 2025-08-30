package com.nocturnevpn.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.animation.ValueAnimator

class AnimatedGradientBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradient: LinearGradient? = null
    private var phase = 0f
    private val borderWidth = 8f
    private val cornerRadius = 32f
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (gradient == null) {
            android.util.Log.d("AnimatedBorderTest", "onDraw: gradient is null, not drawing border")
            return // Do not draw border if not animating
        }
        android.util.Log.d("AnimatedBorderTest", "onDraw: drawing border")
        val rect = RectF(
            borderWidth / 2,
            borderWidth / 2,
            width - borderWidth / 2,
            height - borderWidth / 2
        )
        borderPaint.shader = gradient
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    override fun setWillNotDraw(willNotDraw: Boolean) {
        super.setWillNotDraw(willNotDraw)
        invalidate()
    }

    fun startBorderAnimation() {
        if (width == 0) {
            // Wait until layout is done
            post { startBorderAnimation() }
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, width.toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedValue as Float
                // Update the gradient here!
                gradient = LinearGradient(
                    phase, 0f, width.toFloat() + phase, height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#6622CC"),
                        Color.parseColor("#22CCC2"),
                        Color.parseColor("#6622CC")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.MIRROR
                )
                invalidate()
            }
            start()
        }
    }

    fun stopBorderAnimation() {
        animator?.cancel()
        animator = null
        gradient = null // Remove the shader
        invalidate()
    }
    
    fun isAnimating(): Boolean {
        return animator != null && animator!!.isRunning
    }
    
    fun hasGradient(): Boolean {
        return gradient != null
    }
} 