package dev.deads.webapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
    
    private val DEFAULT_URL = "https://pelisflix20.space/"
    private var cursorX = 640f
    private var cursorY = 360f
    private val step = 45f 

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. SALTAMOS LA SEGURIDAD DEL SISTEMA: Forzamos la pantalla completa a nivel de ventana de Android
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // Mantenemos la pantalla siempre encendida para que no se apague en mitad de la peli
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rootLayout = FrameLayout(this)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.setSupportMultipleWindows(false)
            
            // User Agent de PC para evitar restricciones de móvil
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            
            webViewClient = WebViewClient()
            
            webChromeClient = object : WebChromeClient() {
                // 2. FORZAMOS EL MODO VÍDEO DEL SISTEMA
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customView = view
                    // Forzamos a que el video ocupe CUALQUIER espacio del sistema
                    rootLayout.addView(customView, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    webView.visibility = View.GONE
                    
                    // Ocultamos las barras de navegación del TV Box si existieran
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN 
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                    
                    cursorView.bringToFront()
                }

                override fun onHideCustomView() {
                    if (customView == null) return
                    rootLayout.removeView(customView)
                    customView = null
                    webView.visibility = View.VISIBLE
                    // Restauramos la visibilidad normal
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    cursorView.bringToFront()
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
        updateCursor()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
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
                    // 3. CLIC INTELIGENTE REFORZADO
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
                    
                    // Inyectamos script para forzar el clic en elementos de video
                    webView.evaluateJavascript("""
                        (function() {
                            var el = document.elementFromPoint($cursorX, $cursorY);
                            if (el) {
                                el.click();
                                if(el.tagName === 'VIDEO' || el.className.includes('player')) {
                                    if(el.requestFullscreen) el.requestFullscreen();
                                    else if(el.webkitRequestFullscreen) el.webkitRequestFullscreen();
                                }
                            }
                        })();
                    """.trimIndent(), null)
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

    private fun updateCursor() {
        cursorX = cursorX.coerceIn(0f, rootLayout.width.toFloat())
        cursorY = cursorY.coerceIn(0f, rootLayout.height.toFloat())
        cursorView.x = cursorX
        cursorView.y = cursorY
        cursorView.bringToFront()
    }
}
