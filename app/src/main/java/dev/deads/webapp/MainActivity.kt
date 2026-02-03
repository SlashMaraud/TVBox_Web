package dev.deads.webapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private lateinit var sharedPreferences: SharedPreferences
    
    private val DEFAULT_URL = "https://pelisflix20.space/"
    private var cursorX = 640f
    private var cursorY = 360f
    private val step = 45f // Velocidad de movimiento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creamos un contenedor que permite poner cosas una encima de otra
        val root = FrameLayout(this)
        
        // 1. Configuramos la Web
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            webViewClient = WebViewClient()
            loadUrl(DEFAULT_URL)
        }
        root.addView(webView)

        // 2. Creamos el PUNTO ROJO como una pieza real de Android
        cursorView = View(this).apply {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(Color.RED)
            shape.setStroke(3, Color.WHITE)
            background = shape
            layoutParams = FrameLayout.LayoutParams(25, 25)
        }
        root.addView(cursorView)

        setContentView(root)
        updateCursor()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> cursorY -= step
                KeyEvent.KEYCODE_DPAD_DOWN -> cursorY += step
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    // El truco del clic simulado
                    val downTime = android.os.SystemClock.uptimeMillis()
                    val downEvent = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
                    val upEvent = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_UP, cursorX, cursorY, 0)
                    webView.dispatchTouchEvent(downEvent)
                    webView.dispatchTouchEvent(upEvent)
                    return true
                }
            }
            updateCursor()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updateCursor() {
        cursorView.x = cursorX
        cursorView.y = cursorY
    }
}
