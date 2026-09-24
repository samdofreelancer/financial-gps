package com.financialgps.application.account.model;

/**
 * Identity &amp; Access roles. 007 grants exactly one role, so this is a plain constant rather than
 * an enum hierarchy; the API adapter uses it to build the authenticated principal.
 */
public final class OwnerRole {

    /** Every registered owner holds this role ({@code account.role}). */
    public static final String OWNER = "OWNER";

    private OwnerRole() {
    }
}
