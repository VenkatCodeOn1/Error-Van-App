package org.errorvan;

import java.util.Map;

public class ConfigProvider {

    public static TripConfig getConfig(String country, String proposition) {
        return CONFIG_MAP.get(proposition + "-" + country);
    }
//String waveId, String vehicleType, String tripType, String friendlyTripType, String countryCode, String fm, String fp, String providerTripId
    private static final Map<String, TripConfig> CONFIG_MAP = Map.ofEntries(
            //HD - GB
            Map.entry("HD-GB", new TripConfig("3","TESCO_PREMIUM_VAN", "HD", "HD", "GB", "HOME_DELIVERY", "GHS", null,"HOME_DELIVERY","GHS")),
            //CC - GB  COLLECT_FROM_STORE , STORE_COLLECT
            Map.entry("CC-GB", new TripConfig("3","STORE_COLLECT", "CC", "PA", "GB", "CLICK_AND_COLLECT", "GHS", null,"CLICK_AND_COLLECT","GHS" )),
            //HD - CE countries
            Map.entry("HD-SK", new TripConfig("3","THIRD_PARTY", "HD", "HD", "SK", "HOME_DELIVERY", "GHS", "10120-1954120","HOME_DELIVERY","GHS" )),
            Map.entry("HD-IE", new TripConfig("3","TESCO_PREMIUM_VAN", "HD", "HD", "IE", "HOME_DELIVERY", "GHS", null,"HOME_DELIVERY","GHS" )),
            Map.entry("HD-CZ", new TripConfig("3","THIRD_PARTY", "HD", "HD", "CZ", "HOME_DELIVERY", "GHS", "10120-1954122","HOME_DELIVERY","GHS" )),
            //CC - CE countries
            Map.entry("HD-HU", new TripConfig("3","THIRD_PARTY", "HD", "HD", "HU", "HOME_DELIVERY", "GHS", null,"HOME_DELIVERY","GHS" )),
            Map.entry("CC-HU", new TripConfig("3","STORE_COLLECT", "CC", "CC", "HU", "CLICK_AND_COLLECT", "GHS", null,"CLICK_AND_COLLECT","GHS" )),
            Map.entry("CC-CZ", new TripConfig("3","STORE_COLLECT", "CC", "CC", "CZ", "CLICK_AND_COLLECT", "GHS", null,"CLICK_AND_COLLECT","GHS" )),
            Map.entry("CC-IE", new TripConfig("3","STORE_COLLECT", "CC", "CC", "IE", "CLICK_AND_COLLECT", "GHS", null,"CLICK_AND_COLLECT","GHS" )),
            Map.entry("CC-SK", new TripConfig("3","STORE_COLLECT", "CC", "CC", "SK", "CLICK_AND_COLLECT", "GHS", null,"CLICK_AND_COLLECT","GHS" ))


        // Add additional configurations as needed cc-ce:- vehicle type-storecollect , prov- null, fm- click and collect
    );
}
