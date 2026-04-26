package com.westminster.smartcampus.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.westminster.smartcampus.exception.ResourceNotFoundException;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        return ErrorResponseFactory.build(Response.Status.NOT_FOUND, exception.getMessage(), uriInfo);
    }
}
