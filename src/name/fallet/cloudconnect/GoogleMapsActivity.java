package name.fallet.cloudconnect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import name.fallet.cloudconnect.model.ApiException;
import name.fallet.cloudconnect.model.ConnectionParameters;
import name.fallet.cloudconnect.model.LocatedDevice;
import name.fallet.cloudconnect.model.ViewParameters;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.welaika.android.errbit.ErrbitNotifier;

/**
 * 
 * Pour changer l'orientation de l'écran : Ctrl + Windows + F11
 * 
 * @author lfallet
 */
public class GoogleMapsActivity extends MapActivity implements OnDoubleTapListener {

	private static final String TAG = GoogleMapsActivity.class.getSimpleName();

	private MapView mapView;

	private PoiItemizedOverlay poiOverlay;

	private MyLocationOverlay myLocationOverlay;

	/** Overlay des devices */
	private PoiItemizedOverlay notActiveDevicesOverlay, todayActiveDevicesOverlay, recentlyActiveDevicesOverlay;

	private final MobileDevicesApiHelper mdApiHelper = new MobileDevicesApiHelper();

	private int nbDevicesInactifs;

	// approximativement le centre de Paris
	private static final GeoPoint initGeoPoint = new GeoPoint(48863850, 2338170);
	private static final int ZOOM_PAR_DEFAUT = 12;
	private static final float RATIO_ZOOM_TO_SPAN = 1.1f;
	private static final String ZOOM_LEVEL_BUNDLE_KEY = "zlbk";
	private static final String MAP_CENTER_LAT_BUNDLE_KEY = "mcLatbk";
	private static final String MAP_CENTER_LNG_BUNDLE_KEY = "mcLngbk";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// initialization of errbit bug report
		if (getErrbitParameters()) {
			ErrbitNotifier.register(this, "errbit.clubchauffeur.com", "a6ae624b1a7a2570a25c01f8b12e5f40", "test", false);
		}

		mapView = (MapView) findViewById(R.id.mapview);
		final List<Overlay> mapOverlays = mapView.getOverlays();
		if (mapOverlays.isEmpty()) {
			// TODO : rendre la position de l'utilisateur optionnelle
			// position de l'utilisateur
			myLocationOverlay = new MyLocationOverlay(this, mapView);
			mapOverlays.add(myLocationOverlay);

			// couche du dessous pour les voitures inactives
			final Drawable notActiveDeviceDrawable = this.getResources().getDrawable(R.drawable.executive_car_48x48_ultralight);
			notActiveDevicesOverlay = new PoiItemizedOverlay(notActiveDeviceDrawable, this);
			mapOverlays.add(notActiveDevicesOverlay);

			// couche pour les voitures actives du jour
			final Drawable todayActiveDeviceDrawable = this.getResources().getDrawable(R.drawable.executive_car_48x48_lighter);
			todayActiveDevicesOverlay = new PoiItemizedOverlay(todayActiveDeviceDrawable, this);
			mapOverlays.add(todayActiveDevicesOverlay);

			// couche du dessus pour les voitures actives récemment (durée
			// paramétrable)
			final Drawable recentlyActiveDeviceDrawable = this.getResources().getDrawable(R.drawable.executive_car_48x48);
			recentlyActiveDevicesOverlay = new PoiItemizedOverlay(recentlyActiveDeviceDrawable, this);
			mapOverlays.add(recentlyActiveDevicesOverlay);
		} else {
			Log.d(TAG, mapOverlays.size() + " overlays déjà présents");
		}

		// cadrage et zoom
		mapView.setBuiltInZoomControls(true);

		// FIXME : peut être à supprimer car ne semble pas aider au double-clic
		mapView.setClickable(true); // pour réagir aux clics

		// vérification si le create a été appelé après une reconfig d'écran
		final Collection<LocatedDevice> data = (Collection<LocatedDevice>) getLastNonConfigurationInstance();
		if (data == null) {
			Log.d(TAG, "Démarrage from scratch, pas de données");
			mapView.getController().setCenter(initGeoPoint);
			mapView.getController().setZoom(ZOOM_PAR_DEFAUT);
		} else {
			Log.d(TAG, "Redessin des données mémorisées");
			// on restore
			Collection<LocatedDevice> locatedDevices = data;
			// relancer le dessin de ces devices
			final ViewParameters viewParameters = initializeDisplayParameters();
			displayDevicesOnMap(locatedDevices, viewParameters);
		}

		final Intent intent = getIntent();
		// if the activity is created from search
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			Log.i(TAG, "on arrive par la recherche");
			Log.d(TAG, "données mémorisées : " + data);
			String query = intent.getStringExtra(SearchManager.QUERY);
			searchDeviceByUnitId(query);
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
			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), getResources().getText(R.string.emptyQuery),
					Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		Log.d(TAG, "Chaîne recherchée : " + query);
		Collection<LocatedDevice> locatedDevices = ((CloudConnectApplication) this.getApplication()).getLocatedDevices();
		if (locatedDevices.isEmpty()) {
			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), getResources().getText(R.string.noDeviceKnown),
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
					break;
				}
			}
			if (null == deviceMatchingSearch) { // no result
				Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), getResources()
						.getText(R.string.noDeviceFound), Toast.LENGTH_SHORT);
				toast.show();
			} else { // centrer sur le device trouvé
				mapView.getController().animateTo(new GeoPoint(deviceMatchingSearch.getLat(), deviceMatchingSearch.getLng()));
				mapView.getController().zoomIn();
				mapView.getController().zoomIn();
			}
		}
	}

	/**
	 * To retain an object during a runtime configuration change
	 * 
	 * - ne pas oublier d'appeler getLastNonConfigurationInstance() to recover your object.<br>
	 * - ne jamais conserver des View, Drawable, etc sinon un window leak va survenir
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		final Collection<LocatedDevice> data = ((CloudConnectApplication) this.getApplication()).getLocatedDevices();
		return data;
	}

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

		final MapView mapView = (MapView) findViewById(R.id.mapview);
		outState.putSerializable(ZOOM_LEVEL_BUNDLE_KEY, mapView.getZoomLevel());
		outState.putSerializable(MAP_CENTER_LAT_BUNDLE_KEY, mapView.getMapCenter().getLatitudeE6());
		outState.putSerializable(MAP_CENTER_LNG_BUNDLE_KEY, mapView.getMapCenter().getLongitudeE6());

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
			GeoPoint ancienCentre = new GeoPoint(lat, lng);
			mapView.getController().animateTo(ancienCentre);
		}

		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
	}

	@Override
	protected void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
	}

	private void ajouterPOIsurLaMap(final List<Overlay> mapOverlays) {
		Drawable drawablePoi = this.getResources().getDrawable(R.drawable.androidmarker);
		poiOverlay = new PoiItemizedOverlay(drawablePoi, this);
		mapOverlays.add(poiOverlay);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
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
			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(),
					getResources().getText(R.string.connectionParamsNotSet), Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		// is network fine ?
		if (!isConnectedToNetwork(GoogleMapsActivity.this.getApplicationContext())) {
			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(),
					getResources().getText(R.string.networkNotConnected), Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		// récupération asynchrone des données
		new DeviceDataFetcherTask().execute(connectionParameters);
	}

	/**
	 * Nettoie les overlay et replace les éléments dessus.
	 * 
	 * @param locatedDevices
	 * @param viewParameters
	 */
	private void displayDevicesOnMap(final Collection<LocatedDevice> locatedDevices, final ViewParameters viewParameters) {

		// pour le moment on nettoie tout et on recrée (plus tard, essayer de
		// les déplacer)
		notActiveDevicesOverlay.clear();
		todayActiveDevicesOverlay.clear();
		recentlyActiveDevicesOverlay.clear();
		nbDevicesInactifs = 0;

		final long aujourdhuiMs = System.currentTimeMillis();
		// la notion de récent est un paramètre de visualisation
		final long recentEnMinutesAuparavantMs = aujourdhuiMs - viewParameters.relativeTimeRecentDevicesInMinutes * 60 * 1000L;
		final long aujourdhuiMinuitMs = aujourdhuiMs - (aujourdhuiMs % (24 * 60 * 60 * 1000L));
		final Date dateAujourdhuiMinuit = new Date(aujourdhuiMinuitMs);
		final Date dateRecentEnMinutesAuparavant = new Date(recentEnMinutesAuparavantMs);

		Log.d(TAG, "now's date : " + new Date());
		Log.d(TAG, "2 min ago : " + dateRecentEnMinutesAuparavant);
		Log.d(TAG, "beginning of the day : " + dateAujourdhuiMinuit);

		for (LocatedDevice locatedDevice : locatedDevices) {
			if (locatedDevice.isLocalise()) {
				GeoPoint point = new GeoPoint(locatedDevice.getLat(), locatedDevice.getLng());
				StringBuffer buffer = new StringBuffer();
				buffer.append(getResources().getText(R.string.unitid)).append(locatedDevice.getId());
				buffer.append("\n").append(getResources().getText(R.string.modid)).append(locatedDevice.getModid());
				buffer.append("\n").append(getResources().getText(R.string.validity)).append(locatedDevice.getDateInformations());
				OverlayItem overlayItem = new OverlayItem(point, getResources().getText(R.string.device) + " " + locatedDevice.getId(),
						buffer.toString());

				// créer un nouvel objet car cette méthode peut être appelée plusieurs fois
				// TODO à reprendre : conserver les timestamp UTC, formater les affichages avec des calendar
				// Calendar gregCal = GregorianCalendar.getInstance(TimeZone.getDefault());
				// gregCal.setTimeInMillis(locatedDevice.getDateInformations().getTime());
				// final Date dateInfoDevice = new Date(locatedDevice.getDateInformations().getTime());

				Log.d(TAG, locatedDevice.getId() + ": " + locatedDevice.getDateInformations());

				// selon la fraîcheur de l'info
				boolean deviceDisplayed = false;
				final Date dateInfoDevice = locatedDevice.getDateInformations();
				if (dateInfoDevice.after(dateRecentEnMinutesAuparavant)) {
					recentlyActiveDevicesOverlay.addOverlay(overlayItem);
					deviceDisplayed = true;
				} else if (dateInfoDevice.after(dateAujourdhuiMinuit)) {
					todayActiveDevicesOverlay.addOverlay(overlayItem);
					deviceDisplayed = true;
				} else {
					if (viewParameters.displayInactiveDevices) {
						notActiveDevicesOverlay.addOverlay(overlayItem);
						deviceDisplayed = true;
					}
					nbDevicesInactifs++;
				}

				if (deviceDisplayed)
					viewParameters.extendsBoundariesIfNecessary(locatedDevice.getLat(), locatedDevice.getLng());
			}
		}

		// rafraîchir la carte
		mapView.postInvalidate();
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
		return displayParameters;
	}

	/**
	 * Boîte de dialogue de rapport d'erreur
	 */
	private void creerDialogueException(Exception e) {
		AlertDialog.Builder builder = new AlertDialog.Builder(GoogleMapsActivity.this);
		// FIXME : en cas de non connectivité aux réseaux mobiles, l'erreur
		// arrive ici et le builder plante
		builder.setTitle(e.getClass().getSimpleName());
		builder.setMessage(e.getMessage() + "\n" + e.getCause());
		builder.setNeutralButton(getResources().getText(R.string.close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// GoogleMapsActivity.this.finish();
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
			afficherEcranConfiguration();
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
		Log.d(TAG, "onSearchRequested() appelé");
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
	 * Un Intent est nécessaire pour faire appel à une autre Activity
	 */
	private void afficherEcranConfiguration() {
		Intent preferencesIntent = new Intent(this, MobileDevicesPreferenceActivity.class);
		startActivity(preferencesIntent);
	}

	/** */
	private void createCreditsDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(GoogleMapsActivity.this);
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

	public boolean onDoubleTap(MotionEvent e) {
		Log.d(TAG, "Appel de onDoubleTap() sur l'activité");
		// FIXME le double tap ne fonctionne pas
		Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), "onDoubleTap", Toast.LENGTH_SHORT).show();
		int x = (int) e.getX(), y = (int) e.getY();
		Projection p = mapView.getProjection();
		mapView.getController().animateTo(p.fromPixels(x, y)); // zoom in to a
																// point you
																// tapped
		mapView.getController().zoomIn();
		return true;
	}

	public boolean onDoubleTapEvent(MotionEvent e) {
		// FIXME le double tap ne fonctionne pas
		Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), "onDoubleTapEvent", Toast.LENGTH_SHORT).show();
		return true;
	}

	public boolean onSingleTapConfirmed(MotionEvent e) {
		return true;
	}

	/**
	 * Get information from g8teway api.
	 * 
	 * @author lfallet
	 */
	private class DeviceDataFetcherTask extends AsyncTask<ConnectionParameters, Void, Collection<LocatedDevice>> {

		private ProgressDialog progressDialog;

		private final MobileDevicesApiHelper mdApiHelper = new MobileDevicesApiHelper();

		/**
		 * Lancer le dialogue d'avancement (attente)
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(GoogleMapsActivity.this, "", getResources().getText(R.string.progress_dialog), true);
		}

		@Override
		protected Collection<LocatedDevice> doInBackground(ConnectionParameters... params) {
			try {
				return mdApiHelper.recupererPositionVehicules(params[0], false);
			} catch (ApiException e) {
				e.printStackTrace();
				// FIXME : pas de création de dialogue dans une async task
				// creerDialogueException(e);
			}
			return new ArrayList<LocatedDevice>();
		}

		/**
		 * Masquer le dialogue d'avancement. Afficher un toast.
		 */
		@Override
		protected void onPostExecute(Collection<LocatedDevice> locatedDevices) {
			super.onPostExecute(locatedDevices);
			((CloudConnectApplication) getApplication()).getLocatedDevices().clear();
			((CloudConnectApplication) getApplication()).getLocatedDevices().addAll(locatedDevices);

			final ViewParameters viewParameters = initializeDisplayParameters();
			displayDevicesOnMap(locatedDevices, viewParameters);
			if (viewParameters.centrerVueSuiteRafraichissement) {
				// centrer la vue sur les objets
				final MapView mapView = (MapView) findViewById(R.id.mapview);
				GeoPoint pointMedian = new GeoPoint((viewParameters.minLat + viewParameters.maxLat) / 2,
						(viewParameters.minLng + viewParameters.maxLng) / 2);
				mapView.getController().animateTo(pointMedian);
				mapView.getController().zoomToSpan(Math.round((viewParameters.maxLat - viewParameters.minLat) * RATIO_ZOOM_TO_SPAN),
						Math.round((viewParameters.maxLng - viewParameters.minLng) * RATIO_ZOOM_TO_SPAN));
			}

			progressDialog.dismiss();

			// attention un toast doit être lancé dans le Thread UI, pas un
			// autre ; piste d'amélioration : utiliser un handler
			final int nbDevicesActifsLocalises = recentlyActiveDevicesOverlay.size();
			final int nbDevicesActifsAujourdhuiLocalises = nbDevicesActifsLocalises + todayActiveDevicesOverlay.size();
			final int nbDevicesTotalLocalises = nbDevicesActifsAujourdhuiLocalises + notActiveDevicesOverlay.size();
			final StringBuilder texteToast = new StringBuilder(40);
			texteToast.append(getResources().getQuantityString(R.plurals.located_devices, nbDevicesActifsLocalises,
					nbDevicesActifsLocalises));
			texteToast.append("\n");
			if (viewParameters.displayInactiveDevices) {
				texteToast.append(getResources().getString(R.string.today_and_total_located_devices, nbDevicesActifsAujourdhuiLocalises,
						nbDevicesTotalLocalises));
			} else {
				texteToast.append(getResources().getString(R.string.today_and_not_displayed_located_devices,
						nbDevicesActifsAujourdhuiLocalises, nbDevicesInactifs));
			}
			Toast toast = Toast.makeText(getApplicationContext(), texteToast, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

}
