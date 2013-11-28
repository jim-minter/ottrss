package uk.co.minter.ottrss.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import uk.co.minter.ottrss.R;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.utils.Fling;

public class HeadlineView extends RelativeLayout {
	private Article article;
	private ImageView iv;
	private TextView tv1, tv2, tv3;
	private CheckBox cb;
	private GestureDetector gestureDetector;
	private boolean handleFling = false;

	public HeadlineView(Context context) {
		super(context);
		init(context, null);
	}

	public HeadlineView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public HeadlineView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		LayoutInflater.from(context).inflate(R.layout.layout_headlineview, this, true);
		setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		gestureDetector = new GestureDetector(getContext(), new GestureListener());

		iv = (ImageView)findViewById(R.id.bullet);
		tv1 = (TextView)findViewById(R.id.title);
		tv2 = (TextView)findViewById(R.id.author);
		tv3 = (TextView)findViewById(R.id.updated);
		cb = (CheckBox)findViewById(R.id.marked);
		cb.setOnCheckedChangeListener(new CheckBoxListener());

		if(attrs != null) {
			TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.HeadlineView);
			handleFling = arr.getBoolean(R.styleable.HeadlineView_handle_fling, false);
			arr.recycle();
		}

		if(handleFling) {
			tv1.setMaxLines(Integer.MAX_VALUE);
			tv1.setEllipsize(null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return handleFling ? gestureDetector.onTouchEvent(event) : super.onTouchEvent(event);
	}

	public void setArticle(Article article) {
		cb.setOnCheckedChangeListener(null);
		this.article = article;
		refresh();
		cb.setOnCheckedChangeListener(new CheckBoxListener());
	}

	void refresh() {
		iv.setImageResource(article.unread == Article.UnreadState.READ ? R.drawable.bullet_white : article.unread == Article.UnreadState.UNREAD ? R.drawable.bullet_green : R.drawable.bullet_blue);
		tv1.setText(article.title);
		tv2.setText(article.author);
		tv2.setMaxLines(article.author == null ? 0 : 1);
		tv3.setText(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date(article.updated * 1000L)));
		cb.setChecked(article.marked);
	}

	public void flingLeft() {
		article.advanceUnreadState();
		article.update(true);
		refresh();
	}

	public void flingRight() {
		article.reverseUnreadState();
		article.update(true);
		refresh();
	}

	private class CheckBoxListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			article.marked = cb.isChecked();
			article.update(true);
		}
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			switch(Fling.isFling(e1, e2)) {
				case -1:
					flingLeft();
					return true;
				case 1:
					flingRight();
					return true;
				default:
					return false;
			}
		}
	}
}
