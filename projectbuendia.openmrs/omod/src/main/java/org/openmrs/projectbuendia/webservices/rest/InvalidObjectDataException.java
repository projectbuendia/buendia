package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception for invalid object data posted by the client.
 * Created by kpy on 2014-11-19.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid object data provided as input")
public class InvalidObjectDataException extends ResponseException {
    private static final long serialVersionUID = 1L;

    public InvalidObjectDataException() {
        super();
    }

    public InvalidObjectDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidObjectDataException(String message) {
        super(message);
    }

    public InvalidObjectDataException(Throwable cause) {
        super(cause);
    }
}
