package com.financialgps.application.account;

/**
 * Login failure (FR-002). One identical body must be produced for unknown email AND wrong
 * password — the anti-enumeration guarantee (research §6, AntiRevelationTest).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }
}
