package com.utkualtas.likeview


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.FloatRange
import androidx.annotation.NonNull
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


public class LikeView : View {


    private val PARTICLES_COUNT_MIN = 12
    private val PARTICLES_COUNT_MAX = 15
    private val ANIMATION_DURATION = 300 //milliseconds
    private var mAnimationStartTime: Long = 0

    private val mContext: Context
    private lateinit var mPaint: Paint
    private var particleList: MutableList<Particle> = ArrayList()
    private lateinit var destRect: Rect
    private lateinit var likeImage: Bitmap
    private var isLiked = false
    private var isAnimationStart = false


    private var mLikedIconResId: Int = 0
    private var mUnLikedIconResId: Int = 0


    constructor(context: Context) : super(context) {
        this.mContext = context
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.mContext = context
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.mContext = context
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.mContext = context
        init(attrs)
    }

    @SuppressLint("Recycle")
    private fun init(attrs: AttributeSet?){

        val typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.LikeView)
        mLikedIconResId = typedArray.getResourceId(R.styleable.LikeView_iconActive, 0)
        mUnLikedIconResId = typedArray.getResourceId(R.styleable.LikeView_iconPassive, 0)

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        destRect = Rect()
        likeImage = BitmapFactory.decodeResource(mContext.resources, mLikedIconResId)

        destRect.top = 200
        destRect.left = 200
        destRect.bottom = destRect.top + likeImage.height - 50
        destRect.right = destRect.left + likeImage.width - 50
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = 100
        val desiredHeight = 500

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int
        val height: Int

        //Measure Width
        width = when (widthMode) {
            MeasureSpec.EXACTLY -> //Must be this size
                widthSize
            MeasureSpec.AT_MOST -> //Can't be bigger than...
                Math.min(desiredWidth, widthSize)
            else -> //Be whatever you want
                desiredWidth
        }

        //Measure Height
        height = when (heightMode) {
            MeasureSpec.EXACTLY -> //Must be this size
                heightSize
            MeasureSpec.AT_MOST -> //Can't be bigger than...
                Math.min(desiredHeight, heightSize)
            else -> //Be whatever you want
                desiredHeight
        }
        //MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isAnimationStart) {
            if (mAnimationStartTime == 0.toLong()) {
                mAnimationStartTime = System.currentTimeMillis()
            }
        }

        canvas.drawBitmap(likeImage, Rect(0, 0, likeImage.width, likeImage.height), destRect, null)




        drawParticle(canvas, destRect)

        if (isAnimationStart) {
            invalidate()
        }
    }

    private fun drawParticle(canvas: Canvas, mainRect: Rect) {
        val paint = Paint()
        for (particle in particleList) {
            val x = sin(particle.angle * PI / 180)
            val y = cos(particle.angle * PI / 180)
            val calc = calculateNextFrame(particle).currentDistance
            paint.color =
                changeColorAlpha(particle.color, calculateNextFrame(particle).currentAlpha)
            if (particle.targetDistance == calc) {
                particleList.remove(particle)
                if (particleList.size == 0) {
                    stopAnimation()
                }
                return
            }
            canvas.drawCircle(
                (particle.xPosition + particle.currentDistance * x).toFloat(),
                (particle.yPosition + particle.currentDistance * y).toFloat(), particle.size, paint
            )
            particleList[particleList.indexOf(particle)].currentDistance = calc
        }
    }

    private fun calculateNextFrame(particle: Particle): Calculated {
        val now = System.currentTimeMillis()
        val pathGone: Float = (now - mAnimationStartTime).toFloat() / ANIMATION_DURATION

        val currentDistance = if (pathGone < 1.0f) {
            particle.targetDistance * FastOutSlowInInterpolator().getInterpolation(pathGone)
        } else {
            particle.targetDistance
        }

        val currentAlpha: Int = if (pathGone < 1.0f) {
            255 - (particle.color.alpha * DecelerateInterpolator().getInterpolation(pathGone)).toInt() / 3 * 2
        } else {
            0
        }
        return Calculated(
            currentDistance = currentDistance,
            currentAlpha = currentAlpha
        )
    }

    private fun stopAnimation() {
        isAnimationStart = false
        mAnimationStartTime = 0
        invalidate()
    }

    private fun changeColorAlpha(color: Int, newAlpha: Int): Int {
        return Color.argb(newAlpha, color.red, color.green, color.blue)
    }


    private fun createRandomDots(mainRect: Rect) {
        clearParticles()
        val centerX = ((mainRect.left + mainRect.right) / 2).toFloat()
        val centerY = ((mainRect.top + mainRect.bottom) / 2).toFloat()
        val randomParticleCount = (PARTICLES_COUNT_MIN..PARTICLES_COUNT_MAX).random()

        for (i in 0..randomParticleCount) {
            particleList.add(
                Particle(
                    xPosition = centerX,
                    yPosition = centerY,
                    targetDistance = (60..70).random().toFloat(),
                    currentDistance = 0f,
                    size = (2..6).random().toFloat(),
                    angle = (0..360).random().toFloat(),
                    color = Color.argb(255, 228, 13, 86)
                )
            )
        }
    }


    private fun clearParticles() {
        particleList.clear()
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isAnimationStart = true
                isLiked = !isLiked
                likeImage = if (isLiked) {
                    BitmapFactory.decodeResource(
                        mContext.resources,
                        mLikedIconResId
                    )
                } else {
                    BitmapFactory.decodeResource(
                        mContext.resources,
                        mUnLikedIconResId
                    )
                }
                createRandomDots(mainRect = destRect)
                postInvalidate()

            }
        }
        return super.onTouchEvent(event)
    }


    fun setIcons(@NonNull likedIconResId: Int, @NonNull unlikedIconResId: Int) {
        this.mLikedIconResId = likedIconResId
        this.mUnLikedIconResId = unlikedIconResId
    }


}


data class Particle(
    var xPosition: Float,
    var yPosition: Float,
    var targetDistance: Float,
    var currentDistance: Float,
    var size: Float, @FloatRange(from = 0.0, to = 360.0) var angle: Float,
    var color: Int
)

data class Calculated(var currentDistance: Float, var currentAlpha: Int)