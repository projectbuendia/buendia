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

/**
 * Exception to indicate that a ProjectBuendia-specific part of configuration
 * is invalid.
 */
public class ConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 3372903640609418225L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String format, Object... args) {
        super(String.format(format, args));
    }

    // Parameters are the opposite way round to normal to support formatted messages more clearly.
    public ConfigurationException(Throwable cause, String message) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }
}
