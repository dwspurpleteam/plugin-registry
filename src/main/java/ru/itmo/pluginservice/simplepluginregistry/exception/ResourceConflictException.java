package ru.itmo.pluginservice.simplepluginregistry.exception;

public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
