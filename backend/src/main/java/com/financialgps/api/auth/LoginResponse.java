package com.financialgps.api.auth;

import java.util.UUID;

/** Login response — identity only. */
public record LoginResponse(UUID id, String email) {
}
