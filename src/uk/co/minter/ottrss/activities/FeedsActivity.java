package uk.co.minter.ottrss.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import uk.co.minter.ottrss.App;
import uk.co.minter.ottrss.R;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.api.Feed;

public class FeedsActivity extends Activity implements OnItemClickListener, OnCheckedChangeListener {
	private SharedPreferences sp;
	private SimpleCursorAdapter adapter;
	private Cursor c;

	private CheckBox cb;
	private ListView lv;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refresh();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setTitle("Feeds");

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[] { "text" }, new int[] { android.R.id.text1 }, 0);

		cb = (CheckBox)findViewById(R.id.show_all);
		cb.setOnCheckedChangeListener(this);

		lv = (ListView)findViewById(R.id.listView);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("articles"));

		cb.setChecked(sp.getBoolean("show_all", false));
		refresh();
	}

	@Override
	protected void onPause() {
		super.onPause();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		SharedPreferences.Editor e = sp.edit();
		e.putBoolean("show_all", cb.isChecked());
		e.commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(c != null)
			c.close();
	}

	public void refresh() {
		if(cb.isChecked())
			c = App.db.rawQuery("SELECT id AS _id, title || ' (' || unread || ')' AS text FROM feeds WHERE articles > 0 ORDER BY title", null);
		else
			c = App.db.rawQuery("SELECT id AS _id, title || ' (' || unread || ')' AS text FROM feeds WHERE unread > 0 ORDER BY title", null);

		Cursor oldc = adapter.swapCursor(c);
		if(oldc != null)
			oldc.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void settings(MenuItem menuitem) {
		Intent i = new Intent(this, SettingsActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(i);
	}

	public void resetDB(MenuItem menuitem) {
		Article.deleteOld(this, System.currentTimeMillis());
		Feed.deleteOld(this, System.currentTimeMillis());
	}

	public void sync(MenuItem menuitem) {
		Account[] accounts = AccountManager.get(this).getAccountsByType(getString(R.string.account_type));
		if(accounts.length == 0)
			settings(menuitem);
		else {
			Bundle b = new Bundle();
			b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			ContentResolver.requestSync(accounts[0], getString(R.string.provider_name), b);
		}
	}

	public void lastSync(MenuItem menuitem) {
		long lastSync = sp.getLong("last_sync", 0);
		String s = "Last synced at " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(lastSync));
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		refresh();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent i = new Intent(this, ArticlesActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		i.putExtra("id", (int)id);
		startActivity(i);
	}
}
