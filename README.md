# Smart Campus Sensor and Room Management API

This is the coursework implementation for the 5COSC022W Client-Server Architectures assignment.

Technology choices:

- Java
- Maven
- JAX-RS using Jersey 2.x
- Grizzly embedded HTTP server
- JSON using Jersey Jackson
- In-memory data structures only

This implementation uses JAX-RS only and stores data in memory without any database dependency.

## How To Build

```powershell
mvn clean package
```

## How To Run

```powershell
mvn compile exec:java
```

The intended API base URL is:

```text
http://localhost:8080/api/v1
```

## Postman Collection

An importable Postman collection is included at:

```text
postman/SmartCampusAPI.postman_collection.json
```

## Endpoint Checklist

The API implements these endpoints:

```text
GET    /api/v1
GET    /api/v1/rooms
POST   /api/v1/rooms
GET    /api/v1/rooms/{roomId}
DELETE /api/v1/rooms/{roomId}
GET    /api/v1/sensors
GET    /api/v1/sensors?type=CO2
POST   /api/v1/sensors
GET    /api/v1/sensors/{sensorId}/readings
POST   /api/v1/sensors/{sensorId}/readings
GET    /api/v1/diagnostics/failure
```

## Report Answers

These answers address the coursework report questions.

### Part 1.1: JAX-RS Resource Lifecycle

By default, JAX-RS creates resource classes using a per-request lifecycle, so a fresh resource instance is normally used for each incoming request. This means instance fields inside `SensorRoomResource`, `SensorResource`, and `SensorReadingResource` should not be treated as durable application state. The API therefore stores shared data in a single `InMemoryDataStore` rather than inside individual resource objects. Because several HTTP requests may access the store at the same time, the store uses `ConcurrentHashMap` and synchronized methods to protect cross-collection updates, such as creating a sensor and also adding its ID to the parent room.

### Part 1.2: HATEOAS / Hypermedia

Hypermedia is useful because clients can discover available resources from API responses instead of relying only on separate static documentation. The discovery endpoint returns links for rooms, sensors, and nested sensor readings. This supports a more self-describing API: a client can start at `/api/v1`, inspect the returned links, and navigate to the available collections with less hard-coded knowledge.

### Part 2.1: Room List Responses

Returning only room IDs produces smaller responses and saves bandwidth, which can help when thousands of rooms exist. However, clients then need extra requests to retrieve names, capacities, and sensor associations. This API returns full room objects from `GET /rooms` because it makes the client simpler and provides a comprehensive room list as requested by the specification. For a much larger production system, pagination or an optional compact view could reduce payload size.

### Part 2.2: DELETE Idempotency

The room DELETE implementation is idempotent for empty rooms. If a room has sensors, deletion is blocked with `409 Conflict` and the server state does not change. If an empty room is deleted, the first request removes it and returns `204 No Content`. If the same DELETE request is sent again, the room is already absent and the API still returns `204 No Content`; the final server state remains the same after one request or many repeated requests.

### Part 3.1: @Consumes JSON Mismatch

The POST methods use `@Consumes(MediaType.APPLICATION_JSON)`, so JAX-RS expects a JSON request body with `Content-Type: application/json`. If a client sends `text/plain` or `application/xml`, Jersey cannot match the request entity to the method's declared media type and returns `415 Unsupported Media Type`. The API includes an exception mapper for JAX-RS web exceptions so this error is returned as structured JSON rather than a default HTML error page.

### Part 3.2: QueryParam Versus PathParam For Filtering

`@QueryParam` is more suitable for filtering because the resource remains the sensor collection and the query string simply narrows the representation returned from that collection. `/sensors?type=CO2` clearly means "return sensors filtered by type". A path like `/sensors/type/CO2` makes the filter look like a separate resource hierarchy, which becomes awkward when adding optional filters such as status, roomId, or multiple search criteria.

### Part 4.1: Sub-Resource Locator Pattern

The sub-resource locator keeps nested reading logic in `SensorReadingResource` instead of placing every path inside `SensorResource`. This makes the API easier to maintain because sensor collection operations and sensor reading history operations are separated into focused classes. In a larger API, this prevents one oversized controller class and allows each nested resource to evolve independently.

### Part 5.2: Why 422 For Missing Linked Resources

`422 Unprocessable Entity` is appropriate when the JSON syntax is valid but the content cannot be processed according to business rules. When a sensor is posted with a `roomId` that does not exist, the `/sensors` endpoint itself exists, so a plain `404 Not Found` could misleadingly suggest the URL is wrong. The real problem is a missing linked resource inside an otherwise understandable payload, so this API returns `422`.

### Part 5.4: Stack Trace Security Risk

Exposing Java stack traces is risky because they can reveal internal package names, class names, method names, file paths, dependency versions, and implementation details. Attackers can use that information to identify vulnerable libraries, infer business logic, and craft more targeted requests. The global exception mapper logs the technical details server-side but returns only a generic `500 Internal Server Error` JSON body to clients.

The diagnostics failure endpoint intentionally raises an unexpected runtime exception so the global exception mapper can be verified during testing. The response remains a generic JSON `500` and does not expose the Java exception type or stack trace.

### Part 5.5: JAX-RS Filters For Logging

JAX-RS filters are better for logging because logging is a cross-cutting concern that applies to every endpoint. A `ContainerRequestFilter` and `ContainerResponseFilter` handle method, URI, and status code logging in one place. This avoids duplicated `Logger.info()` calls in each resource method and ensures new endpoints automatically receive the same logging behavior.

## Sample Curl Commands

Run these after starting the server with `mvn compile exec:java`.

### Discovery

```cmd
curl http://localhost:8080/api/v1
```

### List Rooms

```cmd
curl http://localhost:8080/api/v1/rooms
```

### Create Room

```cmd
curl -i -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":40}"
```

### Get Room By ID

```cmd
curl http://localhost:8080/api/v1/rooms/LIB-301
```

### Filter Sensors By Type

```cmd
curl http://localhost:8080/api/v1/sensors?type=CO2
```

### Create Sensor

```cmd
curl -i -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"LIB-301\"}"
```

### Add Sensor Reading

```cmd
curl -i -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d "{\"value\":715.5}"
```

### Get Sensor Reading History

```cmd
curl http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### Delete Room With Sensors (409 Conflict)

```cmd
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### Create Sensor With Missing Room (422 Unprocessable Entity)

```cmd
curl -i -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"BAD-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"NO-ROOM\"}"
```

### Create Maintenance Sensor

```cmd
curl -i -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"TEMP-099\",\"type\":\"Temperature\",\"status\":\"MAINTENANCE\",\"currentValue\":0,\"roomId\":\"LIB-301\"}"
```

### Post Reading To Maintenance Sensor (403 Forbidden)

```cmd
curl -i -X POST http://localhost:8080/api/v1/sensors/TEMP-099/readings -H "Content-Type: application/json" -d "{\"value\":18.4}"
```

### Trigger Global Safety Net (500 Internal Server Error)

```cmd
curl -i http://localhost:8080/api/v1/diagnostics/failure
```
