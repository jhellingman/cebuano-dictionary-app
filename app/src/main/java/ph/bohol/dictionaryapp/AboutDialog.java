package ph.bohol.dictionaryapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * AboutDialog show some information about the dictionary app.
 *
 * @author Jeroen Hellingman.
 */
class AboutDialog extends Dialog {
    private static Context context = null;

    /**
     * Create a new AboutDialog.
     *
     * @param newContext the context to be used.
     */
    AboutDialog(final Context newContext) {
        super(newContext);
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        setContentView(R.layout.about_dialog);
        WebView webView = findViewById(R.id.info_text);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl("file:///android_asset/html/about.html");
    }
}
