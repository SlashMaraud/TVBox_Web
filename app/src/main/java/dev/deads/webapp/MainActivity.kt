package dev.deads.webapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
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
    
    private val DEFAULT_URL = "https://hdfull.one"
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

        // Limpiar cookies viejas por si acaso hay un token corrupto
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true) // Importante para algunos captchas
            
            // DISFRAZ DE CHROME DE WINDOWS
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            // ESTA FUNCIÓN ES LA CLAVE: Borra el rastro de que es una APP de Android
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // No forzamos la carga, dejamos que el navegador fluya
                return false 
            }
        }
            
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                // Si la web intenta abrir una ventana de verificación, la cargamos en el mismo WebView
                val newWebView = WebView(this@MainActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        webView.loadUrl(request?.url.toString())
                        return true
                    }
                }
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                rootLayout.addView(customView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
                webView.visibility = View.GONE
                showCursorTemporarily()
            }

            override fun onHideCustomView() {
                if (customView == null) return
                rootLayout.removeView(customView)
                customView = null
                webView.visibility = View.VISIBLE
                showCursorTemporarily()
            }
        }

        webView.loadUrl(DEFAULT_URL)

        // PUNTERO
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
                    if (cursorY < 100f && customView == null) webView.scrollBy(0, -250) else cursorY -= step
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (cursorY > (rootLayout.height - 150f) && customView == null) webView.scrollBy(0, 250) else cursorY += step
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    enviarClic()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (customView != null) {
                        webView.webChromeClient?.onHideCustomView()
                        return true
                    }
                    if (webView.canGoBack()) {
                        webView.goBack()
                        return true
                    }
                }
            }
            updateCursor()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enviarClic() {
        val downTime = android.os.SystemClock.uptimeMillis()
        val downEvent = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val upEvent = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        
        if (customView != null) {
            customView?.dispatchTouchEvent(downEvent)
            customView?.dispatchTouchEvent(upEvent)
        } else {
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
        cursorView.bringToFront()
    }
}
