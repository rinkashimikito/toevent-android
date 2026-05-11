package com.immedio.toevent.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.UrgencyLevel
import com.immedio.toevent.domain.model.UrgencyThresholds
import com.immedio.toevent.util.DateFormatters
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class FloatingCountdownService : Service() {

    @Inject lateinit var notificationService: NotificationService

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var titleView: TextView? = null
    private var countdownView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var currentEvent: Event? = null
    private var urgencyThresholds = UrgencyThresholds.DEFAULT
    private var privacyMode = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateDisplay()
            val remaining = currentEvent?.let { (it.startDate - System.currentTimeMillis()) / 1000.0 } ?: 60.0
            val interval = if (remaining in 0.0..300.0) 1000L else 30_000L
            handler.postDelayed(this, interval)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentEvent = Event(
                    id = intent.getStringExtra("event_id") ?: "",
                    title = intent.getStringExtra("event_title") ?: "Event",
                    startDate = intent.getLongExtra("event_start", 0),
                    endDate = intent.getLongExtra("event_end", 0),
                    isAllDay = false,
                    calendarColor = 0,
                    calendarId = "",
                    calendarTitle = "",
                    meetingUrl = intent.getStringExtra("meeting_url"),
                )
                privacyMode = intent.getBooleanExtra("privacy_mode", false)
                val imm = intent.getDoubleExtra("urgency_imminent", 300.0)
                val soon = intent.getDoubleExtra("urgency_soon", 900.0)
                val app = intent.getDoubleExtra("urgency_approaching", 1800.0)
                urgencyThresholds = UrgencyThresholds(imm, soon, app)

                showFloatingChip()
                handler.removeCallbacks(tickRunnable)
                handler.post(tickRunnable)
            }
            ACTION_UPDATE -> {
                currentEvent = Event(
                    id = intent.getStringExtra("event_id") ?: "",
                    title = intent.getStringExtra("event_title") ?: "Event",
                    startDate = intent.getLongExtra("event_start", 0),
                    endDate = intent.getLongExtra("event_end", 0),
                    isAllDay = false,
                    calendarColor = 0,
                    calendarId = "",
                    calendarTitle = "",
                    meetingUrl = intent.getStringExtra("meeting_url"),
                )
                privacyMode = intent.getBooleanExtra("privacy_mode", false)
                updateDisplay()
            }
            ACTION_STOP -> {
                removeFloatingChip()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showFloatingChip() {
        if (floatingView != null) return

        val density = resources.displayMetrics.density

        // Container
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(
                (12 * density).toInt(),
                (6 * density).toInt(),
                (12 * density).toInt(),
                (6 * density).toInt(),
            )
            background = GradientDrawable().apply {
                cornerRadius = 20 * density
                setColor(Color.parseColor("#E0000000"))
            }
            alpha = 0.92f
        }

        titleView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        countdownView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        container.addView(titleView)
        container.addView(countdownView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (80 * density).toInt()
        }

        // Make draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // If barely moved, treat as tap -> open calendar
                    if (abs(event.rawX - initialTouchX) < 10 && abs(event.rawY - initialTouchY) < 10) {
                        currentEvent?.let { evt ->
                            val calIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("content://com.android.calendar/time/${evt.startDate}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(calIntent)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(container, params)
        updateDisplay()
    }

    private fun updateDisplay() {
        val event = currentEvent ?: return

        if (event.endDate <= System.currentTimeMillis()) {
            removeFloatingChip()
            stopSelf()
            return
        }

        val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
        val countdown = if (remaining <= 0) "Now" else DateFormatters.formatHybridCountdown(remaining)
        val urgency = UrgencyLevel.from(remaining, urgencyThresholds)

        val title = if (privacyMode) "Busy" else event.title
        titleView?.text = title
        countdownView?.text = countdown

        // Urgency color on background
        val bgColor = when (urgency) {
            UrgencyLevel.NORMAL -> "#E0000000"
            UrgencyLevel.APPROACHING -> "#E0795900"
            UrgencyLevel.SOON -> "#E0CC5500"
            UrgencyLevel.IMMINENT, UrgencyLevel.NOW -> "#E0CC0000"
        }
        (floatingView?.background as? GradientDrawable)?.setColor(Color.parseColor(bgColor))
    }

    private fun removeFloatingChip() {
        handler.removeCallbacks(tickRunnable)
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
        titleView = null
        countdownView = null
    }

    override fun onDestroy() {
        removeFloatingChip()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.immedio.toevent.FLOATING_START"
        const val ACTION_UPDATE = "com.immedio.toevent.FLOATING_UPDATE"
        const val ACTION_STOP = "com.immedio.toevent.FLOATING_STOP"

        fun startIntent(context: Context, event: Event, privacyMode: Boolean, thresholds: UrgencyThresholds): Intent {
            return Intent(context, FloatingCountdownService::class.java).apply {
                action = ACTION_START
                putExtra("event_id", event.id)
                putExtra("event_title", event.title)
                putExtra("event_start", event.startDate)
                putExtra("event_end", event.endDate)
                putExtra("meeting_url", event.meetingUrl)
                putExtra("privacy_mode", privacyMode)
                putExtra("urgency_imminent", thresholds.imminent)
                putExtra("urgency_soon", thresholds.soon)
                putExtra("urgency_approaching", thresholds.approaching)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, FloatingCountdownService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
