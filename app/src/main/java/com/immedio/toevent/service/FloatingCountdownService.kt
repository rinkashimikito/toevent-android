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
import android.widget.LinearLayout
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
    private var container: LinearLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private var events: List<EventData> = emptyList()
    private var urgencyThresholds = UrgencyThresholds.DEFAULT
    private var privacyMode = false

    private data class EventData(
        val id: String, val title: String, val startDate: Long,
        val endDate: Long, val meetingUrl: String?,
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateDisplay()
            val nextStart = events.firstOrNull()?.startDate
            val remaining = nextStart?.let { (it - System.currentTimeMillis()) / 1000.0 } ?: 60.0
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
            ACTION_START, ACTION_UPDATE -> {
                val ids = intent.getStringArrayExtra("event_ids") ?: arrayOf()
                val titles = intent.getStringArrayExtra("event_titles") ?: arrayOf()
                val starts = intent.getLongArrayExtra("event_starts") ?: longArrayOf()
                val ends = intent.getLongArrayExtra("event_ends") ?: longArrayOf()
                val urls = intent.getStringArrayExtra("event_urls") ?: arrayOf()

                events = ids.indices.map { i ->
                    EventData(
                        id = ids[i],
                        title = titles.getOrElse(i) { "Event" },
                        startDate = starts.getOrElse(i) { 0 },
                        endDate = ends.getOrElse(i) { 0 },
                        meetingUrl = urls.getOrElse(i) { null }?.takeIf { it.isNotEmpty() },
                    )
                }

                privacyMode = intent.getBooleanExtra("privacy_mode", false)
                urgencyThresholds = UrgencyThresholds(
                    intent.getDoubleExtra("urgency_imminent", 300.0),
                    intent.getDoubleExtra("urgency_soon", 900.0),
                    intent.getDoubleExtra("urgency_approaching", 1800.0),
                )

                if (intent.action == ACTION_START) {
                    showFloatingChip()
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                } else {
                    updateDisplay()
                }
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * density).toInt(), (10 * density).toInt(),
                (14 * density).toInt(), (10 * density).toInt(),
            )
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(Color.parseColor("#E0000000"))
            }
            alpha = 0.94f
        }
        container = root

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

        // Draggable
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(root, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(event.rawX - initialTouchX) < 10 && abs(event.rawY - initialTouchY) < 10) {
                        events.firstOrNull()?.let { evt ->
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

        floatingView = root
        windowManager?.addView(root, params)
        updateDisplay()
    }

    private fun updateDisplay() {
        val root = container ?: return
        root.removeAllViews()

        val density = resources.displayMetrics.density
        val now = System.currentTimeMillis()

        // Remove ended events
        val active = events.filter { it.endDate > now }
        if (active.isEmpty()) {
            removeFloatingChip()
            stopSelf()
            return
        }

        // Urgency color from first event
        val firstRemaining = (active[0].startDate - now) / 1000.0
        val urgency = UrgencyLevel.from(firstRemaining, urgencyThresholds)
        val bgColor = when (urgency) {
            UrgencyLevel.NORMAL -> "#E0000000"
            UrgencyLevel.APPROACHING -> "#E0795900"
            UrgencyLevel.SOON -> "#E0CC5500"
            UrgencyLevel.IMMINENT, UrgencyLevel.NOW -> "#E0CC0000"
        }
        (root.background as? GradientDrawable)?.setColor(Color.parseColor(bgColor))

        // Show up to 3 events
        for ((i, evt) in active.take(3).withIndex()) {
            val remaining = (evt.startDate - now) / 1000.0
            val countdown = if (remaining <= 0) "Now" else DateFormatters.formatHybridCountdown(remaining)
            val title = if (privacyMode) "Busy" else evt.title

            if (i > 0) {
                // Separator
                val sep = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (0.5f * density).toInt(),
                    ).apply { topMargin = (4 * density).toInt(); bottomMargin = (4 * density).toInt() }
                    setBackgroundColor(Color.parseColor("#40FFFFFF"))
                }
                root.addView(sep)
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            }

            val countdownTv = TextView(this).apply {
                text = countdown
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (i == 0) 16f else 13f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginEnd = (8 * density).toInt() }
            }

            val titleTv = TextView(this).apply {
                text = title
                setTextColor(Color.parseColor(if (i == 0) "#FFFFFF" else "#B0FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (i == 0) 12f else 11f)
                maxLines = 1
                maxWidth = (140 * density).toInt()
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            row.addView(countdownTv)
            row.addView(titleTv)
            root.addView(row)
        }

        // Show "+N more" if there are more
        if (active.size > 3) {
            val more = TextView(this).apply {
                text = "+${active.size - 3} more"
                setTextColor(Color.parseColor("#80FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = (2 * density).toInt() }
            }
            root.addView(more)
        }
    }

    private fun removeFloatingChip() {
        handler.removeCallbacks(tickRunnable)
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
        container = null
    }

    override fun onDestroy() {
        removeFloatingChip()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.immedio.toevent.FLOATING_START"
        const val ACTION_UPDATE = "com.immedio.toevent.FLOATING_UPDATE"
        const val ACTION_STOP = "com.immedio.toevent.FLOATING_STOP"

        fun startIntent(
            context: Context, events: List<Event>,
            privacyMode: Boolean, thresholds: UrgencyThresholds,
        ): Intent {
            return Intent(context, FloatingCountdownService::class.java).apply {
                action = ACTION_START
                putExtra("event_ids", events.map { it.id }.toTypedArray())
                putExtra("event_titles", events.map { it.title }.toTypedArray())
                putExtra("event_starts", events.map { it.startDate }.toLongArray())
                putExtra("event_ends", events.map { it.endDate }.toLongArray())
                putExtra("event_urls", events.map { it.meetingUrl ?: "" }.toTypedArray())
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
