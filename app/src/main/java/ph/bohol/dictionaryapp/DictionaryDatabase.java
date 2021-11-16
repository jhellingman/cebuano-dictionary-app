package ph.bohol.dictionaryapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ph.bohol.util.normalizer.CebuanoNormalizer;
import ph.bohol.util.stemmer.Derivation;
import ph.bohol.util.stemmer.RootWordProvider;

public final class DictionaryDatabase extends SQLiteAssetHelper
        implements RootWordProvider {

    static final String HEAD_HEAD = "head";
    static final String HEAD_ENTRY_ID = "entryid";
    static final String HEAD_DERIVATION = "derivation";
    static final String HEAD_TYPE = "type";
    static final String ENTRY_ENTRY = "entry";
    static final String ENTRY_HEAD = "head";

    private static final String ENTRY_ID = "_id";
    private static final int MIN_ROOT_LENGTH = 3;
    private static final String DATABASE_NAME = "dictionary_database";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "DictionaryDatabase";

    private static final int ENTRY_CACHE_SIZE = 100;
    private static final Map<Integer, Spanned> entryCache = Collections.synchronizedMap(new EntryCache(ENTRY_CACHE_SIZE));
    private static final int ROOT_CACHE_SIZE = 1000;
    private static final Map<String, Boolean> rootCache = Collections.synchronizedMap(new RootCache(ROOT_CACHE_SIZE));

    private static DictionaryDatabase instance = null;

    private Context context;

    /**
     * Create a new DictionaryDatabase object. Prevent resource leaks by using this only as a singleton, using the
     * getInstance() method.
     *
     * @param newContext the application context.
     */
    private DictionaryDatabase(final Context newContext) {
        super(newContext, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = newContext;
    }

    /**
     * Get the instance of the DictionaryDatabase singleton. Create it if it is not yet available.
     *
     * @param context a context. The application context will be obtained from this context.
     * @return the instance of the DictionaryDatabase singleton.
     */
    static DictionaryDatabase getInstance(final Context context) {
        // Use the application context, which will ensure that you do not accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (instance == null) {
            Log.d(TAG, "Creating new DictionaryDatabase object");
            instance = new DictionaryDatabase(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Combine all SQL sub-queries in the list to a single SQL query with the UNION statement. All fragments
     * must be valid in the context of a UNION statement.
     *
     * @param queries The SQL queries to be combined.
     * @return A SQL string having all sub-queries combined.
     */
    private static String unionize(final List<String> queries) {
        StringBuilder result = new StringBuilder();
        for (String query : queries) {
            if (result.length() > 0) {
                result.append(" UNION ");
            }
            result.append(query);
        }
        return result.toString();
    }

    @Override
    public boolean isRootWord(final String root) {
        if (root.length() < MIN_ROOT_LENGTH) {
            return false;
        }

        Boolean isRoot = rootCache.get(root);
        if (isRoot != null) {
            return isRoot;
        }

        Log.d(TAG, "Query for root: " + root);
        String sqlQuery = "SELECT 1 FROM WCED_head WHERE normalized_head = ? AND pos != '' LIMIT 1";
        String[] selectionArguments = {root};
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(sqlQuery, selectionArguments)) {
            boolean result = cursor.getCount() > 0;
            rootCache.put(root, result);
            return result;
        }
    }

    @Override
    public boolean isRootWordWithType(final String root, final String type) {
        if (root.length() < MIN_ROOT_LENGTH) {
            return false;
        }

        Boolean isRoot = rootCache.get(root + "." + type);
        if (isRoot != null) {
            return isRoot;
        }

        Log.d(TAG, "Query for root: " + root + " with type: " + type);
        String sqlQuery = "SELECT 1 FROM WCED_head WHERE normalized_head = ? AND pos LIKE ? LIMIT 1";
        String[] selectionArguments = {root, "%" + type + "%"};
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(sqlQuery, selectionArguments)) {
            boolean result = cursor.getCount() > 0;
            rootCache.put(root + "." + type, result);
            return result;
        }
    }

    Cursor getHeads(final String head, final boolean reverseLookup, final List<Derivation> derivations) {
        CebuanoNormalizer n = new CebuanoNormalizer();
        String normalizedHead = n.normalize(head);

        List<String> subQueries = new LinkedList<>();
        List<String> arguments = new ArrayList<>();

        subQueries.add("SELECT _id, entryid, head, normalized_head, NULL AS derivation, 'n' AS type "
                + "FROM WCED_head WHERE normalized_head LIKE ?");
        arguments.add(normalizedHead + "%");

        if (reverseLookup) {
            subQueries.add("SELECT _id, entryid, translation as head, translation as normalized_head, "
                    + "NULL as derivation, 'r' AS type "
                    + "FROM WCED_translation WHERE translation LIKE ?");
            arguments.add(head + "%");
        }

        if (derivations != null) {
            final String snippetFormat =
                    "SELECT _id, entryid, head, normalized_head, '%s' AS derivation, 'd' AS type FROM wced_head "
                            + "WHERE normalized_head = ?";

            for (Derivation derivation : derivations) {
                String snippet = String.format(snippetFormat, derivation.toString().replace("'", "''"));
                subQueries.add(snippet);
                arguments.add(derivation.getRoot());
            }
        }

        String query = unionize(subQueries);
        query += " ORDER BY normalized_head COLLATE NOCASE";

        Log.d(TAG, "Query for heads and derived forms: " + head);
        Log.d(TAG, "Query SQL: " + query);

        String[] selectionArguments = new String[arguments.size()];
        arguments.toArray(selectionArguments);

        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(query, selectionArguments);
    }

    Cursor getEntry(final int entryId) {
        String sqlQuery = "SELECT * FROM WCED_entry WHERE _id = ?";
        String[] selectionArguments = {Integer.toString(entryId)};
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(sqlQuery, selectionArguments);
        cursor.moveToFirst();
        return cursor;
    }

    private String getEntryHtml(final int entryId) {
        Cursor cursor = getEntry(entryId);
        int entryIndex = cursor.getColumnIndex(DictionaryDatabase.ENTRY_ENTRY);
        if (entryIndex < 0) return ""; // This is an error
        String entryXml = cursor.getString(entryIndex);
        cursor.close();
        return EntryTransformer.getInstance(context).transform(entryXml, EntryTransformer.STYLE_COMPACT);
    }

    /**
     * Get a Spanned object with the rich-text content of an entry.
     *
     * @param entryId The entryId for which the rich-text is wanted.
     * @return a Spanned object with the content of the entry.
     */
    Spanned getEntrySpanned(final int entryId) {
        Spanned entrySpanned = entryCache.get(entryId);
        if (entrySpanned == null) {
            Log.d(TAG, "Getting entry rendered in Spanned for entryId: " + entryId);

            String entryHtml = getEntryHtml(entryId);
            entrySpanned = Html.fromHtml(entryHtml);
            entryCache.put(entryId, entrySpanned);
        }
        return entrySpanned;
    }

    /**
     * Find the entryId of the next entry.
     *
     * @param entryId The entryId of which the next entry is sought.
     * @return the next entryId, or the original entryId if this entry is the last.
     */
    int getNextEntryId(final int entryId) {
        String sqlQuery = "SELECT _id FROM WCED_entry WHERE _id > ? ORDER BY _id LIMIT 1";
        String[] selectionArguments = {Integer.toString(entryId)};

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(sqlQuery, selectionArguments)) {
            if (cursor.getCount() == 0) {
                return entryId;
            }
            cursor.moveToFirst();
            int entryIndex = cursor.getColumnIndex(ENTRY_ID);
            if (entryIndex < 0) return entryId; // This is an error
            return cursor.getInt(entryIndex);
        }
    }

    /**
     * Find the entryId of the previous entry.
     *
     * @param entryId The entryId of which the previous entry is sought.
     * @return the previous entryId, or the original entryId if this entry is the first.
     */
    int getPreviousEntryId(final int entryId) {
        String sqlQuery = "SELECT _id FROM WCED_entry WHERE _id < ? ORDER BY _id DESC LIMIT 1";
        String[] selectionArguments = {Integer.toString(entryId)};

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(sqlQuery, selectionArguments)) {
            if (cursor.getCount() == 0) {
                return entryId;
            }
            cursor.moveToFirst();
            int entryIndex = cursor.getColumnIndex(ENTRY_ID);
            if (entryIndex < 0) return entryId; // This is an error
            return cursor.getInt(entryIndex);
        }
    }
}
