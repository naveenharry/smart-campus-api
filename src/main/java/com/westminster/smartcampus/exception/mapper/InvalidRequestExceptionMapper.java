package com.westminster.smartcampus.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.westminster.smartcampus.exception.InvalidRequestException;

@Provider
public class InvalidRequestExceptionMapper implements ExceptionMapper<InvalidRequestException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidRequestException exception) {
        return ErrorResponseFactory.build(Response.Status.BAD_REQUEST, exception.getMessage(), uriInfo);
    }
}
