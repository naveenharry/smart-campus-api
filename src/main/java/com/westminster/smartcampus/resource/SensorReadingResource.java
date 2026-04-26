package com.westminster.smartcampus.resource;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.model.SensorStatus;
import com.westminster.smartcampus.store.InMemoryDataStore;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
    private final String sensorId;
    private final InMemoryDataStore dataStore;

    public SensorReadingResource(String sensorId, InMemoryDataStore dataStore) {
        this.sensorId = sensorId;
        this.dataStore = dataStore;
    }

    @GET
    public Response getReadingHistory() {
        return Response.ok(dataStore.getReadingsForSensor(sensorId)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response appendReading(SensorReading reading, @Context UriInfo uriInfo) {
        if (SensorStatus.MAINTENANCE.equalsIgnoreCase(dataStore.getSensor(sensorId).getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor " + sensorId + " is in MAINTENANCE and cannot accept new readings.");
        }

        SensorReading storedReading = dataStore.appendReading(sensorId, reading);
        dataStore.updateSensorCurrentValue(sensorId, storedReading.getValue());
        return Response.created(buildLocation(uriInfo, storedReading.getId()))
                .entity(storedReading)
                .build();
    }

    public String getSensorId() {
        return sensorId;
    }

    private URI buildLocation(UriInfo uriInfo, String readingId) {
        if (uriInfo == null) {
            return URI.create("/api/v1/sensors/" + sensorId + "/readings/" + readingId);
        }
        return uriInfo.getAbsolutePathBuilder().path(readingId).build();
    }
}
