package uk.co.minter.ottrss.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import uk.co.minter.ottrss.R;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.api.Feed;
import uk.co.minter.ottrss.views.HeadlineView;
import uk.co.minter.ottrss.views.ListViewFling;
import uk.co.minter.ottrss.views.ListViewFling.OnFlingListener;

public class ArticlesActivity extends Activity implements OnCheckedChangeListener, OnItemClickListener, OnFlingListener {
	private Feed f;
	private Cursor c;

	private ArticlesAdapter adapter;
	private CheckBox cb;
	private ListViewFling lv;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int feed_id = intent.getIntExtra("feed_id", -1);
			if(feed_id == f.id || feed_id == -1)
				refresh();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		Bundle extras = getIntent().getExtras();
		f = new Feed(extras.getInt("id"));

		setTitle(f.title);

		adapter = new ArticlesAdapter(this, null, 0);

		cb = (CheckBox)findViewById(R.id.show_all);
		cb.setOnCheckedChangeListener(this);

		lv = (ListViewFling)findViewById(R.id.listView);
		lv.setOnItemClickListener(this);
		lv.setOnFlingListener(this);
		lv.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("articles"));

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		cb.setChecked(sp.getBoolean("show_all", false));

		refresh();
	}

	@Override
	protected void onPause() {
		super.onPause();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		persist();
		overridePendingTransition(0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(c != null)
			c.close();
	}

	private class ArticlesAdapter extends CursorAdapter {
		public ArticlesAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return new HeadlineView(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Article article = new Article(context, cursor);
			((HeadlineView)view).setArticle(article);
		}
	}

	private void refresh() {
		c = Article.getArticlesCursor(f.id, cb.isChecked());
		Cursor oldc = adapter.swapCursor(c);
		if(oldc != null)
			oldc.close();
	}

	private void persist() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor e = sp.edit();
		e.putBoolean("show_all", cb.isChecked());
		e.commit();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		refresh();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent i = new Intent(this, WebViewActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		i.putExtra("feed_id", f.id);
		i.putExtra("position", position);
		startActivity(i);
	}

	@Override
	public void onFlingLeft(AdapterView<?> parent, View view, int position, long id) {
		((HeadlineView)view).flingLeft();
	}

	@Override
	public void onFlingRight(AdapterView<?> parent, View view, int position, long id) {
		((HeadlineView)view).flingRight();
	}
}
