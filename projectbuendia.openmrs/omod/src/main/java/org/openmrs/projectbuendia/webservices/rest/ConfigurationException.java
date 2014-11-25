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

    public ConfigurationException(String messageFormat, Object... messageArguments) {
        super(String.format(messageFormat, messageArguments));
    }

    // Parameters are the opposite way round to normal to support formatted messages more clearly.
    public ConfigurationException(Throwable cause, String message) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause, String messageFormat, Object... messageArguments) {
        super(String.format(messageFormat, messageArguments), cause);
    }
}
