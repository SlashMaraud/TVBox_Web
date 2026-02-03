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
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var editUrl: EditText
    private lateinit var saveUrlButton: Button
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var menuContainer: LinearLayout
    private lateinit var sslCheckBox: CheckBox
    private lateinit var closeMenuButton: Button
    private lateinit var infoButton: Button
    private lateinit var languageButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "TV_WEB_BROWSER_PREFS"
    private val URL_KEY = "SAVED_URL"
    private val SSL_IGNORE_KEY = "SSL_IGNORE"
    private val DEFAULT_URL = "https://pelisflix20.space/"

    // Variables para el ratón virtual
    private var cursorX = 500f
    private var cursorY = 300f
    private val step = 30f // Velocidad del cursor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        rootLayout = findViewById(R.id.constraint_layout_root)
        webView = findViewById(R.id.webView)
        menuContainer = findViewById(R.id.menu_container)
        editUrl = findViewById(R.id.edit_url)
        saveUrlButton = findViewById(R.id.button_save_url)
        sslCheckBox = findViewById(R.id.checkbox_ignore_ssl)
        closeMenuButton = findViewById(R.id.button_close_menu)
        infoButton = findViewById(R.id.button_info)
        languageButton = findViewById(R.id.button_language)

        val savedUrl = loadUrl()
        editUrl.setText(savedUrl)
        setupWebView(savedUrl)
        setupOnBackPressed()

        closeMenuButton.setOnClickListener { closeMenu() }
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
                injectCursorScript() // Creamos el punto visual
            }
        }
        webView.loadUrl(url)
    }

    // Capturamos las teclas del mando
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> cursorY -= step
                KeyEvent.KEYCODE_DPAD_DOWN -> cursorY += step
                KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
                KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
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
                    cursor.style.position = 'fixed';
                    cursor.style.width = '15px';
                    cursor.style.height = '15px';
                    cursor.style.background = 'red';
                    cursor.style.borderRadius = '50%';
                    cursor.style.zIndex = '10000';
                    cursor.style.pointerEvents = 'none';
                    cursor.style.border = '2px solid white';
                    document.body.appendChild(cursor);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun updateCursorPosition() {
        val script = "document.getElementById('virtual-cursor').style.left = '${cursorX}px';" +
                     "document.getElementById('virtual-cursor').style.top = '${cursorY}px';"
        webView.evaluateJavascript(script, null)
    }

    private fun clickAt(x: Float, y: Float) {
        val script = """
            var el = document.elementFromPoint($x, $y);
            if (el) { el.click(); }
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

    private fun closeMenu() { menuContainer.visibility = View.GONE }
    private fun saveUrl(url: String) { sharedPreferences.edit().putString(URL_KEY, url).apply() }
    private fun loadUrl(): String = sharedPreferences.getString(URL_KEY, DEFAULT_URL) ?: DEFAULT_URL
}
