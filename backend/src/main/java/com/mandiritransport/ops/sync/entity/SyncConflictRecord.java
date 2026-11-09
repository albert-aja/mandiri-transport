package com.mandiritransport.ops.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("sync_conflict_records")
public class SyncConflictRecord {

    @TableId(type = IdType.INPUT)
    private String id;
    private String clientMutationId;
    private String actorUserId;
    private String entityType;
    private String entityId;
    private String mutationType;
    private Long expectedVersion;
    private Long actualVersion;
    private String clientPayloadJson;
    private String serverStateJson;
    private String conflictReason;
    private String resolutionStatus;
    private String resolvedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getMutationType() { return mutationType; }
    public void setMutationType(String mutationType) { this.mutationType = mutationType; }
    public Long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }
    public Long getActualVersion() { return actualVersion; }
    public void setActualVersion(Long actualVersion) { this.actualVersion = actualVersion; }
    public String getClientPayloadJson() { return clientPayloadJson; }
    public void setClientPayloadJson(String clientPayloadJson) { this.clientPayloadJson = clientPayloadJson; }
    public String getServerStateJson() { return serverStateJson; }
    public void setServerStateJson(String serverStateJson) { this.serverStateJson = serverStateJson; }
    public String getConflictReason() { return conflictReason; }
    public void setConflictReason(String conflictReason) { this.conflictReason = conflictReason; }
    public String getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(String resolutionStatus) { this.resolutionStatus = resolutionStatus; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
