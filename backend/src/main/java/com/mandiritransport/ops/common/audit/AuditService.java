package com.mandiritransport.ops.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.auth.SecurityUser;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.common.audit.mapper.AuditLogMapper;
import com.mandiritransport.ops.common.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuditService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void log(String entityType, String entityId, String action, String summary, Object before, Object after) {
        SecurityUser current = SecurityUtils.currentUserOrNull();
        AuditLog entry = new AuditLog();
        entry.setId(IdGenerator.newId());
        if (current != null) {
            entry.setActorUserId(current.userId());
            entry.setActorRole(current.role());
        }
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setSummary(summary);
        entry.setBeforeJson(toJsonSafe(before));
        entry.setAfterJson(toJsonSafe(after));
        entry.setCreatedAt(OffsetDateTime.now());
        auditLogMapper.insert(entry);
    }

    public String toJsonSafe(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
