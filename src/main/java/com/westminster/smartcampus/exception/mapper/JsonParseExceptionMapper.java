package com.westminster.smartcampus.exception.mapper;

import javax.annotation.Priority;
import javax.ws.rs.core.Context;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;

@Provider
@Priority(Priorities.USER - 100)
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(JsonParseException exception) {
        return ErrorResponseFactory.build(Response.Status.BAD_REQUEST, "Malformed JSON request body.", uriInfo);
    }
}
