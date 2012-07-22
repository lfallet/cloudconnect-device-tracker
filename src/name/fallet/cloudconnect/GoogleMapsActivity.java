package name.fallet.cloudconnect;

import java.util.Collection;
import java.util.List;

import name.fallet.cloudconnect.model.ApiException;
import name.fallet.cloudconnect.model.ConnectionParameters;
import name.fallet.cloudconnect.model.VehiculeLocalise;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * 
 * Pour changer l'orientation de l'écran : Ctrl + Windows + F11
 * 
 * @author lfallet
 */
public class GoogleMapsActivity extends MapActivity implements OnDoubleTapListener {

	private static final String TAG = GoogleMapsActivity.class.getSimpleName();

	private PoiItemizedOverlay poiOverlay;

	private PoiItemizedOverlay vehiculesOverlay;

	private final MobileDevicesApiHelper mdApiHelper = new MobileDevicesApiHelper();

	private boolean estPremierDemarrage = true;

	// TODO : ajouter cela dans la config
	private boolean centrerVueSuiteRafraichissement = false;

	private static final float RATIO_ZOOM_TO_SPAN = 1.1f;

	// approximativement le centre de Paris
	private static final GeoPoint initGeoPoint = new GeoPoint(48863850, 2338170);

	private static final int ZOOM_PAR_DEFAUT = 12;

	private static final String VEHICULES_OVERLAY_BUNDLE_KEY = "vobk";
	private static final String ZOOM_LEVEL_BUNDLE_KEY = "zlbk";
	private static final String MAP_CENTER_LAT_BUNDLE_KEY = "mcLatbk";
	private static final String MAP_CENTER_LNG_BUNDLE_KEY = "mcLngbk";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final MapView mapView = (MapView) findViewById(R.id.mapview);
		final List<Overlay> mapOverlays = mapView.getOverlays();

		// TODO : positionner une icône différente selon la date de validité des données
		Drawable drawable = this.getResources().getDrawable(R.drawable.executive_car_48x48);
		vehiculesOverlay = new PoiItemizedOverlay(drawable, this);
		mapOverlays.add(vehiculesOverlay);

		// TODO : en faire une option
		// ajouterPOIsurLaMap(mapOverlays);

		// cadrage et zoom
		mapView.setBuiltInZoomControls(true);

		if (estPremierDemarrage) {
			Log.d(TAG, "estPremierDemarrage");
			mapView.getController().setCenter(initGeoPoint);
			mapView.getController().setZoom(ZOOM_PAR_DEFAUT);
			estPremierDemarrage = false;
		}
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

		// outState.putSerializable(VEHICULES_OVERLAY_BUNDLE_KEY, vehiculesOverlay);
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
		vehiculesOverlay = (PoiItemizedOverlay) savedInstanceState.get(VEHICULES_OVERLAY_BUNDLE_KEY);

		final MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.getOverlays().add(vehiculesOverlay);

		Integer lat = (Integer) savedInstanceState.get(MAP_CENTER_LAT_BUNDLE_KEY), lng = (Integer) savedInstanceState
				.get(MAP_CENTER_LNG_BUNDLE_KEY);
		if (lat != null && lng != null) {
			GeoPoint ancienCentre = new GeoPoint(lat, lng);
			mapView.getController().animateTo(ancienCentre);
		}

		super.onRestoreInstanceState(savedInstanceState);
	}

	@SuppressWarnings("unused")
	private void ajouterPOIsurLaMap(final List<Overlay> mapOverlays) {
		Drawable drawablePoi = this.getResources().getDrawable(R.drawable.androidmarker);
		poiOverlay = new PoiItemizedOverlay(drawablePoi, this);
		mapOverlays.add(poiOverlay);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Reçoit l'interaction du bouton rafraîchir
	 */
	public void rafraichirOverlay(View v) {

		ConnectionParameters connectionParameters = initialiserParametresConnexion();
		if (connectionParameters.isMissingAtLeastOneMandatoryValue()) {
			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), "Please fill connection parameters first",
					Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		// TODO : essayer de faire fonctionner le dialog
		final ProgressDialog dialog = ProgressDialog.show(this, "", getResources().getText(R.string.progress_dialog), true);

		try {
			Collection<VehiculeLocalise> vehiculesLocalises = mdApiHelper.recupererPositionVehicules(connectionParameters, false);

			Toast toast = Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), vehiculesLocalises.size()
					+ " véhicules localisés", Toast.LENGTH_SHORT);
			toast.show();

			// pour le moment on nettoie tout et on recrée (plus tard, essayer
			// de les déplacer)
			vehiculesOverlay.clear();

			int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE, minLng = Integer.MAX_VALUE, maxLng = Integer.MIN_VALUE;

			for (VehiculeLocalise vehiculeLocalise : vehiculesLocalises) {

				// Log.d(TAG, vehiculeLocalise.getLat() + "," +
				// vehiculeLocalise.getLng() + " / " +
				// vehiculeLocalise.isLocalise());

				if (vehiculeLocalise.isLocalise()) {
					GeoPoint point = new GeoPoint(vehiculeLocalise.getLat(), vehiculeLocalise.getLng());
					StringBuffer buffer = new StringBuffer();
					buffer.append("id : ").append(vehiculeLocalise.getId());
					buffer.append("\nmodid : ").append(vehiculeLocalise.getModid());
					buffer.append("\nStatut : ").append(vehiculeLocalise.getStatut());
					buffer.append("\nValidité : ").append(vehiculeLocalise.getDateInformations());
					OverlayItem overlayitem = new OverlayItem(point, "Véhicule " + vehiculeLocalise.getId(), buffer.toString());
					vehiculesOverlay.addOverlay(overlayitem);

					minLat = Math.min(minLat, vehiculeLocalise.getLat());
					maxLat = Math.max(maxLat, vehiculeLocalise.getLat());
					minLng = Math.min(minLng, vehiculeLocalise.getLng());
					maxLng = Math.max(maxLng, vehiculeLocalise.getLng());
				}
				final MapView mapView = (MapView) findViewById(R.id.mapview);

				// FIXME : vérifier si c'est utile
				mapView.refreshDrawableState();
			}

			dialog.dismiss();

			if (centrerVueSuiteRafraichissement) {
				// centrer la vue sur les objets
				final MapView mapView = (MapView) findViewById(R.id.mapview);
				GeoPoint pointMedian = new GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2);
				mapView.getController().animateTo(pointMedian);
				mapView.getController().zoomToSpan(Math.round((maxLat - minLat) * RATIO_ZOOM_TO_SPAN),
						Math.round((maxLng - minLng) * RATIO_ZOOM_TO_SPAN));
			}

		} catch (ApiException e) {
			e.printStackTrace();
			creerDialogueException(e);
		}
	}

	/** */
	private ConnectionParameters initialiserParametresConnexion() {
		ConnectionParameters connectionParameters = new ConnectionParameters();
		// récupérer les préférences
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		connectionParameters.setUser(sharedPrefs.getString("user", null));
		connectionParameters.setPassword(sharedPrefs.getString("password", null));
		connectionParameters.setClient(sharedPrefs.getString("client", null));
		connectionParameters.setUrl(sharedPrefs.getString("url", null));
		return connectionParameters;
	}

	/**
	 * Boîte de dialogue de rapport d'erreur
	 */
	private void creerDialogueException(Exception e) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(e.getClass().getSimpleName());
		builder.setMessage(e.getMessage() + "\n" + e.getCause());
		builder.setNeutralButton(getResources().getText(R.string.fermer), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				GoogleMapsActivity.this.finish();
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
			rafraichirOverlay(findViewById(R.id.mapview));
			return true;
		case R.id.menuSettings:
			afficherEcranConfiguration();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Un Intent est nécessaire pour faire appel à une autre Activity
	 */
	private void afficherEcranConfiguration() {
		Intent preferencesIntent = new Intent(this, MobileDevicesPreferenceActivity.class);
		startActivity(preferencesIntent);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Log.d(TAG, "Appel de onDoubleTap() sur l'activité");
		// TODO Auto-generated method stub
		Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), "onDoubleTap", Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		// TODO Auto-generated method stub
		Toast.makeText(GoogleMapsActivity.this.getApplicationContext(), "onDoubleTapEvent", Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

}
