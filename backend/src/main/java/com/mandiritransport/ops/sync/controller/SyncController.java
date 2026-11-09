package com.mandiritransport.ops.sync.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.sync.dto.SyncDtos.DriverBootstrapResponse;
import com.mandiritransport.ops.sync.dto.SyncDtos.ResolveSyncConflictRequest;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationsRequest;
import com.mandiritransport.ops.sync.dto.SyncDtos.SyncMutationsResponse;
import com.mandiritransport.ops.sync.entity.SyncConflictRecord;
import com.mandiritransport.ops.sync.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/driver-bootstrap")
    public ApiResponse<DriverBootstrapResponse> driverBootstrap() {
        return ApiResponse.ok(syncService.getDriverBootstrap());
    }

    @PostMapping("/mutations")
    public ApiResponse<SyncMutationsResponse> submitMutations(@Valid @RequestBody SyncMutationsRequest req) {
        return ApiResponse.ok(syncService.processMutations(req));
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<SyncConflictRecord>> listConflicts(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(syncService.listConflicts(status));
    }

    @PostMapping("/conflicts/{id}/resolve")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<SyncConflictRecord> resolveConflict(@PathVariable String id,
                                                           @Valid @RequestBody ResolveSyncConflictRequest req) {
        return ApiResponse.ok(syncService.resolveConflict(id, req));
    }
}
