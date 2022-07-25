package ph.bohol.dictionaryapp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

// See http://stackoverflow.com/questions/531427/ for OnSharedPreferenceChangeListener

public class DictionaryPreferenceActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {
    private static final String KEY_SEARCH_FONT_SIZE = "search_font_size";
    public static final String KEY_PRESENTATION_FONT_SIZE = "font_size";
    public static final String KEY_EXPAND_ABBREVIATIONS = "expand_abbreviations";
    public static final String KEY_PRESENTATION_STYLE = "presentation_style";
    public static final String KEY_REVERSE_LOOKUP = "reverse_lookup";
    public static final String KEY_MEASURE_UNITS = "measure_units";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_USE_STEMMING = "use_stemming";
    public static final String KEY_LAST_SEARCH_WORD = "last_search_word";
    public static final String KEY_SHOW_PREVIEW = "show_preview";

    public static final String VALUE_MEASURE_ORIGINAL = "original";
    public static final String VALUE_MEASURE_METRIC = "metric";

    private ListPreference searchFontSizeListPreference;
    private ListPreference presentationFontSizeListPreference;
    private ListPreference presentationStyleListPreference;
    private ListPreference measureUnitListPreference;

    @SuppressWarnings("deprecation")
    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        searchFontSizeListPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_SEARCH_FONT_SIZE);
        presentationFontSizeListPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_PRESENTATION_FONT_SIZE);
        presentationStyleListPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_PRESENTATION_STYLE);
        measureUnitListPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_MEASURE_UNITS);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummaries(sharedPreferences);
    }

    @Override
    protected final void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummaries(sharedPreferences);
    }

    private void updateSummaries(SharedPreferences sharedPreferences) {
        if (presentationStyleListPreference != null) {
            presentationStyleListPreference.setSummary(presentationStyleToText(sharedPreferences.getString(KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL)));
        }
        if (presentationFontSizeListPreference != null) {
            presentationFontSizeListPreference.setSummary(fontSizeToText(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20")));
        }
        if (searchFontSizeListPreference != null) {
            searchFontSizeListPreference.setSummary(fontSizeToText(sharedPreferences.getString(KEY_SEARCH_FONT_SIZE, "20")));
        }
        if (measureUnitListPreference != null) {
            measureUnitListPreference.setSummary(measureUnitToText(sharedPreferences.getString(KEY_MEASURE_UNITS, VALUE_MEASURE_ORIGINAL)));
        }
    }

    @Override
    public final void onSharedPreferenceChanged(@NonNull final SharedPreferences sharedPreferences, final String key) {
        switch (key) {
            case KEY_PRESENTATION_STYLE:
                if (presentationStyleListPreference != null) {
                    presentationStyleListPreference.setSummary(presentationStyleToText(sharedPreferences.getString(KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL)));
                }
                break;
            case KEY_PRESENTATION_FONT_SIZE:
                if (presentationFontSizeListPreference != null) {
                    presentationFontSizeListPreference.setSummary(fontSizeToText(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20")));
                }
                break;
            case KEY_SEARCH_FONT_SIZE:
                if (searchFontSizeListPreference != null) {
                    searchFontSizeListPreference.setSummary(fontSizeToText(sharedPreferences.getString(KEY_SEARCH_FONT_SIZE, "20")));
                }
                break;
            case KEY_MEASURE_UNITS:
                if (measureUnitListPreference != null) {
                    measureUnitListPreference.setSummary(measureUnitToText(sharedPreferences.getString(KEY_MEASURE_UNITS, VALUE_MEASURE_ORIGINAL)));
                }
                break;
        }
    }

    private String presentationStyleToText(final String presentationStyle) {
        if (EntryTransformer.STYLE_STRUCTURAL.equalsIgnoreCase(presentationStyle)) {
            return getString(R.string.presentation_structural);
        }
        if (EntryTransformer.STYLE_COMPACT.equalsIgnoreCase(presentationStyle)) {
            return getString(R.string.presentation_compact);
        }
        if (EntryTransformer.STYLE_DEBUG.equalsIgnoreCase(presentationStyle)) {
            return getString(R.string.presentation_debug);
        }
        return getString(R.string.presentation_traditional);
    }

    private String fontSizeToText(final String fontSize) {
        if ("12".equalsIgnoreCase(fontSize)) {
            return getString(R.string.font_size_tiny);
        }
        if ("16".equalsIgnoreCase(fontSize)) {
            return getString(R.string.font_size_small);
        }
        if ("24".equalsIgnoreCase(fontSize)) {
            return getString(R.string.font_size_large);
        }
        return getString(R.string.font_size_medium);
    }

    private String measureUnitToText(final String measureUnit) {
        if (VALUE_MEASURE_METRIC.equalsIgnoreCase(measureUnit)) {
            return getString(R.string.measure_metric);
        }
        return getString(R.string.measure_original);
    }
}
