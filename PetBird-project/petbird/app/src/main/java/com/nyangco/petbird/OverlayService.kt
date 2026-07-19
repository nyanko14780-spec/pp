package com.nyangco.petbird

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.random.Random

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var petView: View? = null
    private var memeView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private val memeImages = listOf(
        R.drawable.meme1,
        R.drawable.meme2,
        R.drawable.meme3,
        R.drawable.meme4,
        R.drawable.meme5
    )

    companion object {
        const val CHANNEL_ID = "petbird_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceCompat()
        addPetView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundServiceCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "냥코 펫",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("냥코 펫이 화면 위에서 놀고 있어요")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun windowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun addPetView() {
        val inflater = LayoutInflater.from(this)
        petView = inflater.inflate(R.layout.overlay_pet, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDrag = false

        petView!!.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) isDrag = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(petView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDrag) {
                        showRandomMeme(params.x, params.y)
                        bounceView(view)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(petView, params)
        startIdleWander(params)
    }

    private fun bounceView(view: View) {
        view.animate()
            .scaleX(1.25f).scaleY(1.25f)
            .setDuration(120)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
    }

    private fun showRandomMeme(petX: Int, petY: Int) {
        removeMemeView()

        val inflater = LayoutInflater.from(this)
        memeView = inflater.inflate(R.layout.overlay_meme, null)
        val imageView = memeView!!.findViewById<ImageView>(R.id.memeImage)
        imageView.setImageResource(memeImages[Random.nextInt(memeImages.size)])

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = petX
        params.y = (petY - 160).coerceAtLeast(0)

        memeView!!.alpha = 0f
        memeView!!.scaleX = 0.5f
        memeView!!.scaleY = 0.5f
        windowManager.addView(memeView, params)
        memeView!!.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()

        handler.postDelayed({
            memeView?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
                removeMemeView()
            }?.start()
        }, 1800)
    }

    private fun removeMemeView() {
        memeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // view already removed, ignore
            }
        }
        memeView = null
    }

    // 가끔 살짝 움찔움찔 움직여서 살아있는 느낌 주기
    private fun startIdleWander(params: WindowManager.LayoutParams) {
        val wanderRunnable = object : Runnable {
            override fun run() {
                petView?.let { view ->
                    view.animate()
                        .translationYBy(-12f)
                        .setDuration(300)
                        .withEndAction {
                            view.animate().translationYBy(12f).setDuration(300).start()
                        }.start()
                }
                handler.postDelayed(this, Random.nextLong(4000, 9000))
            }
        }
        handler.postDelayed(wanderRunnable, Random.nextLong(3000, 6000))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        petView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        removeMemeView()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
