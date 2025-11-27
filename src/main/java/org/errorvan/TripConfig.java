package org.errorvan;

//String waveId, String vehicleType, String tripType, String friendlyTripType, String countryCode, String fm, String fp, String providerTripId
public class TripConfig {
    String waveId;
    String vehicleType;
    String tripType;
    String friendlyTripType;
    String countryCode;
    String fm;
    String fp;
    String providerTripId;
    String fulfilmentMethod;
    String fulfilmentProposition;
    //String locationUUID;
    public TripConfig(String waveId, String vehicleType, String tripType, String friendlyTripType, String countryCode, String fm, String fp, String providerTripId,String fulfilmentMethod,String fulfilmentProposition ) {
        this.waveId = waveId;
        this.vehicleType = vehicleType;
        this.tripType = tripType;
        this.friendlyTripType = friendlyTripType;
        this.countryCode = countryCode;
        this.fm = fm;
        this.fp = fp;
        this.providerTripId = providerTripId;
        this.fulfilmentMethod=fulfilmentMethod;
        this.fulfilmentProposition=fulfilmentProposition;
        //this.locationUUID = locationUUID;
    }
    public String getWaveId() {
        return waveId;
    }

    public void setWaveId(String waveId) {
        this.waveId = waveId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public String getFriendlyTripType() {
        return friendlyTripType;
    }

    public void setFriendlyTripType(String friendlyTripType) {
        this.friendlyTripType = friendlyTripType;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getFm() {
        return fm;
    }

    public void setFm(String fm) {
        this.fm = fm;
    }

    public String getFp() {
        return fp;
    }

    public void setFp(String fp) {
        this.fp = fp;
    }

    public String getProviderTripId() {
        return providerTripId;
    }

    public void setProviderTripId(String providerTripId) {
        this.providerTripId = providerTripId;
    }

/*    static final Map<String, TripConfig> CONFIGS = Map.of(
            "GB_HD", new TripConfig("1", "TESCO_PREMIUM_VAN", "HD", "HD", "GB", "HOME_DELIVERY", "GHS", null),
            "GB_CC", new TripConfig("3", "COLLECT_FROM_LOCATION", "CC", "PA", "GB", "CLICK_AND_COLLECT", "GHS", null),
            "SK_HD", new TripConfig("3", "THIRD_PARTY", "HD", "HD", "SK", "HOME_DELIVERY", "GHS", "10120-1954120")
    );*/
}
