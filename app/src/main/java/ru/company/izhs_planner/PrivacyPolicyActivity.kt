package ru.company.izhs_planner

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.google.android.material.appbar.MaterialToolbar

class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        val webView = findViewById<WebView>(R.id.webView)
        
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        try {
            webView.loadUrl("file:///android_asset/privacy_policy.html")
        } catch (e: Exception) {
            webView.loadData(
                "<html><body><h3>Политика конфиденциальности</h3><p>Ошибка загрузки. Проверьте подключение.</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }
}