package name.fallet.cloudconnect;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Stocke les identifiants de connexion.
 * 
 * @author lfallet
 */
public class MobileDevicesPreferenceActivity extends PreferenceActivity {

	/**
	 * recharger les valeurs stockées et les positionner dans les champs est fait automatiquement ?
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// une vue n'est pas nécessaire, elle est gérée par le framework
		// setContentView(R.layout.settings);
	}

}
