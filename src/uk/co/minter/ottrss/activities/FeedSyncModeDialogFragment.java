package uk.co.minter.ottrss.activities;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import uk.co.minter.ottrss.R;
import uk.co.minter.ottrss.api.Feed;
import uk.co.minter.ottrss.api.Feed.SyncMode;

public class FeedSyncModeDialogFragment extends DialogFragment {
	public Feed f;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.pick_sync_mode);
		builder.setSingleChoiceItems(R.array.sync_mode, f.syncmode.ordinal(), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				f.syncmode = SyncMode.values()[which];
				f.update();
				dismiss();
			}
		});
		return builder.create();
	}
}
