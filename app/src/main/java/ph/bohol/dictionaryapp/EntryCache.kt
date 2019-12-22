package ph.bohol.dictionaryapp

import android.text.Spanned
import java.util.LinkedHashMap

internal class EntryCache(private val capacity: Int) : LinkedHashMap<Int?, Spanned?>(capacity + 1, LOAD_FACTOR, true) {

    override fun removeEldestEntry(eldest: Map.Entry<Int?, Spanned?>): Boolean {
        return size > capacity
    }

    companion object {
        private const val LOAD_FACTOR = 1.1f
    }
}