package ph.bohol.dictionaryapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ph.bohol.util.normalizer.CebuanoNormalizer;
import ph.bohol.util.stemmer.Derivation;
import ph.bohol.util.stemmer.Stemmer;
import ph.bohol.util.stemmer.StemmerParser;

public class MainActivity extends AppCompatActivity
implements OnQueryTextListener {
    static final String SEARCH_WORD = "ph.bohol.dictionaryapp.SEARCH_WORD";
    static final String ENTRY_ID = "ph.bohol.dictionaryapp.ENTRY_ID";

    private static final String TAG = "MainActivity";
    private static final int RESULT_SETTINGS = 1;
    private static final int RESULT_SHOW_ENTRY = 2;
    private static final String ASSET_STEMMER_CEBUANO = "xml/stemmerCebuano.xml";

    private SearchView searchView = null;
    private ListView listView = null;
    private WebView webView = null;
    private Cursor cursor = null;
    private String searchWord = null;
    private Stemmer stemmer = null;
    private boolean reverseLookup = false;
    private boolean useStemming = false;
    private String lastSearchWord = null;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "OnCreate");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        retrievePreferences(sharedPreferences);
        intializeStemmer();

        listView = findViewById(R.id.listview);
        webView = findViewById(R.id.webview);
        webView.setBackgroundColor(Color.DKGRAY);

        // Get searchWord after resume (e.g. after a rotation).
        if (savedInstanceState != null) {
            searchWord = savedInstanceState.getString(SEARCH_WORD);
            Log.d(TAG, "Got searchWord '" + searchWord + "' from savedInstanceState");
        } else {
            Intent intent = getIntent();
            searchWord = intent.getStringExtra(MainActivity.SEARCH_WORD);
            Log.d(TAG, "Got searchWord '" + searchWord + "' from intent");
        }
        if (searchWord == null || searchWord.isEmpty()) {
            searchWord = lastSearchWord;
        }
        update();
    }

    private void update() {
        if (searchWord == null || searchWord.isEmpty()) {
            searchWord = "";
            showFrontPage();
        } else {
            populateList(searchWord);
        }
    }

    private void showFrontPage() {
        listView.setVisibility(View.INVISIBLE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl("file:///android_asset/html/frontpage.html");
    }

    @Override
    public final void onSaveInstanceState(final Bundle outState) {
        outState.putString(SEARCH_WORD, searchWord);
        super.onSaveInstanceState(outState);
    }

    private void populateList(final String newSearchWord) {
        webView.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.VISIBLE);
        PrepareCursorTask cursorTask = new PrepareCursorTask(this);
        cursorTask.execute(newSearchWord);
    }

    private void setCursorAdapter(final Cursor newCursor) {
        // First cleanup our previous cursor, to prevent resource leaks.
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;

        listView.setAdapter(new HeadCursorAdapter(this, cursor));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            cursor.moveToPosition(position);
            String entryId = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.HEAD_ENTRY_ID));

            // Result will be search word if cross-reference is followed.
            Intent intent = new Intent(MainActivity.this, ShowEntryActivity.class);
            intent.putExtra(ENTRY_ID, entryId);
            startActivityForResult(intent, RESULT_SHOW_ENTRY);
        });
    }

    private void retrievePreferences(SharedPreferences sharedPreferences) {
        reverseLookup = sharedPreferences.getBoolean(DictionaryPreferenceActivity.KEY_REVERSE_LOOKUP, false);
        useStemming = sharedPreferences.getBoolean(DictionaryPreferenceActivity.KEY_USE_STEMMING, false);
        lastSearchWord = sharedPreferences.getString(DictionaryPreferenceActivity.KEY_LAST_SEARCH_WORD, "");
    }

    private void saveSearchWord() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(DictionaryPreferenceActivity.KEY_LAST_SEARCH_WORD, searchWord);
        editor.apply();
    }

    private void intializeStemmer() {
        if (stemmer == null) {
            InputStream stream = null;
            try {
                stream = getAssets().open(ASSET_STEMMER_CEBUANO);
            } catch (IOException e) {
                Log.d(TAG, "Fatal error: unable to open " + ASSET_STEMMER_CEBUANO);
                e.printStackTrace();
            }

            StemmerParser parser = new StemmerParser();
            stemmer = parser.parse(stream);
            stemmer.setRootProvider(DictionaryDatabase.getInstance(this));
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setIconifiedByDefault(false);

        // Set search word already retrieved from the intent in onCreate();
        searchView.setQuery(searchWord, false);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, DictionaryPreferenceActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
            case R.id.about:
                AboutDialog about = new AboutDialog(this);
                about.requestWindowFeature(Window.FEATURE_NO_TITLE);
                about.setTitle(R.string.about_cebuano_dictionary);
                about.show();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED && !(requestCode == RESULT_SETTINGS)) {
            return;
        }

        switch (requestCode) {
            case RESULT_SHOW_ENTRY:
                overridePendingTransition(R.anim.left_in, R.anim.right_out);

                // User followed a cross-reference to another entry, search for it.
                if (resultCode == Activity.RESULT_OK) {
                    searchWord = data.getStringExtra(MainActivity.SEARCH_WORD);
                    searchView.setQuery(searchWord, false);
                    Log.d(TAG, "Activity returned with '" + searchWord + "'");
                    update();
                }
                break;
            case RESULT_SETTINGS:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                retrievePreferences(sharedPreferences);
                update();
                break;
            default:
                break;
        }
    }

    @Override
    protected final void onPause() {
        super.onPause();
    }

    @Override
    protected final void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        retrievePreferences(sharedPreferences);
    }

    @Override
    protected final void onDestroy() {
        saveSearchWord();
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public final boolean onQueryTextChange(final String newText) {
        if (!newText.equals(searchWord)) {
            searchWord = newText;
            update();
        }
        return false;
    }

    @Override
    public final boolean onQueryTextSubmit(final String query) {
        saveSearchWord();
        // Already handled by the onQueryTextChange() handler, just close the search
        searchView.clearFocus();
        return true;
    }

    private class PrepareCursorTask extends AsyncTask<String, Void, Cursor> {
        private final Context context;

        PrepareCursorTask(final Context newContext) {
            this.context = newContext;
        }

        @Override
        protected Cursor doInBackground(final String... searchWords) {
            CebuanoNormalizer n = new CebuanoNormalizer();
            String normalizedSearchWord = n.normalize(searchWord);

            DictionaryDatabase database = DictionaryDatabase.getInstance(context);
            Cursor headsCursor;

            List<Derivation> derivations = null;
            if (useStemming) {
                derivations = stemmer.findDerivations(normalizedSearchWord);
            }

            headsCursor = database.getHeads(searchWord, reverseLookup, derivations);

            // Move the cursor to the first entry, do force the database do some heavy-lifting
            // on this task's thread before handing it back to the UI thread.
            headsCursor.moveToFirst();
            return headsCursor;
        }

        @Override
        protected void onPostExecute(final Cursor result) {
            if (!isCancelled()) {
                setCursorAdapter(result);
            }
        }
    }
}
