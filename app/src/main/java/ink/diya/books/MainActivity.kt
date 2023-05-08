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
        webView.addJavascriptInterface(WebAppInterface(this, webView), "Android")
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

/** Methods for allowing JavaScript to access android specific methods */
class WebAppInterface(private val mContext: Context, private val webView: WebView) {
    /**
     * Show a toast from the web page
     *
     * @param toast The message to tell the user
     */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    /**
     * Show a confirm dialog from the web page
     *
     * @param message The question to ask the user
     * @param yesFunc The javascript function to execute if the user clicks yes
     * @param noFunc The javascript function to execute if the user clicks no
     */
    @JavascriptInterface
    fun showConfirmDialog(message: String, yesFunc: String, noFunc: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(mContext)
        builder.setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                webView.post { webView.evaluateJavascript(yesFunc, null) }
            }
            .setNegativeButton("No") { _, _ ->
                webView.post { webView.evaluateJavascript(noFunc, null) }
            }
        builder.create().show()
    }
}