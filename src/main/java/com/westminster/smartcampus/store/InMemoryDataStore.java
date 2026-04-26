package com.westminster.smartcampus.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.westminster.smartcampus.exception.InvalidRequestException;
import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.exception.ResourceConflictException;
import com.westminster.smartcampus.exception.ResourceNotFoundException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.model.SensorStatus;

public final class InMemoryDataStore {
    private static final InMemoryDataStore INSTANCE = new InMemoryDataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readingsBySensorId = new ConcurrentHashMap<>();

    private InMemoryDataStore() {
    }

    public static InMemoryDataStore getInstance() {
        return INSTANCE;
    }

    public synchronized List<Room> getAllRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::getId))
                .map(this::copyRoom)
                .collect(Collectors.toList());
    }

    public synchronized Room createRoom(Room room) {
        if (room == null) {
            throw new InvalidRequestException("Room request body is required.");
        }

        String id = requireText(room.getId(), "Room id is required.");
        String name = requireText(room.getName(), "Room name is required.");
        if (room.getCapacity() < 0) {
            throw new InvalidRequestException("Room capacity cannot be negative.");
        }
        if (rooms.containsKey(id)) {
            throw new ResourceConflictException("Room already exists: " + id);
        }

        // Room-to-sensor links are owned by server logic, not client payloads.
        Room storedRoom = new Room(id, name, room.getCapacity(), new ArrayList<>());
        rooms.put(id, storedRoom);
        return copyRoom(storedRoom);
    }

    public synchronized Room getRoom(String roomId) {
        String id = requireText(roomId, "Room id is required.");
        Room room = rooms.get(id);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + id);
        }
        return copyRoom(room);
    }

    public synchronized boolean roomExists(String roomId) {
        if (isBlank(roomId)) {
            return false;
        }
        return rooms.containsKey(roomId.trim());
    }

    public synchronized boolean roomHasSensors(String roomId) {
        if (isBlank(roomId)) {
            return false;
        }
        Room room = rooms.get(roomId.trim());
        return room != null && room.getSensorIds() != null && !room.getSensorIds().isEmpty();
    }

    public synchronized void deleteRoom(String roomId) {
        if (isBlank(roomId)) {
            throw new InvalidRequestException("Room id is required.");
        }
        rooms.remove(roomId.trim());
    }

    public synchronized List<Sensor> getAllSensors() {
        return sensors.values().stream()
                .sorted(Comparator.comparing(Sensor::getId))
                .map(this::copySensor)
                .collect(Collectors.toList());
    }

    public synchronized List<Sensor> getSensorsByType(String type) {
        if (isBlank(type)) {
            return getAllSensors();
        }
        String requestedType = type.trim();
        return sensors.values().stream()
                .filter(sensor -> sensor.getType() != null && sensor.getType().equalsIgnoreCase(requestedType))
                .sorted(Comparator.comparing(Sensor::getId))
                .map(this::copySensor)
                .collect(Collectors.toList());
    }

    public synchronized Sensor createSensor(Sensor sensor) {
        if (sensor == null) {
            throw new InvalidRequestException("Sensor request body is required.");
        }

        String id = requireText(sensor.getId(), "Sensor id is required.");
        String type = requireText(sensor.getType(), "Sensor type is required.");
        String roomId = requireText(sensor.getRoomId(), "Sensor roomId is required.");
        String status = normalizeStatus(sensor.getStatus());

        Room room = rooms.get(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException("Cannot create sensor because roomId does not exist: " + roomId);
        }
        if (sensors.containsKey(id)) {
            throw new ResourceConflictException("Sensor already exists: " + id);
        }

        Sensor storedSensor = new Sensor(id, type, status, sensor.getCurrentValue(), roomId);
        sensors.put(id, storedSensor);
        readingsBySensorId.putIfAbsent(id, new ArrayList<>());
        if (!room.getSensorIds().contains(id)) {
            room.getSensorIds().add(id);
        }
        return copySensor(storedSensor);
    }

    public synchronized Sensor getSensor(String sensorId) {
        String id = requireText(sensorId, "Sensor id is required.");
        Sensor sensor = sensors.get(id);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + id);
        }
        return copySensor(sensor);
    }

    public synchronized boolean sensorExists(String sensorId) {
        if (isBlank(sensorId)) {
            return false;
        }
        return sensors.containsKey(sensorId.trim());
    }

    public synchronized List<SensorReading> getReadingsForSensor(String sensorId) {
        String id = requireText(sensorId, "Sensor id is required.");
        if (!sensors.containsKey(id)) {
            throw new ResourceNotFoundException("Sensor not found: " + id);
        }
        return readingsBySensorId.getOrDefault(id, new ArrayList<>()).stream()
                .map(this::copyReading)
                .collect(Collectors.toList());
    }

    public synchronized SensorReading appendReading(String sensorId, SensorReading reading) {
        String id = requireText(sensorId, "Sensor id is required.");
        if (!sensors.containsKey(id)) {
            throw new ResourceNotFoundException("Sensor not found: " + id);
        }
        if (reading == null) {
            throw new InvalidRequestException("Sensor reading request body is required.");
        }

        String readingId = isBlank(reading.getId()) ? UUID.randomUUID().toString() : reading.getId().trim();
        long timestamp = reading.getTimestamp() > 0 ? reading.getTimestamp() : System.currentTimeMillis();
        SensorReading storedReading = new SensorReading(readingId, timestamp, reading.getValue());

        readingsBySensorId.computeIfAbsent(id, ignored -> new ArrayList<>()).add(storedReading);
        return copyReading(storedReading);
    }

    public synchronized void updateSensorCurrentValue(String sensorId, double currentValue) {
        String id = requireText(sensorId, "Sensor id is required.");
        Sensor sensor = sensors.get(id);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + id);
        }
        sensor.setCurrentValue(currentValue);
    }

    public synchronized void clear() {
        rooms.clear();
        sensors.clear();
        readingsBySensorId.clear();
    }

    private Room copyRoom(Room room) {
        return new Room(room.getId(), room.getName(), room.getCapacity(), safeList(room.getSensorIds()));
    }

    private Sensor copySensor(Sensor sensor) {
        return new Sensor(sensor.getId(), sensor.getType(), sensor.getStatus(), sensor.getCurrentValue(),
                sensor.getRoomId());
    }

    private SensorReading copyReading(SensorReading reading) {
        return new SensorReading(reading.getId(), reading.getTimestamp(), reading.getValue());
    }

    private List<String> safeList(List<String> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(source);
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return SensorStatus.ACTIVE;
        }

        String normalized = status.trim().toUpperCase();
        if (!SensorStatus.ACTIVE.equals(normalized)
                && !SensorStatus.MAINTENANCE.equals(normalized)
                && !SensorStatus.OFFLINE.equals(normalized)) {
            throw new InvalidRequestException("Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE.");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new InvalidRequestException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
