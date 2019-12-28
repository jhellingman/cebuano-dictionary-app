package ph.bohol.dictionaryapp

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

// See http://stackoverflow.com/questions/531427/ for OnSharedPreferenceChangeListener
class DictionaryPreferenceActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {

    private var searchFontSizeListPreference: ListPreference? = null
    private var presentationFontSizeListPreference: ListPreference? = null
    private var presentationStyleListPreference: ListPreference? = null
    private var measureUnitListPreference: ListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        searchFontSizeListPreference = preferenceScreen.findPreference(KEY_SEARCH_FONT_SIZE) as ListPreference
        presentationFontSizeListPreference = preferenceScreen.findPreference(KEY_PRESENTATION_FONT_SIZE) as ListPreference
        presentationStyleListPreference = preferenceScreen.findPreference(KEY_PRESENTATION_STYLE) as ListPreference
        measureUnitListPreference = preferenceScreen.findPreference(KEY_MEASURE_UNITS) as ListPreference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateSummaries(sharedPreferences)
    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateSummaries(sharedPreferences)
    }

    private fun updateSummaries(sharedPreferences: SharedPreferences) {
        if (presentationStyleListPreference != null) {
            presentationStyleListPreference!!.summary =
                    presentationStyleToText(sharedPreferences.getString(KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL) as String)
        }
        if (presentationFontSizeListPreference != null) {
            presentationFontSizeListPreference!!.summary =
                    fontSizeToText(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20") as String)
        }
        if (searchFontSizeListPreference != null) {
            searchFontSizeListPreference!!.summary =
                    fontSizeToText(sharedPreferences.getString(KEY_SEARCH_FONT_SIZE, "20") as String)
        }
        if (measureUnitListPreference != null) {
            measureUnitListPreference!!.summary =
                    measureUnitToText(sharedPreferences.getString(KEY_MEASURE_UNITS, VALUE_MEASURE_ORIGINAL) as String)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            KEY_PRESENTATION_STYLE -> if (presentationStyleListPreference != null) {
                presentationStyleListPreference!!.summary =
                        presentationStyleToText(sharedPreferences.getString(KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL) as String)
            }
            KEY_PRESENTATION_FONT_SIZE -> if (presentationFontSizeListPreference != null) {
                presentationFontSizeListPreference!!.summary =
                        fontSizeToText(sharedPreferences.getString(KEY_PRESENTATION_FONT_SIZE, "20") as String)
            }
            KEY_SEARCH_FONT_SIZE -> if (searchFontSizeListPreference != null) {
                searchFontSizeListPreference!!.summary =
                        fontSizeToText(sharedPreferences.getString(KEY_SEARCH_FONT_SIZE, "20") as String)
            }
            KEY_MEASURE_UNITS -> if (measureUnitListPreference != null) {
                measureUnitListPreference!!.summary =
                        measureUnitToText(sharedPreferences.getString(KEY_MEASURE_UNITS, VALUE_MEASURE_ORIGINAL) as String)
            }
        }
    }

    private fun presentationStyleToText(presentationStyle: String): String {
        if (EntryTransformer.STYLE_STRUCTURAL.equals(presentationStyle, ignoreCase = true)) {
            return getString(R.string.presentation_structural)
        }
        if (EntryTransformer.STYLE_COMPACT.equals(presentationStyle, ignoreCase = true)) {
            return getString(R.string.presentation_compact)
        }
        return if (EntryTransformer.STYLE_DEBUG.equals(presentationStyle, ignoreCase = true)) {
            getString(R.string.presentation_debug)
        } else getString(R.string.presentation_traditional)
    }

    private fun fontSizeToText(fontSize: String): String {
        if ("12".equals(fontSize, ignoreCase = true)) {
            return getString(R.string.font_size_tiny)
        }
        if ("16".equals(fontSize, ignoreCase = true)) {
            return getString(R.string.font_size_small)
        }
        return if ("24".equals(fontSize, ignoreCase = true)) {
            getString(R.string.font_size_large)
        } else getString(R.string.font_size_medium)
    }

    private fun measureUnitToText(measureUnit: String): String {
        return if (VALUE_MEASURE_METRIC.equals(measureUnit, ignoreCase = true)) {
            getString(R.string.measure_metric)
        } else getString(R.string.measure_original)
    }

    companion object {
        const val KEY_SEARCH_FONT_SIZE = "search_font_size"
        const val KEY_PRESENTATION_FONT_SIZE = "font_size"
        const val KEY_EXPAND_ABBREVIATIONS = "expand_abbreviations"
        const val KEY_PRESENTATION_STYLE = "presentation_style"
        const val KEY_REVERSE_LOOKUP = "reverse_lookup"
        const val KEY_MEASURE_UNITS = "measure_units"
        const val KEY_NIGHT_MODE = "night_mode"
        const val KEY_USE_STEMMING = "use_stemming"
        const val KEY_LAST_SEARCH_WORD = "last_search_word"
        const val KEY_SHOW_PREVIEW = "show_preview"
        const val VALUE_MEASURE_ORIGINAL = "original"
        const val VALUE_MEASURE_METRIC = "metric"
    }
}