// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception for invalid object data posted by the client. */
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
