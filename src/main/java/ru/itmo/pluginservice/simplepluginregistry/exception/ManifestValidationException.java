package ru.itmo.pluginservice.simplepluginregistry.exception;

import java.util.List;

public class ManifestValidationException extends RuntimeException {

    private final List<String> errors;

    public ManifestValidationException(List<String> errors) {
        super("Manifest validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
