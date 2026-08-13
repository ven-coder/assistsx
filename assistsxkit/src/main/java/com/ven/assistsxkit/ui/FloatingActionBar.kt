package com.ven.assistsxkit.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import com.ven.assistsxkit.R
import kotlin.math.abs

class FloatingActionBar(context: Context) : FrameLayout(context) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    private val fabSize = dp(48)
    private val barHeight = dp(48)
    private val touchSlop = dp(8)
    private val autoCollapseDelay = 5000L
    private val snapDuration = 200L

    private var isExpanded = false
    private var isOnLeftEdge = false
    private var isClick = false
    private var isDragging = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var downTransX = 0f
    private var downTransY = 0f
    private var screenWidth = 0
    private var decorViewRef: FrameLayout? = null

    private val fabContainer: FrameLayout
    private val fabIcon: ImageView
    private val expandedBar: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val collapseRunnable = Runnable { collapse() }

    var onCloseClick: (() -> Unit)? = null
    var onBackClick: (() -> Unit)? = null
    var onForwardClick: (() -> Unit)? = null
    var onRefreshClick: (() -> Unit)? = null

    init {
        fabIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_24)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER
        }

        fabContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(fabSize, fabSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#23252A".toColorInt())
            }
            addView(fabIcon, LayoutParams(fabSize, fabSize))
        }

        expandedBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, barHeight)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor("#23252A".toColorInt())
            }
            gravity = Gravity.CENTER_VERTICAL
            val paddingH = dp(4)
            setPadding(paddingH, 0, paddingH, 0)
        }

        addView(fabContainer)
        addView(expandedBar)
        expandedBar.visibility = GONE

        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun attachToActivity(activity: Activity) {
        val decorView = activity.window.decorView as FrameLayout
        decorView.addView(this, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        decorViewRef = decorView

        val metrics = activity.resources.displayMetrics
        screenWidth = metrics.widthPixels

        isOnLeftEdge = true
        post {
            val rect = visibleRect()
            translationX = 0f
            translationY = (rect.bottom - fabSize).toFloat()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        animate().cancel()
        expandedBar.visibility = GONE
        fabContainer.visibility = VISIBLE
        cancelAutoCollapse()
        snapToEdge()
    }

    private fun expand() {
        if (isExpanded) return
        isExpanded = true
        fabContainer.visibility = GONE
        expandedBar.visibility = VISIBLE
        rebuildBarButtons()
        post {
            snapToEdge()
            startAutoCollapse()
        }
    }

    private fun rebuildBarButtons() {
        expandedBar.removeAllViews()
        val collapseIcon = if (isOnLeftEdge) R.drawable.que else R.drawable.ic_arrow_forward
        val collapse = Triple("collapse", collapseIcon, { collapse() })
        val back = Triple("back", R.drawable.ic_arrow_back_bar_24, { onBackClick?.invoke(); restartAutoCollapse() })
        val forward = Triple("forward", R.drawable.ic_arrow_forward_bar_24, { onForwardClick?.invoke(); restartAutoCollapse() })
        val refresh = Triple("refresh", R.drawable.ic_refresh_circle_24, { onRefreshClick?.invoke(); restartAutoCollapse() })
        val close = Triple("close", R.drawable.ic_exit_24, { onCloseClick?.invoke(); restartAutoCollapse() })
        // The bar grows rightward from a left-edge anchor and leftward from a
        // right-edge anchor, so the collapse button must sit nearest the anchor
        // edge. On the right, close/refresh mirror the left bar's outer end, with
        // refresh directly to the right of close; back/forward keep their fixed
        // left-to-right order on both sides.
        val ordered = if (isOnLeftEdge) {
            listOf(collapse, back, forward, refresh, close)
        } else {
            listOf(close, refresh, back, forward, collapse)
        }
        for ((tag, iconRes, action) in ordered) {
            val btn = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), barHeight)
                setImageResource(iconRes)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setOnClickListener { action() }
                this.tag = tag
            }
            expandedBar.addView(btn)
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animate().cancel()
                isClick = true
                isDragging = false
                downRawX = event.rawX
                downRawY = event.rawY
                downTransX = translationX
                downTransY = translationY
                cancelAutoCollapse()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.rawX - downRawX)
                val dy = abs(event.rawY - downRawY)
                if (dx > touchSlop || dy > touchSlop) {
                    isClick = false
                    isDragging = true
                }
                if (isDragging) {
                    val (minY, maxY) = yBounds()
                    translationX = (downTransX + event.rawX - downRawX)
                        .coerceIn(0f, (screenWidth - width).toFloat())
                    translationY = (downTransY + event.rawY - downRawY).coerceIn(minY, maxY)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isClick) {
                    if (isExpanded) {
                        dispatchClickToBarChild(event)
                    } else {
                        expand()
                    }
                }
                if (!isClick || isExpanded) {
                    snapToEdge(recomputeEdge = !isExpanded)
                }
                if (isExpanded) {
                    startAutoCollapse()
                }
                return true
            }
        }
        return false
    }

    private fun dispatchClickToBarChild(event: MotionEvent) {
        val localX = event.x
        val localY = event.y
        for (i in 0 until expandedBar.childCount) {
            val child = expandedBar.getChildAt(i)
            val rect = Rect()
            child.getHitRect(rect)
            if (rect.contains(localX.toInt(), localY.toInt())) {
                child.performClick()
                break
            }
        }
    }

    private fun snapToEdge(recomputeEdge: Boolean = false) {
        // Only re-derive the edge from the FAB's own position while it is collapsed
        // and draggable. When the bar is expanded (or collapsing), keep the edge
        // recorded at expand time so the FAB returns to the side it expanded from
        // instead of being nudged across by the bar's larger width.
        if (recomputeEdge) {
            val centerX = translationX + fabSize / 2f
            isOnLeftEdge = centerX < screenWidth / 2f
        }

        val viewWidth = if (isExpanded) expandedBar.width else fabSize
        val targetX = if (isOnLeftEdge) {
            0f
        } else {
            (screenWidth - viewWidth).toFloat()
        }

        val (minY, maxY) = yBounds()
        val targetY = translationY.coerceIn(minY, maxY)

        animate()
            .translationX(targetX)
            .translationY(targetY)
            .setDuration(snapDuration)
            .start()
    }

    private fun visibleRect(): Rect {
        val rect = Rect()
        decorViewRef?.getWindowVisibleDisplayFrame(rect)
        return rect
    }

    private fun yBounds(): Pair<Float, Float> {
        // Bounds come from the decorView's visible frame (the FAB's coordinate
        // space), not the physical screen size — the window may be inset by the
        // status bar and nav bar, so screenHeight overshoots the real area.
        val rect = visibleRect()
        val minY = rect.top.toFloat()
        val maxY = (rect.bottom - height).toFloat().coerceAtLeast(minY)
        return minY to maxY
    }

    private fun startAutoCollapse() {
        cancelAutoCollapse()
        handler.postDelayed(collapseRunnable, autoCollapseDelay)
    }

    private fun restartAutoCollapse() {
        if (isExpanded) {
            startAutoCollapse()
        }
    }

    private fun cancelAutoCollapse() {
        handler.removeCallbacks(collapseRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAutoCollapse()
    }
}
