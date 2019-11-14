package ph.bohol.dictionaryapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import static ph.bohol.dictionaryapp.DictionaryPreferenceActivity.*;

public class ShowEntryActivity extends Activity
        implements OnSharedPreferenceChangeListener {
    private static final int DEFAULT_FONT_SIZE = 20;
    private int fontSize = DEFAULT_FONT_SIZE;
    private static final int RESULT_SETTINGS = 1;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final String ENTRY_ID = "ph.bohol.dictionaryapp.ENTRY_ID";
    private static final String TAG = "ShowEntryActivity";
    private static final String SEARCH_URL = "search:";
    private int entryId;
    private boolean expandAbbreviations = false;
    private boolean useMetric = false;
    private boolean givenSwipeNextHint = false;
    private boolean givenSwipePreviousHint = false;
    private String presentationStyle = EntryTransformer.STYLE_TRADITIONAL;
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_entry);

        Log.d(TAG, "OnCreate");

        overridePendingTransition(R.anim.right_in, R.anim.left_out);

        // Show the Up button in the action bar.
        setupActionBar();

        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = (view, event) -> gestureDetector.onTouchEvent(event);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        retrievePreferences(preferences);

        // Get entryId after resume (e.g. after a rotation).
        if (savedInstanceState != null) {
            entryId = savedInstanceState.getInt(ENTRY_ID);
        } else {
            Intent intent = getIntent();
            entryId = Integer.parseInt(intent.getStringExtra(MainActivity.ENTRY_ID));
        }

        showEntry();
    }

    @Override
    public final void onSaveInstanceState(final Bundle outState) {
        outState.putInt(ENTRY_ID, entryId);
        super.onSaveInstanceState(outState);
    }

    private void showEntry() {
        DictionaryDatabase database = DictionaryDatabase.getInstance(this);
        try (Cursor cursor = database.getEntry(entryId)) {
            String entry = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.ENTRY_ENTRY));
            String head = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.ENTRY_HEAD));
            setTitle(head);

            String htmlEntry = transformEntry(entry);

            if (htmlEntry != null) {
                WebView webView = (WebView) this.findViewById(R.id.webViewEntry);
                if (webView != null) {
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                            if (url.startsWith(SEARCH_URL)) {
                                // Send back to main. This way, links clicked in the data do not result in opening lots
                                // of MainActivity instances.
                                String searchWord = url.substring(SEARCH_URL.length());
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra(MainActivity.SEARCH_WORD, searchWord);
                                setResult(Activity.RESULT_OK, resultIntent);
                                finish();
                                return true;
                            }

                            // Show URL in external browser
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                            return true;
                        }
                    });

                    webView.loadDataWithBaseURL("", htmlEntry, "text/html", "UTF-8", "");
                    webView.setOnTouchListener(gestureListener);
                }
            }
        }
    }

    private String transformEntry(final String entry) {
        String wrappedEntry = "<dictionary>" + entry + "</dictionary>";

        EntryTransformer entryTransformer = EntryTransformer.getInstance(this);
        entryTransformer.setExpandAbbreviations(expandAbbreviations);
        entryTransformer.setFontSize(fontSize);
        entryTransformer.setUseMetric(useMetric);
        return entryTransformer.transform(wrappedEntry, presentationStyle);
    }

    private void retrievePreferences(SharedPreferences sharedPreferences) {
        expandAbbreviations = sharedPreferences.getBoolean(KEY_EXPAND_ABBREVIATIONS, true);
        fontSize = Integer.parseInt(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20"));
        presentationStyle = sharedPreferences.getString(KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL);
        useMetric = sharedPreferences.getString(KEY_MEASURE_UNITS, VALUE_MEASURE_ORIGINAL).equals(VALUE_MEASURE_METRIC);
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_results, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                // NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;

            case R.id.action_next:
                if (!givenSwipeNextHint) {
                    Toast.makeText(this, getResources().getString(R.string.can_swipe_to_move_to_next_entry),
                            Toast.LENGTH_SHORT).show();
                    givenSwipeNextHint = true;
                }
                moveToNextEntry();
                break;

            case R.id.action_previous:
                if (!givenSwipePreviousHint) {
                    Toast.makeText(this, getResources().getString(R.string.can_swipe_to_move_to_previous_entry),
                            Toast.LENGTH_SHORT).show();
                    givenSwipePreviousHint = true;
                }
                moveToPreviousEntry();
                break;

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
        return super.onOptionsItemSelected(item);
    }

    private void moveToPreviousEntry() {
        DictionaryDatabase database = DictionaryDatabase.getInstance(this);
        int newEntryId = database.getPreviousEntryId(entryId);
        if (newEntryId == entryId) {
            Toast.makeText(this, getResources().getString(R.string.reached_first_entry), Toast.LENGTH_SHORT).show();
        }
        entryId = newEntryId;
        showEntry();
    }

    private void moveToNextEntry() {
        DictionaryDatabase database = DictionaryDatabase.getInstance(this);
        int newEntryId = database.getNextEntryId(entryId);
        if (newEntryId == entryId) {
            Toast.makeText(this, getResources().getString(R.string.reached_last_entry), Toast.LENGTH_SHORT).show();
        }
        entryId = newEntryId;
        showEntry();
    }

    @Override
    protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED && !(requestCode == RESULT_SETTINGS)) {
            return;
        }
        if (requestCode == RESULT_SETTINGS) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            retrievePreferences(sharedPreferences);
            showEntry();
        }
    }

    @Override
    protected final void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        retrievePreferences(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected final void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public final void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        retrievePreferences(sharedPreferences);
        switch (key) {
            case KEY_EXPAND_ABBREVIATIONS:
                expandAbbreviations = sharedPreferences.getBoolean(KEY_EXPAND_ABBREVIATIONS, true);
                break;
            case KEY_PRESENTATION_FONT_SIZE:
                fontSize = Integer.parseInt(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20"));
                break;
            case KEY_PRESENTATION_STYLE:
                presentationStyle = sharedPreferences.getString(KEY_PRESENTATION_STYLE,
                        EntryTransformer.STYLE_TRADITIONAL);
                break;
            case KEY_MEASURE_UNITS:
                useMetric = sharedPreferences.getString(KEY_MEASURE_UNITS,
                        VALUE_MEASURE_ORIGINAL).equals(
                        VALUE_MEASURE_METRIC);
                break;
        }
    }

    private class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    moveToNextEntry();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    moveToPreviousEntry();
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception in MyGestureDetector: " + e);
            }
            return false;
        }
    }
}
