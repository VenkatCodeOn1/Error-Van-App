package org.errorvan;

import java.time.Instant;
import java.util.*;

public class TripBuilder {

    public Map<String, Object> createTrip(Map<String, String> r,
                                          TripConfig config,
                                          Map<String, LocationInfoHelper> ccMapping,
                                          Instant now,
                                          String nowUtc) {

        Map<String, Object> t = new LinkedHashMap<>();

        EventPojo event = new EventPojo(
                nowUtc,
                ErrorVanApp.buildEventId(r, config, ccMapping),
                "com.tesco.transport.vanscheduling.api.Trip.tripFinalised.1.0.0",
                UUID.randomUUID().toString() + "-999"
        );

        t.put("event", event);

        Map<String, Object> vt = new LinkedHashMap<>();
        vt.put("tripId", ErrorVanApp.buildTripId(r, config));
        vt.put("providerTripId", config.getProviderTripId());
        vt.put("friendlyTripId", ErrorVanApp.buildFriendlyId(r, config, ccMapping));
        vt.put("departureTime", r.get("ORDER_SLOT_START_TIME"));
        vt.put("returnTime", r.get("ORDER_SLOT_END_TIME"));
        vt.put("tripDistance", Integer.parseInt(r.get("ORDER_WEIGHT").split("\\.")[0]));

        if ("CC".equalsIgnoreCase(config.tripType)) {
            String uuid = r.get("COLLECTION_LOCATION_UUID");
            LocationInfoHelper info = ccMapping.get(uuid);
            vt.put("vehicleType", info != null ? info.collectionType : config.getVehicleType());
        } else {
            vt.put("vehicleType", config.getVehicleType());
        }

        vt.put("stops", new ArrayList<Map<String, Object>>());
        vt.put("tripStatus", "FINAL");
        vt.put("countryCode", config.countryCode);
        vt.put("branchNumber", r.get("MAIN_STORE_ID"));
        vt.put("tescoLocationUUID", r.get("MAIN_STORE_LOCATION_UUID"));
        vt.put("dateTime", now.toString().substring(0, 19) + "Z");
        vt.put("totalNumberOfTrips", "1");
        vt.put("tripNumber", "1");
        vt.put("scheduleId", ErrorVanApp.buildScheduleId(r, config, ccMapping));
        vt.put("waveId", config.waveId);
        vt.put("fromHubDepartureTime", r.get("ORDER_SLOT_START_TIME"));
        vt.put("plannedTripWeightInKg", 0.0);
        vt.put("fulfilmentMethod", config.fulfilmentMethod);
        vt.put("fulfilmentProposition", config.fulfilmentProposition);

        t.put("vehicleTrip", vt);

        return t;
    }
}
