package com.apexagro.identity.api;

import java.util.UUID;

public interface IdentityApi {
    boolean isIdentified(UUID userId);
    boolean hasRole(UUID userId, String role);
    // ContactDto: телефон и язык пользователя для отправки SMS
    // UserSummaryDto: id, phone, fullName, verificationLevel
}