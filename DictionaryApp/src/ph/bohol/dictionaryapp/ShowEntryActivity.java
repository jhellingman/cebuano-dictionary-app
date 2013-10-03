package ph.bohol.dictionaryapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ShowEntryActivity extends Activity
{
	private int entryId;
	
	private int fontSize = 20;
	private boolean expandAbbreviations = false;
	private String presentationStyle = EntryTransformer.STYLE_TRADITIONAL;
	
	private EntryTransformer entryTransformer = null;
	
	private static final int RESULT_SETTINGS = 1;
		
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private static final String ENTRY_ID = "ph.bohol.dictionaryapp.ENTRY_ID";

	private static final String TAG = "ShowEntryActivity";
	
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_entry);
		
		Log.d(TAG, "OnCreate");
		
		overridePendingTransition(R.anim.right_in, R.anim.left_out);
		
		// Show the Up button in the action bar.
		setupActionBar();
		
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() 
        {
            public boolean onTouch(View v, MotionEvent event) 
            {
                return gestureDetector.onTouchEvent(event);
            }
        };
		
		retrievePreferences();		
		entryTransformer = new EntryTransformer(this);
		
		// Get entryId after resume (e.g. after a rotation).
        if (savedInstanceState != null)
        {
        	entryId = savedInstanceState.getInt(ENTRY_ID);
        }
        else
        {
        	Intent intent = getIntent();
			entryId = Integer.parseInt(intent.getStringExtra(MainActivity.ENTRY_ID));
        }
				
		showEntry();    	       
	}
	
    @Override
    public void onSaveInstanceState(Bundle outState) 
    {
         outState.putInt(ENTRY_ID, entryId);
         super.onSaveInstanceState(outState);
    }
	
	private void showEntry()
	{		
		DictionaryDatabase database = DictionaryDatabase.getInstance(this);	
		Cursor cursor = database.getEntry(entryId);
		try
		{
			String entry = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.ENTRY_ENTRY));
			String head = cursor.getString(cursor.getColumnIndex(DictionaryDatabase.ENTRY_HEAD));
			setTitle(head);
			
			String htmlEntry = transformEntry(entry);
			
			if (htmlEntry != null)
			{
		        WebView webView = (WebView) this.findViewById(R.id.webViewEntry);
		        if (webView != null)
		        {
		    		webView.setWebViewClient(new WebViewClient() 
		    		{
		    		    @Override
		    		    public boolean shouldOverrideUrlLoading(WebView view, String url) 
		    		    {
		    		        if (url.startsWith("search:")) 
		    		        {
		    					// Send back to main. This way, links clicked in the data do not result in opening lots 
		    		        	// of MainActivity instances.		    		 
		    		            String searchWord = url.substring(7);
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
		finally
		{
			cursor.close();
		}
	}

	private String transformEntry(String entry)
	{
		entry = "<dictionary>" + entry + "</dictionary>";
		
		entryTransformer.setExpandAbbreviations(expandAbbreviations);
		entryTransformer.setFontSize(fontSize);
		return entryTransformer.transform(entry, presentationStyle);		 
	}
	
	private void retrievePreferences()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		expandAbbreviations = preferences.getBoolean(DictionaryPreferenceActivity.KEY_EXPAND_ABBREVIATIONS, false);		
		fontSize = Integer.parseInt(preferences.getString(DictionaryPreferenceActivity.KEY_PRESENTATION_FONT_SIZE, "20"));		
		presentationStyle = preferences.getString(DictionaryPreferenceActivity.KEY_PRESENTATION_STYLE, EntryTransformer.STYLE_TRADITIONAL);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar()
	{
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search_results, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
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
			moveToNextEntry();
			break;
			
		case R.id.action_previous:
			moveToPreviousEntry();
			break;
			
		case R.id.action_settings:
			Intent i = new Intent(this, DictionaryPreferenceActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;
			
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void moveToPreviousEntry()
	{
		DictionaryDatabase database = DictionaryDatabase.getInstance(this);	
		int newEntryId = database.getPreviousEntryId(entryId);
		if (newEntryId == entryId)
		{
			Toast.makeText(this, getResources().getString(R.string.reached_first_entry), Toast.LENGTH_SHORT).show();
		}
		entryId = newEntryId;
		showEntry();		
	}

	private void moveToNextEntry()
	{
		DictionaryDatabase database = DictionaryDatabase.getInstance(this);	
		int newEntryId = database.getNextEntryId(entryId);
		if (newEntryId == entryId)
		{
			Toast.makeText(this, getResources().getString(R.string.reached_last_entry), Toast.LENGTH_SHORT).show();
		}
		entryId = newEntryId;
		showEntry();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) 
		{
			case RESULT_SETTINGS:
				retrievePreferences();
				showEntry();
				break;
		}
	}	
	
	class MyGestureDetector extends SimpleOnGestureListener
	{
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			try
			{
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				{
					return false;
				}
				
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
				{
					moveToNextEntry();
				}
				else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
				{
					moveToPreviousEntry();
				}
			}
			catch (Exception e)
			{
				// nothing
			}
			return false;
		}
	}
	
}