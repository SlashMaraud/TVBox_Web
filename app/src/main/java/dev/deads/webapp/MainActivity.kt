package dev.deads.webapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private lateinit var rootLayout: FrameLayout
    
    private val DEFAULT_URL = "https://hdfull.one/"
    private var cursorX = 640f
    private var cursorY = 360f
    private val step = 45f 

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { cursorView.visibility = View.GONE }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        rootLayout = findViewById(R.id.rootLayout)
        webView = findViewById(R.id.webView)

        // CONFIGURACIÓN DE MOTOR DE ALTO RENDIMIENTO
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // FORZAR MODO ESCRITORIO REAL
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // DISFRAZ DE NAVEGADOR DE ALTA SEGURIDAD
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
            
            // Permitir que Cloudflare use todas las herramientas de red
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // ELIMINAR EL RASTRO DE "WEBVIEW" (La huella digital)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()
                // Si estas cabeceras existen, Cloudflare sabe que es un bot
                headers.remove("X-Requested-With")
                headers.remove("sec-ch-ua-mobile")
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Inyectamos un "Parche de Humano" directo en el corazón de la página
                view?.evaluateJavascript("""
                    (function() {
                        window.chrome = { runtime: {} };
                        Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                        Object.defineProperty(navigator, 'languages', {get: () => ['es-ES', 'es']});
                    })();
                """.trimIndent(), null)
                cookieManager.flush()
                super.onPageFinished(view, url)
            }
        }

        webView.loadUrl(DEFAULT_URL)

        // DISEÑO DEL PUNTERO
        cursorView = View(this).apply {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(Color.RED)
            shape.setStroke(3, Color.WHITE)
            background = shape
            layoutParams = FrameLayout.LayoutParams(30, 30)
        }
        rootLayout.addView(cursorView)
        showCursor()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            showCursor()
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (cursorY < 100f) webView.scrollBy(0, -300) else cursorY -= step
                KeyEvent.KEYCODE_DPAD_DOWN -> if (cursorY > (rootLayout.height - 150f)) webView.scrollBy(0, 300) else cursorY += step
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    enviarClicReal()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) webView.goBack() else finish()
                    return true
                }
            }
            updateCursor()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enviarClicReal() {
        val time = android.os.SystemClock.uptimeMillis()
        // Enviamos la secuencia que un ratón real haría
        webView.dispatchTouchEvent(android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, cursorX, cursorY, 0))
        Handler(Looper.getMainLooper()).postDelayed({
            val upTime = android.os.SystemClock.uptimeMillis()
            webView.dispatchTouchEvent(android.view.MotionEvent.obtain(upTime, upTime, android.view.MotionEvent.ACTION_UP, cursorX, cursorY, 0))
            CookieManager.getInstance().flush()
        }, 150) // 150ms es el tiempo exacto de un clic humano
    }

    private fun showCursor() {
        cursorView.visibility = View.VISIBLE
        cursorView.bringToFront()
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 3000)
    }

    private fun updateCursor() {
        cursorX = cursorX.coerceIn(0f, rootLayout.width.toFloat())
        cursorY = cursorY.coerceIn(0f, rootLayout.height.toFloat())
        cursorView.x = cursorX
        cursorView.y = cursorY
    }
}
