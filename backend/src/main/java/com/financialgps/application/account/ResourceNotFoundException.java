package com.financialgps.application.account;

/**
 * A resource does not exist or belongs to another owner (FR-010): one indistinguishable
 * {@code 404 RESOURCE_NOT_FOUND} body for both cases, so ids cannot be probed.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super("Resource not found");
    }
}
