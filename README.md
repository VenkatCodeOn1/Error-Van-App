# Error Van

A Java-based project for generating JSON reports from CSV data.  
This project is developed using **Java 17** and tested in **IntelliJ IDEA**.

---

## 🚀 Features
- Reads input CSV file
- Converts data into structured JSON
- Creates dated folders automatically
- Stores output JSON files inside project directory
- Easy integration with Kafka (optional)

---

## 🛠️ Tech Stack
- Java (JDK 17 or later)
- Maven (or Gradle, depending on your setup)
- IntelliJ IDEA

---

PURPOSE:

In scenarios where customer orders are missed or not scheduled for delivery, an Error Van is allocated to fulfill those unscheduled orders. To support this recovery process:

A Couchbase query is executed to extract all unscheduled orders from the database by using query.txt file

The result of this query is exported as a CSV file, which contains all relevant information about the orders, including F_TRIP_ID (trip ID).

This CSV file is converted into a structured JSON format using an automated Java or Shell script.

During the conversion:

JSON files are generated based on F_TRIP_ID, meaning each unique trip ID results in one JSON file.

If multiple orders share the same trip ID, they are grouped into a single JSON file containing multiple stops (orders) within the trip.

The generated JSON files are then republished to a Kafka topic, enabling downstream systems to reprocess and schedule these orders for delivery via the allocated error van.

This ensures a reliable and automated mechanism for identifying missed orders and reassigning them for delivery, minimizing customer impact.

---Couchbase Query for generation the csv file (Input File)---

SELECT meta(t).id,
t.reservationStatus,
t.selectedSlot.windowStart as ORDER_SLOT_START_TIME,
t.selectedSlot.windowEnd as ORDER_SLOT_END_TIME,
t.consignment.logisticsUnits[0].customerOrderShortId as CUSTOMER_ORDER_SHORT_ID,
t.consignment.logisticsUnits[0].customerOrderUUID as CUSTOMER_ORDER_UUID,
t.consignment.logisticsUnits[0].fulfilmentOrderShortId as FO_SHORT_ID,
t.consignment.logisticsUnits[0].fulfilmentOrderUUID as FO_UUID,
t.consignment.shipTo.geoLocation.geoCoordinate.latitude as LATITUDE,
t.consignment.shipTo.geoLocation.geoCoordinate.longitude as LONGITUDE,
t.consignment.logisticsUnits[0].weightIncludingPackaging.`value` as ORDER_WEIGHT ,
t.consignment.shipTo.address.postcode as SHIP_TO_POSTCODE,
t.consignment.configLocationId as CONFIG_LOCATION_UUID,
t.consignment.shipFrom.locationUUId as MAIN_STORE_LOCATION_UUID,
t.consignment.shipFrom.locationId as MAIN_STORE_ID,
t.consignment.shipTo.locationId as COLLECTION_LOCATION_ID,
t.consignment.shipTo.locationUUId as COLLECTION_LOCATION_UUID,
t.selectedShippingOption.serviceType as VEHICLE_TYPE,
t.selectedShippingOption.fulfilmentMethod as fulfilmentMethod,
t.selectedShippingOption.fulfilmentProposition as fulfilmentProposition
FROM Transport t
WHERE  (meta(t).id IN [
"r::10305209018",
"r::10302848248",
"r::10304423026",
"r::10304520626"
"r::10311803704"
])

-------------

## 📂 Project Structure
ReportGeneration/
├── src/
│ ├── main/
│ │ ├── java/ # Java source files
│ │ └── resources/ # Config CSV (CorrectConfig.csv)
│ └── test/ # Unit tests
├── target/ # Build output (ignored in Git)
├── pom.xml # Maven dependencies
└── README.md
