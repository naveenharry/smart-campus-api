package com.westminster.smartcampus.resource;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.store.InMemoryDataStore;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class SensorRoomResource {
    private final InMemoryDataStore dataStore = InMemoryDataStore.getInstance();

    @GET
    public Response getAllRooms() {
        return Response.ok(dataStore.getAllRooms()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        Room createdRoom = dataStore.createRoom(room);
        return Response.created(buildLocation(uriInfo, createdRoom.getId()))
                .entity(createdRoom)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        return Response.ok(dataStore.getRoom(roomId)).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        if (dataStore.roomHasSensors(roomId)) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted because it has active sensors.");
        }
        dataStore.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private URI buildLocation(UriInfo uriInfo, String roomId) {
        if (uriInfo == null) {
            return URI.create("/api/v1/rooms/" + roomId);
        }
        return uriInfo.getAbsolutePathBuilder().path(roomId).build();
    }
}
