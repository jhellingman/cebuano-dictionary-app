package ph.bohol.dictionaryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import static ph.bohol.dictionaryapp.DictionaryPreferenceActivity.*;

class HeadCursorAdapter extends CursorAdapter {
    private final LayoutInflater layoutInflater;
    private boolean showPreview;

    @SuppressWarnings("deprecation")
    HeadCursorAdapter(final Context context, final Cursor cursor) {
        super(context, cursor);
        layoutInflater = LayoutInflater.from(context);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        showPreview = sharedPreferences.getBoolean(KEY_SHOW_PREVIEW, true);
    }

    @Override
    public final void bindView(final View view, final Context context, final Cursor cursor) {
        String head = cursor.getString(cursor.getColumnIndexOrThrow(DictionaryDatabase.HEAD_HEAD));

        String derivation = cursor.getString(cursor.getColumnIndexOrThrow(DictionaryDatabase.HEAD_DERIVATION));
        if (derivation != null && !derivation.isEmpty()) {
            head += " (" + derivation + ")";
        }

        TextView textview = view.findViewById(R.id.head);
        textview.setText(head);

        String entryIdString = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.HEAD_ENTRY_ID));
        int entryId = Integer.parseInt(entryIdString);

        if (showPreview) {
            // Loading a preview of an entry is a slow operation, so don't do it on the UI thread.
            TextView detailTextView = view.findViewById(R.id.details);
            detailTextView.setText("");
            FetchEntryDetailsTask task = new FetchEntryDetailsTask(context, detailTextView);
            task.execute(entryId);

            // Show the match-type using an icon, only if an imageView is in the view.
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DictionaryDatabase.HEAD_TYPE));
            if (type != null) {
                ImageView imageView = view.findViewById(R.id.icon);
                if (imageView != null) {
                    if (type.equals("d")) {
                        imageView.setImageResource(R.drawable.ic_tilde);
                        imageView.setVisibility(ImageView.VISIBLE);
                    } else if (type.equals("r")) {
                        imageView.setImageResource(R.drawable.ic_left_arrow);
                        imageView.setVisibility(ImageView.VISIBLE);
                    } else {
                        // imageView.setImageResource(R.drawable.ic_diamonds);
                        imageView.setVisibility(ImageView.INVISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public final View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        if (showPreview) {
            return layoutInflater.inflate(R.layout.heading_row_multiline, parent, false);
        } else {
            return layoutInflater.inflate(R.layout.heading_row, parent, false);
        }
    }

    // See
    // http://www.vogella.com/articles/AndroidBackgroundProcessing/article.html
    // http://stackoverflow.com/questions/8965771/android-asynctask-update-to-the-listview-in-postexecute
    // http://android-developers.blogspot.nl/2010/07/multithreading-for-performance.html
    private class FetchEntryDetailsTask extends AsyncTask<Integer, Void, Spanned> {
        private final Context context;
        private final WeakReference<TextView> detailTextViewReference;

        FetchEntryDetailsTask(final Context newContext, final TextView detailTextView) {
            this.context = newContext;
            this.detailTextViewReference = new WeakReference<>(detailTextView);

            // Are we already working for this TextView? (And the ListView re-used it during scrolling)
            if (detailTextView.getTag() instanceof FetchEntryDetailsTask) {
                FetchEntryDetailsTask task = (FetchEntryDetailsTask) detailTextView.getTag();
                task.cancel(true);
            }
            detailTextView.setTag(this);
        }

        @Override
        protected Spanned doInBackground(final Integer... entryId) {
            DictionaryDatabase database = DictionaryDatabase.getInstance(context);
            return database.getEntrySpanned(entryId[0]);
        }

        @Override
        protected void onPostExecute(final Spanned result) {
            if (isCancelled()) {
                return;
            }

            if (detailTextViewReference != null) {
                TextView detailTextView = detailTextViewReference.get();
                if (detailTextView != null) {
                    detailTextView.setText(result, TextView.BufferType.SPANNABLE);
                    detailTextView.setTag(null);
                }
            }
        }
    }
}
