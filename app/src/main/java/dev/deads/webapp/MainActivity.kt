package dev.deads.webapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.http.SslError
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var editUrl: EditText
    private lateinit var saveUrlButton: Button
    private lateinit var menuContainer: LinearLayout
    private lateinit var sslCheckBox: CheckBox
    private lateinit var closeMenuButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "TV_WEB_BROWSER_PREFS"
    private val URL_KEY = "SAVED_URL"
    private val DEFAULT_URL = "https://pelisflix20.space/"

    // Variables del cursor (Empezamos en el centro aproximado)
    private var cursorX = 640f 
    private var cursorY = 360f
    private val step = 40f // Velocidad del movimiento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        webView = findViewById(R.id.webView)
        menuContainer = findViewById(R.id.menu_container)
        editUrl = findViewById(R.id.edit_url)
        saveUrlButton = findViewById(R.id.button_save_url)
        sslCheckBox = findViewById(R.id.checkbox_ignore_ssl)
        closeMenuButton = findViewById(R.id.button_close_menu)

        val savedUrl = loadUrl()
        editUrl.setText(savedUrl)
        setupWebView(savedUrl)
        setupOnBackPressed()

        closeMenuButton.setOnClickListener { menuContainer.visibility = View.GONE }
        saveUrlButton.setOnClickListener {
            val newUrl = editUrl.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                saveUrl(newUrl)
                webView.loadUrl(newUrl)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Inyectamos el cursor y nos aseguramos de que sea persistente
                injectCursorScript()
            }
        }
        webView.loadUrl(url)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> cursorY = (cursorY - step).coerceAtLeast(0f)
                KeyEvent.KEYCODE_DPAD_DOWN -> cursorY = (cursorY + step).coerceAtMost(webView.height.toFloat())
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX = (cursorX - step).coerceAtLeast(0f)
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX = (cursorX + step).coerceAtMost(webView.width.toFloat())
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    clickAt(cursorX, cursorY)
                    return true
                }
            }
            updateCursorPosition()
        }
        return super.dispatchKeyEvent(event)
    }

    private fun injectCursorScript() {
        val script = """
            (function() {
                var cursor = document.getElementById('virtual-cursor');
                if (!cursor) {
                    cursor = document.createElement('div');
                    cursor.id = 'virtual-cursor';
                    cursor.style.position = 'fixed'; // FIXED para que no se mueva con el scroll
                    cursor.style.width = '20px';
                    cursor.style.height = '20px';
                    cursor.style.background = 'rgba(255, 0, 0, 0.8)';
                    cursor.style.borderRadius = '50%';
                    cursor.style.zIndex = '2147483647';
                    cursor.style.pointerEvents = 'none';
                    cursor.style.border = '2px solid white';
                    cursor.style.boxShadow = '0 0 5px black';
                    cursor.style.left = '${cursorX}px';
                    cursor.style.top = '${cursorY}px';
                    document.body.appendChild(cursor);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun updateCursorPosition() {
        // Usamos translate3d para que el movimiento sea más fluido
        val script = "var c = document.getElementById('virtual-cursor'); if(c) { c.style.left = '${cursorX}px'; c.style.top = '${cursorY}px'; }"
        webView.evaluateJavascript(script, null)
    }

    private fun clickAt(x: Float, y: Float) {
        val script = """
            var el = document.elementFromPoint($x, $y);
            if (el) { 
                el.click();
                // Si es un elemento que necesita foco (como un input), se lo damos
                if(el.focus) el.focus();
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun saveUrl(url: String) { sharedPreferences.edit().putString(URL_KEY, url).apply() }
    private fun loadUrl(): String = sharedPreferences.getString(URL_KEY, DEFAULT_URL) ?: DEFAULT_URL
}
