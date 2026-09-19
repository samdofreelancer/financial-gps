package com.financialgps.application.account;

/** Account deletion without the exact {@code confirmation:"DELETE"} body (FR-012). */
public class ConfirmationRequiredException extends RuntimeException {

    public ConfirmationRequiredException() {
        super("Send {\"confirmation\":\"DELETE\"} to confirm this irreversible action");
    }
}
