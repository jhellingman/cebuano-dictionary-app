package ph.bohol.dictionaryapp

import java.util.LinkedHashMap

internal class RootCache(private val capacity: Int) : LinkedHashMap<String?, Boolean?>(capacity + 1, LOAD_FACTOR, true) {

    override fun removeEldestEntry(eldest: Map.Entry<String?, Boolean?>): Boolean {
        return size > capacity
    }

    companion object {
        private const val LOAD_FACTOR = 1.1f
    }
}