/*
 * Copyright 2016 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an update was requested against an old order.
 */
@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class NewerOrderException extends ResponseException {

    private static final long serialVersionUID = 1L;

    public NewerOrderException() {
        super();
    }

    public NewerOrderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NewerOrderException(String message) {
        super(message);
    }

    public NewerOrderException(Throwable cause) {
        super(cause);
    }

}
