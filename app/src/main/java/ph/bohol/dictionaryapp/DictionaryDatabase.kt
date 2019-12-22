package ph.bohol.dictionaryapp

import android.content.Context
import android.database.Cursor
import android.text.Html
import android.text.Spanned
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import ph.bohol.util.normalizer.CebuanoNormalizer
import ph.bohol.util.stemmer.Derivation
import ph.bohol.util.stemmer.RootWordProvider
import java.util.Collections
import java.util.LinkedList
import kotlin.collections.ArrayList

class DictionaryDatabase

    private constructor(private val context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION), RootWordProvider {

    override fun isRootWord(root: String): Boolean {
        if (root.length < MIN_ROOT_LENGTH) {
            return false
        }
        val isRoot = rootCache[root]
        if (isRoot != null) {
            return isRoot
        }
        Log.d(TAG, "Query for root: $root")
        val sqlQuery = "SELECT 1 FROM WCED_head WHERE normalized_head = ? AND pos != '' LIMIT 1"
        val selectionArguments = arrayOf(root)
        val db = this.writableDatabase
        db.rawQuery(sqlQuery, selectionArguments).use { cursor ->
            val result = cursor.count > 0
            rootCache[root] = result
            return result
        }
    }

    override fun isRootWordWithType(root: String, type: String): Boolean {
        if (root.length < MIN_ROOT_LENGTH) {
            return false
        }
        val isRoot = rootCache["$root.$type"]
        if (isRoot != null) {
            return isRoot
        }
        Log.d(TAG, "Query for root: $root with type: $type")
        val sqlQuery = "SELECT 1 FROM WCED_head WHERE normalized_head = ? AND pos LIKE ? LIMIT 1"
        val selectionArguments = arrayOf(root, "%$type%")
        val db = this.writableDatabase
        db.rawQuery(sqlQuery, selectionArguments).use { cursor ->
            val result = cursor.count > 0
            rootCache["$root.$type"] = result
            return result
        }
    }

    fun getHeads(head: String, reverseLookup: Boolean, derivations: List<Derivation>?): Cursor {
        val n = CebuanoNormalizer()
        val normalizedHead = n.normalize(head)
        val subQueries = LinkedList<String>()
        val arguments = ArrayList<String>()
        subQueries.add("SELECT _id, entryid, head, normalized_head, NULL AS derivation, 'n' AS type "
                + "FROM WCED_head WHERE normalized_head LIKE ?")
        arguments.add("$normalizedHead%")
        if (reverseLookup) {
            subQueries.add("SELECT _id, entryid, translation as head, translation as normalized_head, "
                    + "NULL as derivation, 'r' AS type "
                    + "FROM WCED_translation WHERE translation LIKE ?")
            arguments.add("$head%")
        }
        if (derivations != null) {
            val snippetFormat = ("SELECT _id, entryid, head, normalized_head, '%s' AS derivation, 'd' AS type FROM wced_head "
                    + "WHERE normalized_head = ?")
            derivations.forEach { derivation ->
                val snippet = String.format(snippetFormat, derivation.toString().replace("'", "''"))
                subQueries.add(snippet)
                arguments.add(derivation.root)
            }
        }
        var query = unionize(subQueries)
        query += " ORDER BY normalized_head COLLATE NOCASE"
        Log.d(TAG, "Query for heads and derived forms: $head")
        Log.d(TAG, "Query SQL: $query")
        val selectionArguments = arrayOfNulls<String>(arguments.size)
        arguments.toArray(selectionArguments)
        val db = this.writableDatabase
        return db.rawQuery(query, selectionArguments)
    }

    fun getEntry(entryId: Int): Cursor {
        val sqlQuery = "SELECT * FROM WCED_entry WHERE _id = ?"
        val selectionArguments = arrayOf(entryId.toString())
        val db = this.writableDatabase
        val cursor = db.rawQuery(sqlQuery, selectionArguments)
        cursor.moveToFirst()
        return cursor
    }

    private fun getEntryHtml(entryId: Int): String {
        val cursor = getEntry(entryId)
        val entryXml = cursor.getString(cursor.getColumnIndex(ENTRY_ENTRY))
        cursor.close()
        return EntryTransformer.getInstance(context).transform(entryXml, EntryTransformer.STYLE_COMPACT)
    }

    /**
     * Get a Spanned object with the rich-text content of an entry.
     *
     * @param entryId The entryId for which the rich-text is wanted.
     * @return a Spanned object with the content of the entry.
     */
    fun getEntrySpanned(entryId: Int): Spanned? {
        var entrySpanned = entryCache[entryId]
        if (entrySpanned == null) {
            Log.d(TAG, "Getting entry rendered in Spanned for entryId: $entryId")
            val entryHtml = getEntryHtml(entryId)
            entrySpanned = Html.fromHtml(entryHtml)
            entryCache[entryId] = entrySpanned
        }
        return entrySpanned
    }

    /**
     * Find the entryId of the next entry.
     *
     * @param entryId The entryId of which the next entry is sought.
     * @return the next entryId, or the original entryId if this entry is the last.
     */
    fun getNextEntryId(entryId: Int): Int {
        val sqlQuery = "SELECT _id FROM WCED_entry WHERE _id > ? ORDER BY _id LIMIT 1"
        val selectionArguments = arrayOf(entryId.toString())
        val db = this.writableDatabase
        db.rawQuery(sqlQuery, selectionArguments).use { cursor ->
            if (cursor.count == 0) {
                return entryId
            }
            cursor.moveToFirst()
            return cursor.getInt(cursor.getColumnIndex(ENTRY_ID))
        }
    }

    /**
     * Find the entryId of the previous entry.
     *
     * @param entryId The entryId of which the previous entry is sought.
     * @return the previous entryId, or the original entryId if this entry is the first.
     */
    fun getPreviousEntryId(entryId: Int): Int {
        val sqlQuery = "SELECT _id FROM WCED_entry WHERE _id < ? ORDER BY _id DESC LIMIT 1"
        val selectionArguments = arrayOf(entryId.toString())
        val db = this.writableDatabase
        db.rawQuery(sqlQuery, selectionArguments).use { cursor ->
            if (cursor.count == 0) {
                return entryId
            }
            cursor.moveToFirst()
            return cursor.getInt(cursor.getColumnIndex(ENTRY_ID))
        }
    }

    companion object {
        const val HEAD_HEAD = "head"
        const val HEAD_ENTRY_ID = "entryid"
        const val HEAD_DERIVATION = "derivation"
        const val HEAD_TYPE = "type"
        const val ENTRY_ENTRY = "entry"
        const val ENTRY_HEAD = "head"

        private const val ENTRY_ID = "_id"
        private const val MIN_ROOT_LENGTH = 3
        private const val DATABASE_NAME = "dictionary_database"
        private const val DATABASE_VERSION = 1
        private const val TAG = "DictionaryDatabase"

        private const val ENTRY_CACHE_SIZE = 100
        private val entryCache = Collections.synchronizedMap<Int, Spanned?>(EntryCache(ENTRY_CACHE_SIZE))

        private const val ROOT_CACHE_SIZE = 1000
        private val rootCache = Collections.synchronizedMap<String, Boolean>(RootCache(ROOT_CACHE_SIZE))

        private var instance: DictionaryDatabase? = null

        /**
         * Get the instance of the DictionaryDatabase singleton. Create it if it is not yet available.
         *
         * @param context a context. The application context will be obtained from this context.
         * @return the instance of the DictionaryDatabase singleton.
         */
        @JvmStatic
        fun getInstance(context: Context): DictionaryDatabase? {

            // Use the application context, which will ensure that you do not accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (instance == null) {
                Log.d(TAG, "Creating new DictionaryDatabase object")
                instance = DictionaryDatabase(context.applicationContext)
            }
            return instance
        }

        /**
         * Combine all SQL sub-queries in the list to a single SQL query with the UNION statement. All fragments
         * must be valid in the context of a UNION statement.
         *
         * @param queries The SQL queries to be combined.
         * @return A SQL string having all sub-queries combined.
         */
        private fun unionize(queries: List<String>): String {
            val result = StringBuilder()
            queries.forEach { query ->
                if (result.isNotEmpty()) {
                    result.append(" UNION ")
                }
                result.append(query)
            }
            return result.toString()
        }
    }
}