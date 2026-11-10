package com.mandiritransport.ops.flighttracking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@TableName("flight_tracking_credentials")
public class FlightTrackingCredential {

    @TableId(type = IdType.INPUT)
    private String id;
    private String flightId;
    private String tripId;
    private String provider;
    private String flightNumber;
    private LocalDate flightDate;
    @JsonIgnore
    private String trackingTokenEncrypted;
    @JsonIgnore
    private String userTokenEncrypted;
    private Boolean hasUserToken;
    private String tokenFingerprint;
    private Integer pollingIntervalSeconds;
    private Boolean singleFlight;
    private String trackingPageUrl;
    private String tokenStatus; // ACTIVE, EXPIRED, INVALID, UNKNOWN, SUPERSEDED, DISABLED
    private OffsetDateTime extractedAt;
    private OffsetDateTime lastValidatedAt;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime expiresAt;
    private String validationNotes;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;
    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public LocalDate getFlightDate() { return flightDate; }
    public void setFlightDate(LocalDate flightDate) { this.flightDate = flightDate; }
    public String getTrackingTokenEncrypted() { return trackingTokenEncrypted; }
    public void setTrackingTokenEncrypted(String trackingTokenEncrypted) { this.trackingTokenEncrypted = trackingTokenEncrypted; }
    public String getUserTokenEncrypted() { return userTokenEncrypted; }
    public void setUserTokenEncrypted(String userTokenEncrypted) { this.userTokenEncrypted = userTokenEncrypted; }
    public Boolean getHasUserToken() { return hasUserToken; }
    public void setHasUserToken(Boolean hasUserToken) { this.hasUserToken = hasUserToken; }
    public String getTokenFingerprint() { return tokenFingerprint; }
    public void setTokenFingerprint(String tokenFingerprint) { this.tokenFingerprint = tokenFingerprint; }
    public Integer getPollingIntervalSeconds() { return pollingIntervalSeconds; }
    public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) { this.pollingIntervalSeconds = pollingIntervalSeconds; }
    public Boolean getSingleFlight() { return singleFlight; }
    public void setSingleFlight(Boolean singleFlight) { this.singleFlight = singleFlight; }
    public String getTrackingPageUrl() { return trackingPageUrl; }
    public void setTrackingPageUrl(String trackingPageUrl) { this.trackingPageUrl = trackingPageUrl; }
    public String getTokenStatus() { return tokenStatus; }
    public void setTokenStatus(String tokenStatus) { this.tokenStatus = tokenStatus; }
    public OffsetDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(OffsetDateTime extractedAt) { this.extractedAt = extractedAt; }
    public OffsetDateTime getLastValidatedAt() { return lastValidatedAt; }
    public void setLastValidatedAt(OffsetDateTime lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public String toString() {
        return "FlightTrackingCredential{" +
                "id='" + id + '\'' +
                ", provider='" + provider + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", flightDate=" + flightDate +
                ", tokenFingerprint='" + tokenFingerprint + '\'' +
                ", tokenStatus='" + tokenStatus + '\'' +
                '}';
    }
}

