package uk.co.minter.ottrss.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import uk.co.minter.ottrss.R;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.views.HeadlineView;

public class WebViewActivity extends Activity {
	private Article article;
	private HeadlineView hv;
	private View leftzone, rightzone;
	private WebView wv;
	private Cursor c;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle extras = getIntent().getExtras();
		c = Article.getArticlesCursor(extras.getInt("feed_id"), sp.getBoolean("show_all", false));
		c.moveToPosition(extras.getInt("position"));

		hv = (HeadlineView)findViewById(R.id.headlineView);
		wv = (WebView)findViewById(R.id.webView);
		wv.setOnTouchListener(new WebViewTouchListener());

		WebSettings ws = wv.getSettings();
		ws.setDefaultTextEncodingName("UTF-8");
		ws.setJavaScriptEnabled(true);
		ws.setBuiltInZoomControls(true);
		ws.setDisplayZoomControls(false);

		leftzone = findViewById(R.id.leftZone);
		leftzone.setOnClickListener(new ZoneClickListener());

		rightzone = findViewById(R.id.rightZone);
		rightzone.setOnClickListener(new ZoneClickListener());

		refresh();
	}

	@Override
	protected void onPause() {
		super.onPause();
		overridePendingTransition(0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		c.close();
	}

	void refresh() {
		article = new Article(this, c);
		article.markRead();

		hv.setArticle(article);
		wv.loadUrl(article.getURL());
	}

	private class ZoneClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if(v == rightzone && !c.isLast()) {
				c.moveToNext();
				refresh();
			} else if(v == leftzone && !c.isFirst()) {
				c.moveToPrevious();
				refresh();
			}
		}
	}

	private class WebViewTouchListener implements OnTouchListener {
		private void animateOut() {
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					leftzone.setAlpha(0);
					rightzone.setAlpha(0);
				}
			}, 1000);
		}

		private void animateIn() {
			if(!c.isFirst())
				leftzone.setAlpha(1);
			if(!c.isLast())
				rightzone.setAlpha(1);
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getActionMasked() == MotionEvent.ACTION_DOWN)
				animateIn();
			else if(event.getActionMasked() == MotionEvent.ACTION_UP)
				animateOut();

			return false;
		}
	}
}
