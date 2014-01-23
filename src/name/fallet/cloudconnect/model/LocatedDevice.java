package name.fallet.cloudconnect.model;

import java.util.Date;

/**
 * Raw output from the g8teway
 */
public class LocatedDevice {

    /** unitid */
    private final int id;

    /** IMEI, or random text if simulator */
    private final long modid;

    /** expressed in degrees */
    private double lat = NOT_INITIALIZED;
    /** expressed in degrees */
    private double lng = NOT_INITIALIZED;

    private String status;
    private Date lastSeenOn;

    private static final double NOT_INITIALIZED = 0;

    public LocatedDevice(int id, long modid, Date lastSeenOn) {
        super();
        this.id = id;
        this.modid = modid;
        this.lastSeenOn = lastSeenOn;
    }

    public double getLat() {
        return lat;
    }

    /**
     * 
     * @param lat
     *            in degrees
     * @param lng
     *            degrees
     */
    public void setLatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLng() {
        return lng;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastSeenOn() {
        return lastSeenOn;
    }

    public int getId() {
        return id;
    }

    public long getModid() {
        return modid;
    }

    public boolean isLocalized() {
        return lng != NOT_INITIALIZED || lat != NOT_INITIALIZED;
    }

}
