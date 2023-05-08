package ink.diya.books

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : ComponentActivity() {
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout
    private lateinit var webView : WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        webView.loadUrl("https://diya.ink/")
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = webChromeClient

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    /**
     * Go back on the WebView if there is history,
     * if not, bubble up to systems behavior (exit the activity)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private var filePathCallbackFromWebView: ValueCallback<Array<Uri>>? = null

    /**
     * Handle the file chooser response from the activity
     */
    private val pickFile: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    filePathCallbackFromWebView?.onReceiveValue(arrayOf(uri))
                } else {
                    filePathCallbackFromWebView?.onReceiveValue(null)
                }
            } else {
                filePathCallbackFromWebView?.onReceiveValue(null)
            }
    }

    /**
     * Handle the file chooser from the WebView
     */
    private var webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/epub+zip"
            }
            pickFile.launch(intent)
            filePathCallbackFromWebView = filePathCallback
            return true
        }
    }
}

class WebAppInterface(private val mContext: Context) {
    /** Show a toast from the web page */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }
}