package com.apexagro.audit.api;

import java.util.UUID;

public interface AuditApi {
    void record(String action, String entityType, UUID entityId, Object diff);
}