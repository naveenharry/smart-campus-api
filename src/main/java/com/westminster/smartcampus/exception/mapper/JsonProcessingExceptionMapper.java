package com.westminster.smartcampus.exception.mapper;

import javax.annotation.Priority;
import javax.ws.rs.core.Context;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

@Provider
@Priority(Priorities.USER - 100)
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(JsonProcessingException exception) {
        return ErrorResponseFactory.build(Response.Status.BAD_REQUEST, "Malformed JSON request body.", uriInfo);
    }
}
