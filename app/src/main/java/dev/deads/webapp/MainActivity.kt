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
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private lateinit var rootLayout: FrameLayout
    private var customView: View? = null
    
    private val DEFAULT_URL = "https://pelisflix20.space/"
    private var cursorX = 640f
    private var cursorY = 360f
    private val step = 45f 

    // Temporizador para ocultar el cursor
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        cursorView.visibility = View.GONE
    }
    private val CURSOR_TIMEOUT = 3000L // 3 segundos

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            
            webViewClient = WebViewClient()
            
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customView = view
                    rootLayout.addView(customView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
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
            loadUrl(DEFAULT_URL)
        }
        rootLayout.addView(webView)

        cursorView = View(this).apply {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(Color.RED)
            shape.setStroke(3, Color.WHITE)
            background = shape
            layoutParams = FrameLayout.LayoutParams(30, 30)
        }
        rootLayout.addView(cursorView)

        setContentView(rootLayout)
        showCursorTemporarily() // Mostrar al inicio
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Cada vez que se pulsa una tecla, mostramos el cursor
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

    private fun showCursorTemporarily() {
        cursorView.visibility = View.VISIBLE
        cursorView.bringToFront()
        
        // Reiniciamos el temporizador de ocultación
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
