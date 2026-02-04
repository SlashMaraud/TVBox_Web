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
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private lateinit var rootLayout: FrameLayout
    private var customView: View? = null
    
    private val DEFAULT_URL = "https://hdfull.one/"
    private var cursorX = 640f
    private var cursorY = 360f
    private val step = 45f 

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { cursorView.visibility = View.GONE }
    private val CURSOR_TIMEOUT = 3000L 

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        rootLayout = findViewById(R.id.rootLayout)
        webView = findViewById(R.id.webView)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // User Agent de Chrome estable para evitar sospechas
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            // ELIMINAR EL RASTRO DE APP ANDROID (CLAVE)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()
                headers.remove("X-Requested-With")
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                cookieManager.flush()
                // Engaño para ocultar que es una automatización
                view?.evaluateJavascript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})", null)
                super.onPageFinished(view, url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) return
                customView = view
                rootLayout.addView(customView, FrameLayout.LayoutParams(-1, -1))
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                if (customView == null) return
                rootLayout.removeView(customView)
                customView = null
                webView.visibility = View.VISIBLE
            }
        }

        webView.loadUrl(DEFAULT_URL)

        // Puntero Rojo
        cursorView = View(this).apply {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(Color.RED)
            shape.setStroke(3, Color.WHITE)
            background = shape
            layoutParams = FrameLayout.LayoutParams(30, 30)
        }
        rootLayout.addView(cursorView)
        showCursorTemporarily()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            showCursorTemporarily()
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (cursorY < 100f && customView == null) webView.scrollBy(0, -300) else cursorY -= step
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (cursorY > (rootLayout.height - 150f) && customView == null) webView.scrollBy(0, 300) else cursorY += step
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    enviarClicReal()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (customView != null) webView.webChromeClient?.onHideCustomView()
                    else if (webView.canGoBack()) webView.goBack()
                    return true
                }
            }
            updateCursor()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enviarClicReal() {
        val time = SystemClock.uptimeMillis()
        
        // 1. Simular que el ratón "entra" en el área (Hover)
        val hoverEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_HOVER_MOVE, cursorX, cursorY, 0)
        
        // 2. Simular Presionar
        val downEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        
        // 3. Simular Soltar
        val upEvent = MotionEvent.obtain(time + 100, time + 100, MotionEvent.ACTION_UP, cursorX, cursorY, 0)

        if (customView != null) {
            customView?.dispatchTouchEvent(downEvent)
            customView?.dispatchTouchEvent(upEvent)
        } else {
            webView.dispatchGenericMotionEvent(hoverEvent)
            webView.dispatchTouchEvent(downEvent)
            webView.dispatchTouchEvent(upEvent)
        }
    }

    private fun showCursorTemporarily() {
        cursorView.visibility = View.VISIBLE
        cursorView.bringToFront()
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, CURSOR_TIMEOUT)
    }

    private fun updateCursor() {
        cursorX = cursorX.coerceIn(0f, rootLayout.width.toFloat())
        cursorY = cursorY.coerceIn(0f, rootLayout.height.toFloat())
        cursorView.x = cursorX
        cursorView.y = cursorY
    }
}
