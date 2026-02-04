package dev.deads.webapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private lateinit var rootLayout: FrameLayout
    
    private val TARGET_URL = "https://hdfull.one/"
    private var cursorX = 640f
    private var cursorY = 360f

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de la interfaz
        rootLayout = FrameLayout(this)
        webView = WebView(this)
        rootLayout.addView(webView)
        setContentView(rootLayout)

        // Configuración del motor Web (Nativo)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            // Disfraz de Chrome Desktop para evitar bloqueos básicos
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        // Cargamos la web con el truco del Referer (venimos de Google)
        val headers = mutableMapOf<String, String>()
        headers["Referer"] = "https://www.google.com/"
        webView.loadUrl(TARGET_URL, headers)

        // Crear Puntero Rojo
        cursorView = View(this).apply {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(Color.RED)
            shape.setStroke(3, Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(30, 30)
            background = shape
        }
        rootLayout.addView(cursorView)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> cursorY -= 45
                KeyEvent.KEYCODE_DPAD_DOWN -> cursorY += 45
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= 45
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += 45
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    enviarClic()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) webView.goBack() else finish()
                    return true
                }
            }
            actualizarPuntero()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enviarClic() {
        val time = android.os.SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val up = MotionEvent.obtain(time + 50, time + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    private fun actualizarPuntero() {
        cursorView.x = cursorX
        cursorView.y = cursorY
    }
}
