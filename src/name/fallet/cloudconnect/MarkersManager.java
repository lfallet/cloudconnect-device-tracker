package name.fallet.cloudconnect;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import name.fallet.cloudconnect.model.DeviceRepresentation;
import name.fallet.cloudconnect.model.LocatedDevice;
import name.fallet.cloudconnect.model.ViewParameters;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Ease markers manipulation on the map
 */
public class MarkersManager {

    private final BitmapDescriptor recentlyActiveDeviceBitmap;
    private final BitmapDescriptor activeDeviceBitmap;
    private final BitmapDescriptor inactiveDeviceBitmap;
    private final BitmapDescriptor highlightedDeviceBitmap;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss (dd-MM-yyyy)");

    private final Map<Integer, DeviceRepresentation> devicesRep = new HashMap<Integer, DeviceRepresentation>();

    /** singleton */
    private static MarkersManager instance;

    public static MarkersManager getInstance() {
        if (instance == null) {
            instance = new MarkersManager();
        }
        return instance;
    }

    private MarkersManager() {
        // load bitmaps
        recentlyActiveDeviceBitmap = BitmapDescriptorFactory.fromResource(R.drawable.executive_car_48x48);
        activeDeviceBitmap = BitmapDescriptorFactory.fromResource(R.drawable.executive_car_48x48_lighter);
        inactiveDeviceBitmap = BitmapDescriptorFactory.fromResource(R.drawable.executive_car_48x48_ultralight);
        highlightedDeviceBitmap = BitmapDescriptorFactory.fromResource(R.drawable.executive_car_48x48_red);
    }

    /**
     * 
     * @param googleMap
     * @param devices
     * @param viewParameters
     */
    public void redrawMarkers(final GoogleMap googleMap, final Collection<LocatedDevice> devices, final ViewParameters viewParameters) {
        final Resources r = CloudConnectApplication.getAppContext().getResources();

        // FIXME call that method in initialize ?
        viewParameters.refreshDateBorders();

        for (LocatedDevice locatedDevice : devices) {
            DeviceRepresentation formerDeviceRepresentation = devicesRep.get(locatedDevice.getId());
            DeviceRepresentation deviceRepresentation = new DeviceRepresentation(locatedDevice, viewParameters);

            if (locatedDevice.isLocalized()) {
                // prepare updated content
                final LatLng position = new LatLng(locatedDevice.getLat(), locatedDevice.getLng());
                final StringBuffer snippetBuffer = new StringBuffer();
                // snippetBuffer.append(context.getResources().getText(R.string.modid)).append(locatedDevice.getModid()).append(", ");
                snippetBuffer.append(r.getText(R.string.validity)).append(' ').append(sdf.format(locatedDevice.getLastSeenOn()));
                final String title = r.getText(R.string.device) + " " + locatedDevice.getId();

                if (formerDeviceRepresentation == null) {
                    Log.d("Markers", "DRAWING new marker for unit_id " + deviceRepresentation.getLocatedDevice().getId());
                    Marker marker = createNewMarker(googleMap, deviceRepresentation, position, snippetBuffer.toString(), title, viewParameters);
                    deviceRepresentation.setMarker(marker);
                } else {
                    Marker existingMarkerToAnimate = formerDeviceRepresentation.getMarker();
                    if (existingMarkerToAnimate == null) {
                        if (deviceRepresentation.isVisible()) {
                            Log.d("Markers", "DRAWING new marker for unit_id " + deviceRepresentation.getLocatedDevice().getId());
                            Marker marker = createNewMarker(googleMap, deviceRepresentation, position, snippetBuffer.toString(), title,
                                    viewParameters);
                            deviceRepresentation.setMarker(marker);
                        }
                    } else {
                        deviceRepresentation.setMarker(existingMarkerToAnimate);
                        // animate existing marker
                        existingMarkerToAnimate.setSnippet(snippetBuffer.toString());
                        existingMarkerToAnimate.setIcon(iconFor(deviceRepresentation));
                        existingMarkerToAnimate.setVisible(deviceRepresentation.isVisible());
                        if (deviceRepresentation.isVisible()) {
                            Log.d("Markers", "ANIMATING marker for unit_id " + deviceRepresentation.getLocatedDevice().getId());
                            animateMarker(googleMap, existingMarkerToAnimate, position, false);
                        } else {
                            Log.d("Markers", "REMOVING marker for unit_id " + deviceRepresentation.getLocatedDevice().getId());
                            existingMarkerToAnimate.remove();
                            deviceRepresentation.setMarker(null);
                        }
                    }

                }

                // FIXME
                // if (deviceDisplayed)
                // viewParameters.extendsBoundariesIfNecessary(locatedDevice.getLat(), locatedDevice.getLng());
            }

            devicesRep.put(locatedDevice.getId(), deviceRepresentation);
        }
    }

    /** set all attributes of new marker, add to map */
    private Marker createNewMarker(final GoogleMap googleMap, final DeviceRepresentation deviceRepresentation, final LatLng position,
            final String snippet, final String title, final ViewParameters viewParameters) {
        MarkerOptions mo = new MarkerOptions().position(position).title(title).snippet(snippet);

        mo.icon(iconFor(deviceRepresentation));
        mo.visible(deviceRepresentation.isVisible());

        Marker marker = googleMap.addMarker(mo);
        return marker;
    }

    /**
     * 
     * @param deviceRepresentation
     * @return bitmap to use as an icon
     */
    private BitmapDescriptor iconFor(final DeviceRepresentation deviceRepresentation) {
        if (deviceRepresentation.isHighlighted()) {
            return highlightedDeviceBitmap;
        } else if (deviceRepresentation.isRecentlyActive()) {
            return recentlyActiveDeviceBitmap;
        } else if (deviceRepresentation.isActiveToday()) {
            return activeDeviceBitmap;
        } else {
            return inactiveDeviceBitmap;
        }
    }

    /**
     * 
     * @param marker
     * @param toPosition
     * @param hideMarker
     */
    private void animateMarker(final GoogleMap googleMap, final Marker marker, final LatLng toPosition, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = googleMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                // Log.d("MAPS", "" + t);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    public Collection<LocatedDevice> getLocatedDevices() {
        Collection<LocatedDevice> col = new ArrayList<LocatedDevice>();
        for (DeviceRepresentation devRepresentation : devicesRep.values()) {
            col.add(devRepresentation.getLocatedDevice());
        }
        return col;
    }

    public CharSequence shortTextStats() {
        int nbDevices = devicesRep.size();
        int nbDevicesInactifs = 0, nbDevicesActifs = 0;
        int nbDevicesInvisibles = 0, nbDevicesVisibles = 0;

        // FIXME toast de statistiques
        // final int nbDevicesActifsLocalises = recentlyActiveDevicesOverlay.size();
        // final int nbDevicesActifsAujourdhuiLocalises = nbDevicesActifsLocalises + todayActiveDevicesOverlay.size();
        // final int nbDevicesTotalLocalises = nbDevicesActifsAujourdhuiLocalises + notActiveDevicesOverlay.size();
        // final StringBuilder texteToast = new StringBuilder(40);
        // texteToast.append(getResources().getQuantityString(R.plurals.located_devices, nbDevicesActifsLocalises,
        // nbDevicesActifsLocalises));
        // texteToast.append("\n");
        // if (viewParameters.displayInactiveDevices) {
        // texteToast.append(getResources().getString(R.string.today_and_total_located_devices, nbDevicesActifsAujourdhuiLocalises,
        // nbDevicesTotalLocalises));
        // } else {
        // texteToast.append(getResources().getString(R.string.today_and_not_displayed_located_devices,
        // nbDevicesActifsAujourdhuiLocalises, nbDevicesInactifs));
        // }

        int useless = -1;
        for (DeviceRepresentation device : devicesRep.values()) {
            useless = device.isRecentlyActive() ? ++nbDevicesActifs : ++nbDevicesInactifs;
            useless = device.isVisible() ? ++nbDevicesVisibles : ++nbDevicesInvisibles;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(nbDevices).append(" véhicules");
        sb.append(" (").append(nbDevicesActifs).append(" actifs");
        sb.append(", ").append(nbDevicesVisibles).append(" visibles)");
        return sb.toString();
    }

}
