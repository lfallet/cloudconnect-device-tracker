package name.fallet.cloudconnect.model;

import java.util.Date;

import com.google.android.gms.maps.model.Marker;

/**
 * Representation model of a device
 */
public class DeviceRepresentation {

    private final LocatedDevice locatedDevice;

    // state (has one only)
    private boolean inactive, activeToday, recentlyActive, highlighted;

    // depends on the visualisation parameters
    private boolean visible;

    private Marker marker;

    /**
     * 
     * @param locatedDevice
     * @param viewParameters
     */
    public DeviceRepresentation(LocatedDevice locatedDevice, ViewParameters viewParameters) {
        super();
        this.locatedDevice = locatedDevice;

        this.inactive = false;
        this.activeToday = false;
        this.recentlyActive = false;
        this.highlighted = false;
        this.visible = true;

        final Date lastSeenDate = locatedDevice.getLastSeenOn();
        // TODO conserver les timestamp UTC, formater les affichages avec des calendar
        // Calendar gregCal = GregorianCalendar.getInstance(TimeZone.getDefault());
        // gregCal.setTimeInMillis(locatedDevice.getDateInformations().getTime());
        // final Date dateInfoDevice = new Date(locatedDevice.getDateInformations().getTime());
        // Log.d(TAG, locatedDevice.getId() + ": " + locatedDevice.getDateInformations());

        if (belongsToHighlightedDevices(viewParameters)) {
            highlighted = true;
        } else if (lastSeenDate.after(viewParameters.getBorderForRecentDate())) {
            recentlyActive = true;
        } else if (lastSeenDate.after(viewParameters.getLastMidnightDate())) {
            activeToday = true;
        } else {
            inactive = true;
            if (!viewParameters.displayInactiveDevices) {
                visible = false;
            }
        }
    }

    /** @return true if unitid set in preferences */
    private boolean belongsToHighlightedDevices(final ViewParameters viewParameters) {
        return viewParameters.highlightedUnitIds.contains(locatedDevice.getId());
    }

    public LocatedDevice getLocatedDevice() {
        return locatedDevice;
    }

    public boolean isInactive() {
        return inactive;
    }

    public boolean isActiveToday() {
        return activeToday;
    }

    public boolean isRecentlyActive() {
        return recentlyActive;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public boolean isVisible() {
        return visible;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

}
