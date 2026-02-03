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
import android.view.WindowManager
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
        
        // Aceleración máxima
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        
        setContentView(R.layout.activity_main)
        rootLayout = findViewById(R.id.rootLayout)
        webView = findViewById(R.id.webView)

        // Limpiar todo rastro anterior para evitar que el bucle se alimente de cookies viejas
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(false)
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            
            // Probamos con User Agent de Chrome en LINUX (suele ser más efectivo con Cloudflare)
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            // Inyectamos un pequeño script para ocultar que somos un WebView
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})", null)
                super.onPageFinished(view, url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                rootLayout.addView(customView, FrameLayout.LayoutParams(-1, -1))
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

        // Cargamos la web sin dejar rastro de que somos una APP
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
        showCursorTemporarily()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            showCursorTemporarily()
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (cursorY < 100f && customView == null) webView.scrollBy(0, -250) else cursorY -= step
                KeyEvent.KEYCODE_DPAD_DOWN -> if (cursorY > (rootLayout.height - 150f) && customView == null) webView.scrollBy(0, 250) else cursorY += step
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
        val downEvent = android.view.MotionEvent.obtain(downTime, downTime, 0, cursorX, cursorY, 0)
        val upEvent = android.view.MotionEvent.obtain(downTime, downTime, 1, cursorX, cursorY, 0)
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
