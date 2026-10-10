package com.arbin.panel

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var passwordField: EditText
    private lateinit var loginBtn: Button
    private lateinit var launchBtn: Button
    private lateinit var shizukuStatus: TextView

    private var projectionResultCode = 0
    private var projectionData: Intent? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK && res.data != null) {
            projectionResultCode = res.resultCode
            projectionData = res.data
            launchOverlay()
        } else {
            toast("capture denied")
            launchOverlay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordField = findViewById(R.id.passwordField)
        loginBtn      = findViewById(R.id.btnLogin)
        launchBtn     = findViewById(R.id.btnLaunch)
        shizukuStatus = findViewById(R.id.shizukuStatus)

        ShizukuHelper.requestPermission()
        refreshShizukuStatus()

        loginBtn.setOnClickListener {
            if (!ShizukuHelper.isRunning()) {
                shizukuStatus.text = "shizuku not running"
                toast("start shizuku first")
                return@setOnClickListener
            }
            if (!ShizukuHelper.hasPermission()) {
                ShizukuHelper.requestPermission()
                toast("grant shizuku permission, tap login again")
                return@setOnClickListener
            }

            val pass = passwordField.text.toString().trim()
            if (pass != "Arbin.194") {
                toast("wrong password")
                return@setOnClickListener
            }

            shizukuStatus.text = "shizuku OK — logged in"
            loginBtn.isEnabled = false
            passwordField.isEnabled = false
            launchBtn.isEnabled = true
            launchBtn.alpha = 1.0f
            toast("welcome")
        }

        launchBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                if (!ShizukuHelper.grantOverlay()) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                    toast("grant overlay, tap again")
                    return@setOnClickListener
                }
            }
            requestProjection()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshShizukuStatus()
    }

    private fun refreshShizukuStatus() {
        shizukuStatus.text = when {
            !ShizukuHelper.isRunning()     -> "shizuku not running"
            !ShizukuHelper.hasPermission() -> "shizuku running — permission needed"
            else                            -> "shizuku OK"
        }
        shizukuStatus.setTextColor(
            if (ShizukuHelper.hasPermission()) 0xFF00E676.toInt() else 0xFF888888.toInt()
        )
    }

    private fun requestProjection() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun launchOverlay() {
        val i = Intent(this, OverlayService::class.java).apply {
            putExtra("code", projectionResultCode)
            putExtra("data", projectionData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
        moveTaskToBack(true)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
