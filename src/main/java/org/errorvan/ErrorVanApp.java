package org.errorvan;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

//Main Application Class

public class ErrorVanApp {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Country Code (GB, IE, CZ, HU, SK): ");
        String countryCode = scanner.nextLine().toUpperCase();

        System.out.print("Enter Proposition (HD or CC): ");
        String proposition = scanner.nextLine().toUpperCase();

        TripConfig config = ConfigProvider.getConfig(countryCode, proposition);

        if (config == null) {
            System.out.println("Invalid configuration for given country and proposition.");                 //
            return;
        }

        String mappingCsvPath = "config/CorrectConfig.csv"; // Config File path for comparing the CC Location id
        Map<String, LocationInfoHelper> ccMapping;

        try (InputStream in = ErrorVanApp.class.getClassLoader().getResourceAsStream(mappingCsvPath)) {
            if (in == null) {
                throw new IllegalArgumentException("Mapping CSV not found in resources: " + mappingCsvPath);
            }

            ccMapping = loadLocationMapping(in);  // overload method to accept InputStream
        }


        File csv = new File("C:\\Users\\vsanthankrish\\Downloads\\cz-hd2.csv");       // Input File Path
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();
        var reader = csvMapper.readerFor(new TypeReference<Map<String,String>>() {})
                .with(schema)
                .readValues(csv);

        String projectDir = System.getProperty("user.dir");
        String outputDirName = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        Path outputPath = Paths.get(projectDir, "output", outputDirName);

        Files.createDirectories(outputPath);

        System.out.println("Writing JSON files to folder: " + outputPath.toAbsolutePath());

        Map<String, Map<String, Object>> trips = new LinkedHashMap<>();
        Instant now = Instant.now();
        String nowUtc = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 19) + "Z";

        while (reader.hasNext()) {
            Map<String, String> r = (Map<String, String>) reader.next();
            String key = r.get("F_TRIP_ID");


            trips.computeIfAbsent(key, k -> {
                Map<String, Object> t = new LinkedHashMap<>();
                EventPojo event = new EventPojo(
                        /*now.toString().substring(0, 19) + "Z",*/
                        nowUtc,
                        buildEventId(r, config,ccMapping),
                        "com.tesco.transport.vanscheduling.api.Trip.tripFinalised.1.0.0",
                        UUID.randomUUID().toString()+"-999"
                );
                t.put("event", event);

                Map<String, Object> vt = new LinkedHashMap<>();
                vt.put("tripId", buildTripId(r,config));
                vt.put("providerTripId",config.getProviderTripId());
                vt.put("friendlyTripId", buildFriendlyId(r, config,ccMapping));
                vt.put("departureTime", r.get("ORDER_SLOT_START_TIME"));
                vt.put("returnTime", r.get("ORDER_SLOT_END_TIME"));
                vt.put("tripDistance", Integer.parseInt(r.get("ORDER_WEIGHT").split("\\.")[0]));
                //vt.put("vehicleType", config.getVehicleType());
                if ("CC".equalsIgnoreCase(config.tripType)) {
                    String locationUuid = r.get("COLLECTION_LOCATION_UUID");
                    LocationInfoHelper info = ccMapping.get(locationUuid);

                    if (info != null) {
                        vt.put("vehicleType", info.collectionType);  // from mapping file
                    } else {
                        vt.put("vehicleType", config.getVehicleType()); // fallback
                    }
                } else {
                    vt.put("vehicleType", config.getVehicleType());
                }

                vt.put("stops", new ArrayList<Map<String,Object>>());
                vt.put("tripStatus", "FINAL");
                vt.put("countryCode", config.countryCode);
                vt.put("branchNumber", r.get("MAIN_STORE_ID"));
                //vt.put("tescoLocationUUID", r.get("LOCATION_UUID"));
                vt.put("tescoLocationUUID", r.get("MAIN_STORE_LOCATION_UUID"));
                vt.put("dateTime", now.toString().substring(0, 19) + "Z");
                vt.put("totalNumberOfTrips", "1");
                vt.put("tripNumber", "1");
                vt.put("scheduleId", buildScheduleId(r,config,ccMapping));
                vt.put("waveId", config.waveId);
                vt.put("fromHubDepartureTime", r.get("ORDER_SLOT_START_TIME"));
                //weight
                vt.put("plannedTripWeightInKg", 0.0);
                //vt.put("plannedTripWeightInKg", Double.parseDouble(r.get("ORDER_WEIGHT")));
                vt.put("fulfilmentMethod",config.fulfilmentMethod);
                vt.put("fulfilmentProposition",config.fulfilmentProposition);

                t.put("vehicleTrip", vt);
                return t;
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> tripMap = trips.get(key);
            @SuppressWarnings("unchecked")
            Map<String, Object> vehicleTrip = (Map<String, Object>) tripMap.get("vehicleTrip");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stops = (List<Map<String, Object>>) vehicleTrip.get("stops");
            Map<String, Object> stop = buildStop(r);
            stop.put("stopSequenceId", String.valueOf(stops.size() + 1)); // Make sequence incremental
            stops.add(stop);
            //stops.add(buildStop(r));
            double currentWeight = (double) vehicleTrip.get("plannedTripWeightInKg");
            currentWeight += Double.parseDouble(r.get("ORDER_WEIGHT"));
            vehicleTrip.put("plannedTripWeightInKg", currentWeight);
        }

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        for (var t : trips.values()) {
            Map<String, Object> vehicleTrip = (Map<String, Object>) t.get("vehicleTrip");
            String tripId = (String) vehicleTrip.get("tripId");

            File outFile = outputPath.resolve(tripId + ".json").toFile();
            om.writeValue(outFile, t);
            System.out.println("Generated file: " + outFile.getPath());
        }
    }
    private static String buildFriendlyHour(Map<String, String> r) {
        Instant departureInstant = Instant.parse(r.get("ORDER_SLOT_START_TIME"));
        int hour = departureInstant.atZone(java.time.ZoneOffset.UTC).getHour();
        return String.format("%02d", hour);
    }

    private static Map<String, LocationInfoHelper> loadLocationMapping(InputStream in) throws Exception {
        Map<String, LocationInfoHelper> mapping = new HashMap<>();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();

        try (MappingIterator<Map<String, String>> reader =
                     csvMapper.readerFor(new TypeReference<Map<String, String>>() {})
                             .with(schema)
                             .readValues(in)) {

            while (reader.hasNext()) {
                Map<String, String> row = reader.next();
                String locationUuid = row.get("COLLECTION_LOCATION_UUID");
                String friendlyIdentifier = row.get("FRIENDLY_IDENTIFIER");
                String collectionType = row.get("COLLECTION_TYPE");

                if (locationUuid != null && !locationUuid.isBlank()) {
                    mapping.put(locationUuid.trim(),
                            new LocationInfoHelper(
                                    friendlyIdentifier != null ? friendlyIdentifier.trim() : "XX",
                                    collectionType != null ? collectionType.trim() : "UNKNOWN"));
                }
            }
        }
        return mapping;
    }




    private static String buildEventId(Map<String,String> r,TripConfig config,Map<String, LocationInfoHelper> ccMapping) {
        return "com.tesco.transport.vanscheduling.api.Trip." +
                buildTripId(r,config) + "_" + buildFriendlyId(r,config,ccMapping);
    }

    private static String buildTripId(Map<String,String> r,TripConfig config) {
        String storeid = r.get("MAIN_STORE_ID"),
                collectionid = r.get("COLLECTION_LOCATION_ID"),
                date = r.get("ORDER_SLOT_START_TIME").substring(2,10).replace("-", ""),
                fid = r.get("F_TRIP_ID"),
                hh = r.get("ORDER_SLOT_START_TIME").substring(11,13),
                type = config.tripType;
        // For CC, duplicate store ID at the beginning
        if ("CC".equalsIgnoreCase(type)) {
            return String.join("_", storeid, collectionid, date, fid, "1", hh, config.countryCode, type);
        } else {
            return String.join("_", storeid, date, fid, "1", hh, config.countryCode, type);
        }
    }

    private static String buildFriendlyId(Map<String,String> r, TripConfig config,
                                          Map<String, LocationInfoHelper> ccMapping) {
        Instant departureInstant = Instant.parse(r.get("ORDER_SLOT_START_TIME"));
        int hour = departureInstant.atZone(java.time.ZoneOffset.UTC).getHour();
        String formattedHour = String.format("%02d", hour);

        if ("CC".equalsIgnoreCase(config.tripType)) {
            String locationUuid = r.get("COLLECTION_LOCATION_UUID");
            LocationInfoHelper info = ccMapping.get(locationUuid);

            if (info != null) {
                return r.get("F_TRIP_ID") + info.friendlyIdentifier + formattedHour;
            } else {
                return r.get("F_TRIP_ID") + "XX" + formattedHour; // fallback
            }
        } else {
            return r.get("F_TRIP_ID") + config.getTripType() + formattedHour;
        }
    }


    private static String buildScheduleId(Map<String,String> r,TripConfig config, Map<String, LocationInfoHelper> ccMapping) {
        return "schedule-" +r.get("MAIN_STORE_ID")+  "-"+config.getCountryCode() +  "-" + config.getTripType()+ "-"+
                r.get("ORDER_SLOT_START_TIME") + "-" +
                r.get("ORDER_SLOT_END_TIME") +
                "-FINAL-" + buildFriendlyId(r,config,ccMapping);      /*//return "schedule-" + buildTripId(r,config) + "-" +
        r.get("ORDER_SLOT_START_TIME") + "-" +
                r.get("ORDER_SLOT_END_TIME") +
                "-FINAL-" + buildFriendlyId(r,config);*/
    }

    private static Map<String,Object> buildStop(Map<String,String> r) {
        Map<String,Object> s = new LinkedHashMap<>();
        s.put("customerOrderShortId", r.get("CUSTOMER_ORDER_SHORT_ID"));
        s.put("customerOrderUUID", r.get("CUSTOMER_ORDER_UUID"));
        s.put("fulfilmentOrderShortId", r.get("FO_SHORT_ID"));
        s.put("fulfilmentOrderUUID", r.get("FO_UUID"));
        s.put("departure", r.get("ORDER_SLOT_END_TIME"));
        s.put("arrival", r.get("ORDER_SLOT_START_TIME"));
        s.put("slotEnd", r.get("ORDER_SLOT_END_TIME"));
        s.put("slotStart", r.get("ORDER_SLOT_START_TIME"));
        s.put("latitude", r.get("LATITUDE"));
        s.put("longitude", r.get("LONGITUDE"));
        s.put("shipToPostcode", r.get("SHIP_TO_POSTCODE"));
        s.put("distance", 5);
        s.put("stopSequenceId", "1");
        s.put("idleTime", "0");
        s.put("plannedWeightInKg", Double.parseDouble(r.get("ORDER_WEIGHT")));
        return s;
    }
}
