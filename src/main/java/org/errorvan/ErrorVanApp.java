package org.errorvan;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ErrorVanApp {

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Country Code (GB, IE, CZ, HU, SK): ");
        String countryCode = scanner.nextLine().toUpperCase();

        System.out.print("Enter Proposition (HD or CC): ");
        String proposition = scanner.nextLine().toUpperCase();

        TripConfig config = ConfigProvider.getConfig(countryCode, proposition);
        if (config == null) {
            System.out.println("Invalid configuration for given country and proposition.");
            return;
        }

        // Load CC mapping file
        String mappingCsvPath = "config/CorrectConfig.csv";
        Map<String, LocationInfoHelper> ccMapping;

        try (InputStream in = ErrorVanApp.class.getClassLoader().getResourceAsStream(mappingCsvPath)) {
            if (in == null) {
                throw new IllegalArgumentException("Mapping CSV not found: " + mappingCsvPath);
            }
            ccMapping = loadLocationMapping(in);
        }

        // Input file path
        File csv = new File("C:\\Users\\vsanthankrish\\Downloads\\cz-hd2.csv");

        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();

        MappingIterator<Map<String, String>> reader =
                csvMapper.readerFor(Map.class)
                        .with(schema)
                        .readValues(csv);

        // Output folder
        String projectDir = System.getProperty("user.dir");
        String outputDirName = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        Path outputPath = Paths.get(projectDir, "output", outputDirName);

        Files.createDirectories(outputPath);

        System.out.println("Writing JSON files to: " + outputPath.toAbsolutePath());

        // Trip builder
        TripBuilder builder = new TripBuilder();

        Map<String, Map<String, Object>> trips = new LinkedHashMap<>();
        Instant now = Instant.now();
        String nowUtc = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 19) + "Z";

        // Read CSV & generate trips
        while (reader.hasNext()) {
            Map<String, String> row = reader.next();
            String key = row.get("F_TRIP_ID");

            Map<String, Object> trip = trips.get(key);

            if (trip == null) {
                trip = builder.createTrip(row, config, ccMapping, now, nowUtc);
                trips.put(key, trip);
            }

            Map<String, Object> vehicleTrip =
                    (Map<String, Object>) trip.get("vehicleTrip");

            List<Map<String, Object>> stops =
                    (List<Map<String, Object>>) vehicleTrip.get("stops");

            Map<String, Object> stop = buildStop(row);
            stop.put("stopSequenceId", String.valueOf(stops.size() + 1));
            stops.add(stop);

            double currentWeight = (double) vehicleTrip.get("plannedTripWeightInKg");
            currentWeight += Double.parseDouble(row.get("ORDER_WEIGHT"));
            vehicleTrip.put("plannedTripWeightInKg", currentWeight);
        }

        // Write output JSONs
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        for (Map<String, Object> t : trips.values()) {
            Map<String, Object> vehicleTrip = (Map<String, Object>) t.get("vehicleTrip");
            String tripId = (String) vehicleTrip.get("tripId");

            File outFile = outputPath.resolve(tripId + ".json").toFile();
            om.writeValue(outFile, t);
            System.out.println("Generated: " + outFile.getPath());
        }
    }

    // ------------------------- Utility methods -------------------------

    static Map<String, LocationInfoHelper> loadLocationMapping(InputStream in) throws Exception {
        Map<String, LocationInfoHelper> mapping = new HashMap<>();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();

        MappingIterator<Map<String, String>> reader =
                csvMapper.readerFor(new StringMapTypeRef())
                        .with(schema)
                        .readValues(in);

        while (reader.hasNext()) {
            Map<String, String> row = reader.next();

            String uuid = row.get("COLLECTION_LOCATION_UUID");
            String friendly = row.get("FRIENDLY_IDENTIFIER");
            String type = row.get("COLLECTION_TYPE");

            if (uuid != null && !uuid.isBlank()) {
                mapping.put(uuid.trim(),
                        new LocationInfoHelper(
                                friendly != null ? friendly.trim() : "XX",
                                type != null ? type.trim() : "UNKNOWN"
                        ));
            }
        }
        return mapping;
    }

    static Map<String, Object> buildStop(Map<String, String> r) {
        Map<String, Object> s = new LinkedHashMap<>();
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

    // ------------------------- ID builders -------------------------

    static String buildEventId(Map<String, String> r, TripConfig config,
                               Map<String, LocationInfoHelper> ccMapping) {
        return "com.tesco.transport.vanscheduling.api.Trip." +
                buildTripId(r, config) + "_" +
                buildFriendlyId(r, config, ccMapping);
    }

    static String buildTripId(Map<String, String> r, TripConfig config) {
        String storeid = r.get("MAIN_STORE_ID");
        String collectionid = r.get("COLLECTION_LOCATION_ID");
        String date = r.get("ORDER_SLOT_START_TIME").substring(2, 10).replace("-", "");
        String fid = r.get("F_TRIP_ID");
        String hh = r.get("ORDER_SLOT_START_TIME").substring(11, 13);
        String type = config.tripType;

        if ("CC".equalsIgnoreCase(type)) {
            return String.join("_", storeid, collectionid, date, fid, "1", hh, config.countryCode, type);
        } else {
            return String.join("_", storeid, date, fid, "1", hh, config.countryCode, type);
        }
    }

    static String buildFriendlyId(Map<String, String> r, TripConfig config,
                                  Map<String, LocationInfoHelper> ccMapping) {

        Instant departureInstant = Instant.parse(r.get("ORDER_SLOT_START_TIME"));
        int hour = departureInstant.atZone(java.time.ZoneOffset.UTC).getHour();
        String formattedHour = String.format("%02d", hour);

        if ("CC".equalsIgnoreCase(config.tripType)) {
            String uuid = r.get("COLLECTION_LOCATION_UUID");
            LocationInfoHelper info = ccMapping.get(uuid);

            return r.get("F_TRIP_ID") +
                    (info != null ? info.friendlyIdentifier : "XX") +
                    formattedHour;
        }

        return r.get("F_TRIP_ID") + config.getTripType() + formattedHour;
    }

    static String buildScheduleId(Map<String, String> r, TripConfig config,
                                  Map<String, LocationInfoHelper> ccMapping) {

        return "schedule-" +
                r.get("MAIN_STORE_ID") + "-" +
                config.getCountryCode() + "-" +
                config.getTripType() + "-" +
                r.get("ORDER_SLOT_START_TIME") + "-" +
                r.get("ORDER_SLOT_END_TIME") + "-FINAL-" +
                buildFriendlyId(r, config, ccMapping);
    }
}
