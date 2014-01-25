package name.fallet.cloudconnect;

import java.util.Collection;

import name.fallet.cloudconnect.model.ApiException;
import name.fallet.cloudconnect.model.ConnectionParameters;
import name.fallet.cloudconnect.model.LocatedDevice;
import name.fallet.cloudconnect.model.ViewParameters;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.welaika.android.errbit.ErrbitNotifier;

/**
 * 
 * Pour changer l'orientation de l'écran : Ctrl + Windows + F11
 * 
 * @author lfallet
 */
public class MainActivity extends android.support.v4.app.FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleMap googleMap;
    public final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 42; // value doesn't matter

    private final MobileDevicesApiHelper mdApiHelper = new MobileDevicesApiHelper();

    // approximativement le centre de Paris
    private static final LatLng initLatLng = new LatLng(48.863850, 2.338170);
    private static final int ZOOM_PAR_DEFAUT = 12;
    private static final float RATIO_ZOOM_TO_SPAN = 1.1f;
    private static final String ZOOM_LEVEL_BUNDLE_KEY = "zlbk";
    private static final String MAP_CENTER_LAT_BUNDLE_KEY = "mcLatbk";
    private static final String MAP_CENTER_LNG_BUNDLE_KEY = "mcLngbk";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        registerErrbitNotifier();

        googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview)).getMap();
        // check if google maps is available, install if not
        if (isGoogleMapsInstalled()) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                // TODO restore data in case of configChange
                // final Collection<LocatedDevice> data = (Collection<LocatedDevice>) getLastNonConfigurationInstance();
                // if (data == null) {
                // Log.d(TAG, "Démarrage from scratch, pas de données");
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initLatLng, ZOOM_PAR_DEFAUT));
                // } else {
                // Log.d(TAG, "Redessin des données mémorisées");
                // // on restore
                // Collection<LocatedDevice> locatedDevices = data;
                // // relancer le dessin de ces devices
                // final ViewParameters viewParameters = initializeDisplayParameters();
                // displayDevicesOnMap(locatedDevices, viewParameters);
                // }
            }
        } else {
            Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.install_google_map));
            builder.setCancelable(false);
            builder.setPositiveButton(getResources().getString(R.string.install_google_map_btn), getGoogleMapsListener());
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        final Intent intent = getIntent();
        // if the activity is created from search
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "ACTION_SEARCH intent with query " + query);
            searchDeviceByUnitId(query);
        }
    }

    /** initialization of errbit bug report */
    public void registerErrbitNotifier() {
        if (getErrbitParameters()) {
            ErrbitNotifier.register(this, "errbit.clubchauffeur.com", "a6ae624b1a7a2570a25c01f8b12e5f40", "test", false);
        }
    }

    /**
     * Handles search
     * 
     * @param query
     *            unit_id
     */
    private void searchDeviceByUnitId(final String query) {
        if (TextUtils.isEmpty(query)) {
            Toast toast = Toast.makeText(MainActivity.this.getApplicationContext(), getResources().getText(R.string.emptyQuery),
                    Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Log.d(TAG, "Chaîne recherchée : " + query);
        Collection<LocatedDevice> locatedDevices = MarkersManager.getInstance().getLocatedDevices();
        if (locatedDevices.isEmpty()) {
            Toast toast = Toast.makeText(MainActivity.this.getApplicationContext(), getResources().getText(R.string.noDeviceKnown),
                    Toast.LENGTH_LONG);
            toast.show();
        } else {
            // chercher parmi les devices connus
            LocatedDevice deviceMatchingSearch = null;
            for (LocatedDevice locatedDevice : locatedDevices) {
                boolean isMatchingQuery = query.equalsIgnoreCase(String.valueOf(locatedDevice.getId()))
                        || query.equalsIgnoreCase(String.valueOf(locatedDevice.getModid()));
                if (isMatchingQuery) {
                    deviceMatchingSearch = locatedDevice;
                    Log.d(TAG, "Matching result : unitid " + deviceMatchingSearch.getId() + ", imei " + deviceMatchingSearch.getModid());
                    break;
                }
            }
            if (null == deviceMatchingSearch) { // no result
                Toast toast = Toast.makeText(MainActivity.this.getApplicationContext(), getResources().getText(R.string.noDeviceFound),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else { // centrer sur le device trouvé
                String currentLatLng = googleMap.getCameraPosition().target.latitude + "," + googleMap.getCameraPosition().target.longitude;

                String nextLatLng = deviceMatchingSearch.getLat() + "," + deviceMatchingSearch.getLng();
                float newZoom = googleMap.getCameraPosition().zoom + 1;
                Log.d(TAG, "move lat,lng from " + currentLatLng + " to " + nextLatLng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(deviceMatchingSearch.getLat(), deviceMatchingSearch.getLng()), newZoom));
            }
        }
    }

    /**
     * To retain an object during a runtime configuration change
     * 
     * - ne pas oublier d'appeler getLastNonConfigurationInstance() to recover your object.<br>
     * - ne jamais conserver des View, Drawable, etc sinon un window leak va survenir
     */
    // @Override
    // public Object onRetainNonConfigurationInstance() {
    // final Collection<LocatedDevice> data = ((CloudConnectApplication) this.getApplication()).getLocatedDevices();
    // return data;
    // }

    /**
     * When such a screen orientation occurs, Android restarts the running Activity (onDestroy() is called, followed by onCreate()). To
     * properly handle a restart, it is important that your activity restores its previous state through the normal Activity lifecycle, in
     * which Android calls onSaveInstanceState() before it destroys your activity so that you can save data about the application state.
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Appel de onSaveInstanceState() sur l'activité");

        // final MapView mapView = (MapView) findViewById(R.id.mapview);
        outState.putSerializable(ZOOM_LEVEL_BUNDLE_KEY, googleMap.getCameraPosition().zoom);
        // TODO memorize view
        // outState.putSerializable(MAP_CENTER_LAT_BUNDLE_KEY, googleMap.getProjection().getVisibleRegion().getLatitudeE6());
        // outState.putSerializable(MAP_CENTER_LNG_BUNDLE_KEY, mapView.getMapCenter().getLongitudeE6());

        super.onSaveInstanceState(outState);
    }

    /**
     * Restore the state during onCreate() or onRestoreInstanceState()
     * 
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "Appel de onRestoreInstanceState() sur l'activité");

        final Integer lat = (Integer) savedInstanceState.get(MAP_CENTER_LAT_BUNDLE_KEY), lng = (Integer) savedInstanceState
                .get(MAP_CENTER_LNG_BUNDLE_KEY);
        if (lat != null && lng != null) {
            LatLng ancienCentre = new LatLng(lat, lng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(ancienCentre));
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        checkGooglePlayServicesAvailability();
        super.onResume();
    }

    public boolean checkGooglePlayServicesAvailability() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        Log.d("GooglePlayServicesUtil", "Result of GooglePlayServices check: " + resultCode + " (" + ConnectionResult.SUCCESS
                + " is success)");

        if (resultCode != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, REQUEST_CODE_RECOVER_PLAY_SERVICES);
            dialog.setCancelable(false);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            dialog.show();
            return false;
        }

        return true;
    }

    public boolean isGoogleMapsInstalled() {
        try {
            getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public OnClickListener getGoogleMapsListener() {
        return new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                startActivity(intent);

                // Finish the activity so they can't circumvent the check
                finish();
            }
        };
    }

    /** Check internet availability */
    public boolean isConnectedToNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * Reçoit l'interaction du bouton rafraîchir :
     * <ul>
     * <li>affiche un toast en cas de paramètres de connexion manquant (bloquant)</li>
     * <li>affiche un toast si aucune connexion internet n'est active (bloquant)</li>
     * <li>crée une tâche asynchrone pour contacter la g8teway</li>
     * <li>(via asyncTask) redessine les overlay de la ggmap</li>
     * </ul>
     */
    public void refreshOverlay(View v) {

        // are parameters set ?
        final ConnectionParameters connectionParameters = initializeConnectionParameters();
        if (connectionParameters.isMissingAtLeastOneMandatoryValue()) {
            Toast toast = Toast.makeText(MainActivity.this.getApplicationContext(),
                    getResources().getText(R.string.connectionParamsNotSet), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // is network fine ?
        if (!isConnectedToNetwork(MainActivity.this.getApplicationContext())) {
            Toast toast = Toast.makeText(MainActivity.this.getApplicationContext(), getResources().getText(R.string.networkNotConnected),
                    Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // récupération asynchrone des données
        new DeviceDataFetcherTask().execute(connectionParameters);
    }

    /** */
    private ConnectionParameters initializeConnectionParameters() {
        ConnectionParameters connectionParameters = new ConnectionParameters();
        // récupérer les préférences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        connectionParameters.setUser(sharedPrefs.getString("user", null));
        connectionParameters.setPassword(sharedPrefs.getString("password", null));
        connectionParameters.setClient(sharedPrefs.getString("client", null));
        connectionParameters.setUrl(sharedPrefs.getString("url", null));
        return connectionParameters;
    }

    /** */
    private boolean getErrbitParameters() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getBoolean("sendErrbitReports", false);
    }

    /** */
    private ViewParameters initializeDisplayParameters() {
        ViewParameters displayParameters = new ViewParameters();
        // récupérer les préférences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        displayParameters.displayInactiveDevices = sharedPrefs.getBoolean("displayInactiveDevices", true);
        String recentValue = sharedPrefs.getString("definitionOfRecentDevicesInMinutes", ViewParameters.DEFAULT_RECENT_VALUE.toString());
        Log.d(TAG, "recentValue : " + recentValue);
        Integer recentValueInt = Integer.getInteger(recentValue);
        if (recentValueInt == null) {
            Log.d(TAG, "recentValueInt is null");
            displayParameters.relativeTimeRecentDevicesInMinutes = ViewParameters.DEFAULT_RECENT_VALUE;
        } else {
            Log.v(TAG, "working fine");
            displayParameters.relativeTimeRecentDevicesInMinutes = recentValueInt.intValue();
        }

        // highlight
        String highlightedDevices = sharedPrefs.getString("highlightedDevices", ViewParameters.DEFAULT_RECENT_VALUE.toString());
        if (!highlightedDevices.isEmpty()) {
            String[] unitids = highlightedDevices.split(",");
            for (String unitId : unitids) {
                if (unitId.matches("[0-9]*")) {
                    displayParameters.highlightedUnitIds.add(Integer.valueOf(unitId));
                } else {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "Incorrect definition of highlightedDevices",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        return displayParameters;
    }

    /**
     * Boîte de dialogue de rapport d'erreur
     */
    private void creerDialogueException(String details) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // FIXME : en cas de non connectivité aux réseaux mobiles, l'erreur
        // arrive ici et le builder plante
        builder.setTitle("Exception raised");
        builder.setMessage(details);
        builder.setNeutralButton(getResources().getText(R.string.close), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Appel de onStop() sur l'activité");
        mdApiHelper.closeConnection();
    }

    /**
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menuRefresh:
            refreshOverlay(findViewById(R.id.mapview));
            return true;
        case R.id.menuSearch:
            return onSearchRequested();
        case R.id.menuSettings:
            displayPreferenceActivity();
            return true;
        case R.id.menuCredits:
            createCreditsDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /** Inherit android search behavior */
    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        if (Intent.ACTION_SEARCH.equals(newIntent.getAction())) {
            Log.i(TAG, "newIntent ACTION_SEARCH");
            final String query = newIntent.getStringExtra(SearchManager.QUERY);
            searchDeviceByUnitId(query);
        }
    }

    /**
     * through an Intent
     */
    private void displayPreferenceActivity() {
        Intent preferencesIntent = new Intent(this, MobileDevicesPreferenceActivity.class);
        startActivity(preferencesIntent);
    }

    /** */
    private void createCreditsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.creditsTitle);
        builder.setMessage(R.string.creditsText);
        builder.setNeutralButton(getResources().getText(R.string.close), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Get information from g8teway api.
     * 
     * @author lfallet
     */
    private class DeviceDataFetcherTask extends AsyncTask<ConnectionParameters, Void, Collection<LocatedDevice>> {

        private ProgressDialog progressDialog;

        private final MobileDevicesApiHelper mdApiHelper = new MobileDevicesApiHelper();

        private boolean exceptionRaised = false;

        /**
         * Lancer le dialogue d'avancement (attente)
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "", getResources().getText(R.string.progress_dialog), true);
        }

        @Override
        protected Collection<LocatedDevice> doInBackground(ConnectionParameters... params) {
            try {
                return mdApiHelper.recupererPositionVehicules(params[0], false);
            } catch (ApiException e) {
                e.printStackTrace();
                exceptionRaised = true;
            }
            return null;
        }

        /**
         * Masquer le dialogue d'avancement. Afficher un toast.
         */
        @Override
        protected void onPostExecute(Collection<LocatedDevice> locatedDevices) {
            super.onPostExecute(locatedDevices);

            if (exceptionRaised) {
                progressDialog.dismiss();

                // FIXME : pas de création de dialogue dans une async task
                creerDialogueException("Request failed, please retry later.");
            }

            else {
                final ViewParameters viewParameters = initializeDisplayParameters();
                progressDialog.dismiss();

                MarkersManager.getInstance().redrawMarkers(googleMap, locatedDevices, viewParameters);
                if (viewParameters.centrerVueSuiteRafraichissement) {
                    // centrer la vue sur les objets
                    LatLng pointMedian = new LatLng((viewParameters.minLat + viewParameters.maxLat) / 2,
                            (viewParameters.minLng + viewParameters.maxLng) / 2);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(pointMedian));

                    // TODO review
                    // CameraUpdateFactory.newLatLngBounds(new LatLngBounds(arg0, arg1), arg1);
                    // zoomToSpan(Math.round((viewParameters.maxLat - viewParameters.minLat) * RATIO_ZOOM_TO_SPAN),
                    // Math.round((viewParameters.maxLng - viewParameters.minLng) * RATIO_ZOOM_TO_SPAN));
                }

                Toast toast = Toast.makeText(getApplicationContext(), MarkersManager.getInstance().shortTextStats(), Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

}
