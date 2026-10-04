package pynith.apps.nextel.views.us

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pynith.apps.nextel.R
import pynith.apps.nextel.model.CONData
import pynith.apps.nextel.views.BaseActivity

class PrivacyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.info_policy)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val closeButton = findViewById<FloatingActionButton>(R.id.closeBtn)
        val webView = findViewById<WebView>(R.id.webviewPrivacy)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        closeButton.setOnClickListener { finish() }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.webViewClient = WebViewClient()
        webView.loadUrl("${CONData.WebPage}privacy")
    }
}
