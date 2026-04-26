package com.westminster.smartcampus.resource;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.westminster.smartcampus.exception.InvalidRequestException;
import com.westminster.smartcampus.exception.ResourceNotFoundException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.store.InMemoryDataStore;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {
    private final InMemoryDataStore dataStore = InMemoryDataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        if (type == null || type.trim().isEmpty()) {
            return Response.ok(dataStore.getAllSensors()).build();
        }
        return Response.ok(dataStore.getSensorsByType(type)).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        return Response.ok(dataStore.getSensor(sensorId)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null) {
            throw new InvalidRequestException("Sensor request body is required.");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            throw new InvalidRequestException("Sensor roomId is required.");
        }
        Sensor createdSensor = dataStore.createSensor(sensor);
        return Response.created(buildLocation(uriInfo, createdSensor.getId()))
                .entity(createdSensor)
                .build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingsResource(@PathParam("sensorId") String sensorId) {
        if (!dataStore.sensorExists(sensorId)) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }
        return new SensorReadingResource(sensorId, dataStore);
    }

    private URI buildLocation(UriInfo uriInfo, String sensorId) {
        if (uriInfo == null) {
            return URI.create("/api/v1/sensors/" + sensorId);
        }
        return uriInfo.getAbsolutePathBuilder().path(sensorId).build();
    }
}
