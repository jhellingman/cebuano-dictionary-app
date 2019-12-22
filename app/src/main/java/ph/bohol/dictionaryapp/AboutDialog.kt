package ph.bohol.dictionaryapp

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebView

/**
 * Show some information about the dictionary app.
 */
internal class AboutDialog(newContext: Context) : Dialog(newContext) {

    public override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.about_dialog)
        val webView = findViewById<WebView>(R.id.info_text)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.visibility = View.VISIBLE
        webView.loadUrl("file:///android_asset/html/about.html")
    }
}
