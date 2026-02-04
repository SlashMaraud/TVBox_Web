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

        // CONFIGURACIÓN PARA EVITAR EL "CARTELITO" DE ERROR
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            // Disfraz estable de PC para TV
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            // IGNORAR ERRORES QUE LANZAN EL CARTEL RÁPIDO
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                // Si el error es por el script de Cloudflare, lo ignoramos para que no reinicie el bucle
                super.onReceivedError(view, request, error)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()
                headers.remove("X-Requested-With") 
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                super.onPageFinished(view, url)
            }
        }

        webView.loadUrl(DEFAULT_URL)

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
                    clicConPausa()
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

    private fun clicConPausa() {
        val time = android.os.SystemClock.uptimeMillis()
        // Enviamos el "Presionar"
        webView.dispatchTouchEvent(android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, cursorX, cursorY, 0))
        
        // Esperamos 150ms antes de "Soltar" para que el sistema registre el contacto
        Handler(Looper.getMainLooper()).postDelayed({
            val endTime = android.os.SystemClock.uptimeMillis()
            webView.dispatchTouchEvent(android.view.MotionEvent.obtain(endTime, endTime, android.view.MotionEvent.ACTION_UP, cursorX, cursorY, 0))
            CookieManager.getInstance().flush()
        }, 150)
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
