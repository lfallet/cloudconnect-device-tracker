package name.fallet.cloudconnect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Stocke les identifiants de connexion.
 * 
 * @author lfallet
 */
public class MobileDevicesPreferenceActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = MobileDevicesPreferenceActivity.class.getSimpleName();

	/**
	 * recharger les valeurs stockées et les positionner dans les champs est fait automatiquement ?
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// registering is mandatory
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(MobileDevicesPreferenceActivity.this);

		// une vue n'est pas nécessaire, elle est gérée par le framework
		// setContentView(R.layout.settings);
	}

	/**
	 * Éventuelle alternative au rechargement des préférences à tout moment
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "Appel de onSharedPreferenceChanged(...)");

		// TODO recréer les objets

	}

}
