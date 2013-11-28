package uk.co.minter.ottrss;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import java.util.List;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private AccountManager am;
	private SharedPreferences sp;
	private EditTextPreference url, username, password;
	private ListPreference period, articles;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		am = AccountManager.get(getActivity());
		sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		url = (EditTextPreference)findPreference("url");
		username = (EditTextPreference)findPreference("username");
		password = (EditTextPreference)findPreference("password");
		period = (ListPreference)findPreference("period");
		articles = (ListPreference)findPreference("articles");

		AccountPreferenceListener apl = new AccountPreferenceListener();
		username.setOnPreferenceChangeListener(apl);
		password.setOnPreferenceChangeListener(apl);

		period.setOnPreferenceChangeListener(new PeriodPreferenceListener());
	}

	@Override
	public void onResume() {
		super.onResume();
		sp.registerOnSharedPreferenceChangeListener(this);

		Account[] accounts = am.getAccountsByType(getString(R.string.account_type));
		if(accounts.length > 0) {
			username.setText(accounts[0].name);
			password.setText(am.getPassword(accounts[0]));
			if(!ContentResolver.getSyncAutomatically(accounts[0], getString(R.string.provider_name)))
				period.setValue("0");
			else {
				List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(accounts[0], getString(R.string.provider_name));
				period.setValue(Long.toString(syncs.get(0).period));
			}
		} else {
			username.setText("");
			password.setText("");
			period.setValue("0");
		}

		onSharedPreferenceChanged(sp, null);
	}

	@Override
	public void onPause() {
		super.onPause();
		sp.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		url.setSummary(url.getText());
		username.setSummary(username.getText());
		period.setSummary(period.getEntry());
		articles.setSummary(articles.getEntry());
	}

	private class AccountPreferenceListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			((EditTextPreference)preference).setText((String)newValue);

			if(!username.getText().isEmpty() && !password.getText().isEmpty()) {
				for(Account account : am.getAccountsByType(getString(R.string.account_type)))
					am.removeAccount(account, null, null);

				Account account = new Account(username.getText(), getString(R.string.account_type));
				am.addAccountExplicitly(account, password.getText(), null);
			}

			onSharedPreferenceChanged(sp, null);

			return true;
		}
	}

	private class PeriodPreferenceListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			Account[] accounts = am.getAccountsByType(getString(R.string.account_type));
			if(accounts.length == 0)
				return false;

			period.setValue((String)newValue);

			if(period.getValue().equals("0")) {
				ContentResolver.setSyncAutomatically(accounts[0], getString(R.string.provider_name), false);

				for(PeriodicSync sync : ContentResolver.getPeriodicSyncs(accounts[0], getString(R.string.provider_name)))
					ContentResolver.removePeriodicSync(accounts[0], getString(R.string.provider_name), sync.extras);
			} else {
				ContentResolver.setSyncAutomatically(accounts[0], getString(R.string.provider_name), true);
				ContentResolver.addPeriodicSync(accounts[0], getString(R.string.provider_name), new Bundle(), Long.parseLong(period.getValue()));
			}

			onSharedPreferenceChanged(sp, null);

			return true;
		}
	}
}
