package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception for invalid object data posted by the client.
 * Created by kpy on 2014-11-19.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidObjectDataException extends ResponseException {
    private static final long serialVersionUID = 1L;

    public InvalidObjectDataException() {
        super("Invalid object data provided as input");
    }

    public InvalidObjectDataException(String message) {
        super(message);
    }
}