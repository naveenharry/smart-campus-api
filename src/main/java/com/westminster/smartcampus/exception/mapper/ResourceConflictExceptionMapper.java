package com.westminster.smartcampus.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.westminster.smartcampus.exception.ResourceConflictException;

@Provider
public class ResourceConflictExceptionMapper implements ExceptionMapper<ResourceConflictException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceConflictException exception) {
        return ErrorResponseFactory.build(Response.Status.CONFLICT, exception.getMessage(), uriInfo);
    }
}
