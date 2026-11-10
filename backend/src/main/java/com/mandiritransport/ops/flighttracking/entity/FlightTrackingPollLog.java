package com.mandiritransport.ops.flighttracking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("flight_tracking_poll_logs")
public class FlightTrackingPollLog {

    @TableId(type = IdType.INPUT)
    private String id;
    private String credentialId;
    private OffsetDateTime requestTimestamp;
    private OffsetDateTime responseTimestamp;
    private Integer httpStatus;
    private Long responseDurationMs;
    private Boolean responseValid;
    private String parsingStatus;
    private String flightIdentifier;
    private String payloadHash;
    private String responsePayload;
    private String normalizedPayload;
    private String discoveredFieldsJson;
    private String experimentTag;
    private String errorMessage;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
    public OffsetDateTime getRequestTimestamp() { return requestTimestamp; }
    public void setRequestTimestamp(OffsetDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }
    public OffsetDateTime getResponseTimestamp() { return responseTimestamp; }
    public void setResponseTimestamp(OffsetDateTime responseTimestamp) { this.responseTimestamp = responseTimestamp; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Long getResponseDurationMs() { return responseDurationMs; }
    public void setResponseDurationMs(Long responseDurationMs) { this.responseDurationMs = responseDurationMs; }
    public Boolean getResponseValid() { return responseValid; }
    public void setResponseValid(Boolean responseValid) { this.responseValid = responseValid; }
    public String getParsingStatus() { return parsingStatus; }
    public void setParsingStatus(String parsingStatus) { this.parsingStatus = parsingStatus; }
    public String getFlightIdentifier() { return flightIdentifier; }
    public void setFlightIdentifier(String flightIdentifier) { this.flightIdentifier = flightIdentifier; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public String getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(String normalizedPayload) { this.normalizedPayload = normalizedPayload; }
    public String getDiscoveredFieldsJson() { return discoveredFieldsJson; }
    public void setDiscoveredFieldsJson(String discoveredFieldsJson) { this.discoveredFieldsJson = discoveredFieldsJson; }
    public String getExperimentTag() { return experimentTag; }
    public void setExperimentTag(String experimentTag) { this.experimentTag = experimentTag; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

