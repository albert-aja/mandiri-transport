package com.mandiritransport.ops.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("sync_mutations_log")
public class SyncMutationLog {

    @TableId(value = "client_mutation_id", type = IdType.INPUT)
    private String clientMutationId;
    private String actorUserId;
    private String mutationType;
    private String entityId;
    private String outcomeStatus;
    private String responseJson;
    private OffsetDateTime processedAt;

    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }
    public String getMutationType() { return mutationType; }
    public void setMutationType(String mutationType) { this.mutationType = mutationType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }
    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
