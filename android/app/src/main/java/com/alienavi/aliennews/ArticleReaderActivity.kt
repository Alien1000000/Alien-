package com.alienavi.aliennews

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri

class ArticleReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val url = intent.getStringExtra("url") ?: "https://aliennews.co.il"
        val title = intent.getStringExtra("title") ?: "Alien News"
        val language = intent.getStringExtra("language") ?: "he"
        setContent { AlienNewsTheme { ArticleReader(url, title, language, onClose = ::finish) } }
    }
}

@Composable
private fun ArticleReader(url: String, title: String, language: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val safeUrl = remember(url) { url.takeIf(::isAllowedAlienNewsUrl) ?: "https://aliennews.co.il" }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.webViewClient = WebViewClient()
            webView?.destroy()
        }
    }
    BackHandler { if (webView?.canGoBack() == true) webView?.goBack() else onClose() }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(horizontal = 5.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() else onClose() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, if (language == "he") "חזרה" else "Back", tint = Color.White) }
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "$title\n$url") }, null)) }) { Icon(Icons.Outlined.Share, "שיתוף", tint = Gold) }
            IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Icon(Icons.Outlined.OpenInBrowser, "פתיחה בדפדפן", tint = Color.White) }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = Cream)
        if (loadFailed) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Text(if (language == "he") "לא הצלחנו לטעון את הכתבה. בדוק את החיבור ונסה שוב." else "We could not load the story. Check your connection and try again.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(8.dp))
                Button(onClick = { loadFailed = false; loading = true; webView?.reload() }) { Text(if (language == "he") "נסה שוב" else "Try again") }
                Spacer(Modifier.weight(1f))
            }
        } else AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setGeolocationEnabled(false)
                    settings.mediaPlaybackRequiresUserGesture = true
                    if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        private var mainFrameFailed = false

                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                            mainFrameFailed = false
                            loading = true
                            if (url?.let(::isAllowedAlienNewsUrl) == true) prepareArticleForReading(view)
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return if (isAllowedAlienNewsUrl(request.url.toString())) false else {
                                context.startActivity(Intent(Intent.ACTION_VIEW, request.url)); true
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (url?.let(::isAllowedAlienNewsUrl) == true) prepareArticleForReading(view)
                            loading = false
                            if (!mainFrameFailed) loadFailed = false
                        }
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                loading = false
                                mainFrameFailed = true
                                loadFailed = true
                            }
                        }
                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                loading = false
                                mainFrameFailed = true
                                loadFailed = true
                            }
                        }
                    }
                    loadUrl(safeUrl)
                }
            },
        )
    }
}

private fun isAllowedAlienNewsUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    uri.scheme == "https" && uri.host.equals("aliennews.co.il", ignoreCase = true)
}.getOrDefault(false)

private fun prepareArticleForReading(webView: WebView) {
    webView.evaluateJavascript(
        """
        (() => {
          try {
            localStorage.setItem('alien-news-analytics-consent', 'denied');
            document.querySelectorAll('.consent-panel, .privacy-settings-button')
              .forEach((element) => element.remove());

            const redundantHeadings = new Set([
              'עמדת Alien News', 'מה למדנו', 'למה זה חשוב', 'מסקנת המערכת',
              'הערת המערכת', 'סיכום', 'Alien News position', 'What we learned',
              'Why it matters', 'Editorial conclusion', 'Editorial note', 'Conclusion'
            ]);
            document.querySelectorAll('.article-body-sections h2')
              .forEach((heading) => {
                if (redundantHeadings.has(heading.textContent.trim())) heading.remove();
              });
            document.querySelectorAll('.article-note').forEach((element) => element.remove());
            document.querySelectorAll('.article-analysis span').forEach((element) => element.remove());
          } catch (_) {}
        })();
        """.trimIndent(),
        null,
    )
}
