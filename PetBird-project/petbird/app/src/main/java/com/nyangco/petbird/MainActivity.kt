package com.nyangco.petbird

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var toggleButton: Button
    private var petRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)
        toggleButton = findViewById(R.id.toggleButton)

        permissionButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        toggleButton.setOnClickListener {
            if (!petRunning) {
                startService(Intent(this, OverlayService::class.java))
                petRunning = true
                toggleButton.text = getString(R.string.stop_pet)
            } else {
                stopService(Intent(this, OverlayService::class.java))
                petRunning = false
                toggleButton.text = getString(R.string.start_pet)
            }
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun refreshUi() {
        val granted = hasOverlayPermission()
        permissionButton.isEnabled = !granted
        toggleButton.isEnabled = granted
        statusText.text = if (granted) {
            "권한 허용 완료! 아래 버튼으로 펫을 시작하세요 🐣"
        } else {
            getString(R.string.permission_needed)
        }
    }
}
