package com.westminster.smartcampus.exception.mapper;

import javax.annotation.Priority;
import javax.ws.rs.core.Context;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;

@Provider
@Priority(Priorities.USER - 100)
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(JsonMappingException exception) {
        return ErrorResponseFactory.build(Response.Status.BAD_REQUEST, "Malformed JSON request body.", uriInfo);
    }
}
