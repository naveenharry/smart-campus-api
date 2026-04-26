package com.westminster.smartcampus.exception.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse() == null
                ? Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
                : exception.getResponse().getStatus();
        String reason = Response.Status.fromStatusCode(status) == null
                ? "HTTP Error"
                : Response.Status.fromStatusCode(status).getReasonPhrase();
        return ErrorResponseFactory.build(status, reason, safeMessage(status), uriInfo);
    }

    private String safeMessage(int status) {
        if (status == Response.Status.NOT_FOUND.getStatusCode()) {
            return "The requested API resource was not found.";
        }
        if (status == Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()) {
            return "Unsupported media type. Send JSON requests with Content-Type: application/json.";
        }
        if (status == Response.Status.METHOD_NOT_ALLOWED.getStatusCode()) {
            return "The HTTP method is not allowed for this resource.";
        }
        return "The request could not be processed.";
    }
}
