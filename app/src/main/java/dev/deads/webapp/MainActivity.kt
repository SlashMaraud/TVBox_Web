package dev.deads.webapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

        // 1. LIMPIEZA TOTAL AL INICIAR
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            // Disfraz de Safari en Mac (Muy efectivo contra Cloudflare)
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()
                // Borramos cualquier rastro de que esto es una App
                headers.remove("X-Requested-With")
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Inyectamos script para simular un navegador real
                view?.evaluateJavascript("""
                    Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                    window.chrome = { runtime: {} };
                    Object.defineProperty(navigator, 'languages', {get: () => ['es-ES', 'es']});
                """.trimIndent(), null)
                super.onPageFinished(view, url)
            }
        }

        webView.loadUrl(DEFAULT_URL)

        // --- PUNTERO ---
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
                    enviarClicSuave()
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

    private fun enviarClicSuave() {
        val time = SystemClock.uptimeMillis()
        // Enviamos el clic con un ligero retraso para simular dedo humano
        webView.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0))
        Handler(Looper.getMainLooper()).postDelayed({
            webView.dispatchTouchEvent(MotionEvent.obtain(time + 80, time + 80, MotionEvent.ACTION_UP, cursorX, cursorY, 0))
        }, 80)
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
