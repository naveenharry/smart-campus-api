package com.westminster.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.westminster.smartcampus.dto.ErrorResponse;
import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.exception.mapper.GlobalExceptionMapper;
import com.westminster.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.model.SensorStatus;
import com.westminster.smartcampus.resource.DiagnosticsResource;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.SensorReadingResource;
import com.westminster.smartcampus.resource.SensorResource;
import com.westminster.smartcampus.resource.SensorRoomResource;
import com.westminster.smartcampus.store.InMemoryDataStore;

class SmartCampusApiTest {
    private final InMemoryDataStore dataStore = InMemoryDataStore.getInstance();
    private SensorRoomResource roomResource;
    private SensorResource sensorResource;

    @BeforeEach
    void setUp() {
        dataStore.clear();
        roomResource = new SensorRoomResource();
        sensorResource = new SensorResource();
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoveryEndpointReturnsMetadataAndLinks() {
        Response response = new DiscoveryResource().getApiMetadata();

        assertEquals(200, response.getStatus());
        Map<String, Object> metadata = (Map<String, Object>) response.getEntity();
        assertEquals("1.0.0", metadata.get("version"));
        assertTrue(metadata.containsKey("resources"));
    }

    @Test
    void roomLifecycleSupportsCreateReadListAndIdempotentDelete() {
        Response created = roomResource.createRoom(new Room("LIB-301", "Library Quiet Study", 40), null);
        assertEquals(201, created.getStatus());
        assertNotNull(created.getLocation());

        Response detail = roomResource.getRoomById("LIB-301");
        Room room = (Room) detail.getEntity();
        assertEquals("Library Quiet Study", room.getName());

        Response list = roomResource.getAllRooms();
        assertEquals(200, list.getStatus());

        assertEquals(204, roomResource.deleteRoom("LIB-301").getStatus());
        assertEquals(204, roomResource.deleteRoom("LIB-301").getStatus());
    }

    @Test
    void deletingRoomWithSensorsReturnsConflictScenario() {
        roomResource.createRoom(new Room("LAB-101", "IoT Lab", 30), null);
        sensorResource.createSensor(new Sensor("CO2-001", "CO2", SensorStatus.ACTIVE, 0.0, "LAB-101"), null);

        assertThrows(RoomNotEmptyException.class, () -> roomResource.deleteRoom("LAB-101"));
    }

    @Test
    void creatingSensorRequiresExistingRoom() {
        Sensor sensor = new Sensor("TEMP-001", "Temperature", SensorStatus.ACTIVE, 21.5, "NO-ROOM");

        assertThrows(LinkedResourceNotFoundException.class, () -> sensorResource.createSensor(sensor, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sensorFilteringUsesCaseInsensitiveQueryType() {
        roomResource.createRoom(new Room("LIB-301", "Library Quiet Study", 40), null);
        sensorResource.createSensor(new Sensor("CO2-001", "CO2", SensorStatus.ACTIVE, 0.0, "LIB-301"), null);
        sensorResource.createSensor(new Sensor("TEMP-001", "Temperature", SensorStatus.ACTIVE, 20.0, "LIB-301"),
                null);

        Response response = sensorResource.getSensors("co2");
        List<Sensor> sensors = (List<Sensor>) response.getEntity();

        assertEquals(1, sensors.size());
        assertEquals("CO2-001", sensors.get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void postingReadingAddsHistoryAndUpdatesParentSensorCurrentValue() {
        roomResource.createRoom(new Room("LIB-301", "Library Quiet Study", 40), null);
        sensorResource.createSensor(new Sensor("CO2-001", "CO2", SensorStatus.ACTIVE, 0.0, "LIB-301"), null);

        SensorReadingResource readings = sensorResource.getSensorReadingsResource("CO2-001");
        Response created = readings.appendReading(new SensorReading(null, 0, 715.5), null);

        assertEquals(201, created.getStatus());
        SensorReading reading = (SensorReading) created.getEntity();
        assertNotNull(reading.getId());
        assertTrue(reading.getTimestamp() > 0);

        Sensor parent = (Sensor) sensorResource.getSensorById("CO2-001").getEntity();
        assertEquals(715.5, parent.getCurrentValue());

        List<SensorReading> history = (List<SensorReading>) readings.getReadingHistory().getEntity();
        assertEquals(1, history.size());
    }

    @Test
    void maintenanceSensorCannotAcceptReadings() {
        roomResource.createRoom(new Room("PLANT-1", "Plant Room", 10), null);
        sensorResource.createSensor(new Sensor("TEMP-099", "Temperature", SensorStatus.MAINTENANCE, 0.0, "PLANT-1"),
                null);

        SensorReadingResource readings = sensorResource.getSensorReadingsResource("TEMP-099");

        assertThrows(SensorUnavailableException.class,
                () -> readings.appendReading(new SensorReading(null, 0, 18.4), null));
    }

    @Test
    void exceptionMappersReturnStructuredJsonAndSafeMessages() {
        Response linkedResourceResponse = new LinkedResourceNotFoundExceptionMapper()
                .toResponse(new LinkedResourceNotFoundException("Missing linked room"));
        assertEquals(422, linkedResourceResponse.getStatus());
        assertTrue(linkedResourceResponse.getEntity() instanceof ErrorResponse);

        RuntimeException diagnosticsFailure = assertThrows(RuntimeException.class,
                () -> new DiagnosticsResource().simulateUnexpectedFailure());

        Logger mapperLogger = Logger.getLogger(GlobalExceptionMapper.class.getName());
        Level previousLevel = mapperLogger.getLevel();
        mapperLogger.setLevel(Level.OFF);
        Response globalResponse;
        try {
            globalResponse = new GlobalExceptionMapper().toResponse(diagnosticsFailure);
        } finally {
            mapperLogger.setLevel(previousLevel);
        }
        ErrorResponse error = (ErrorResponse) globalResponse.getEntity();

        assertEquals(500, globalResponse.getStatus());
        assertFalse(error.getMessage().contains("IllegalStateException"));
        assertFalse(error.getMessage().contains("diagnostics failure"));
    }
}
