package com.westminster.smartcampus.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {
    @GET
    public Response getApiMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", "Smart Campus Sensor and Room Management API");
        metadata.put("version", "1.0.0");
        metadata.put("basePath", "/api/v1");
        metadata.put("description", "RESTful JAX-RS API for rooms, sensors, and sensor reading history.");
        metadata.put("contact", "campus-facilities@example.ac.uk");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        resources.put("sensorReadings", "/api/v1/sensors/{sensorId}/readings");
        metadata.put("resources", resources);

        return Response.ok(metadata).build();
    }
}
