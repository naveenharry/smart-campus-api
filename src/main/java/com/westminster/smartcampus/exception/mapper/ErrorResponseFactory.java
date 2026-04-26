package com.westminster.smartcampus.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.westminster.smartcampus.dto.ErrorResponse;

final class ErrorResponseFactory {
    private ErrorResponseFactory() {
    }

    static Response build(Response.Status status, String message, UriInfo uriInfo) {
        return build(status.getStatusCode(), status.getReasonPhrase(), message, uriInfo);
    }

    static Response build(int status, String error, String message, UriInfo uriInfo) {
        ErrorResponse body = new ErrorResponse(status, error, message, requestPath(uriInfo),
                System.currentTimeMillis());
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private static String requestPath(UriInfo uriInfo) {
        if (uriInfo == null || uriInfo.getRequestUri() == null) {
            return "";
        }
        return uriInfo.getRequestUri().getPath();
    }
}
