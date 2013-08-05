package uk.co.minter.ottrss.activities;

import android.app.Activity;
import android.os.Bundle;
import uk.co.minter.ottrss.SettingsFragment;

public class SettingsActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
}
