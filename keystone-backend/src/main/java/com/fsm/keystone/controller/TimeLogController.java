package com.fsm.keystone.controller;

import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.dto.TimeLogResponse;
import com.fsm.keystone.service.TimeLogPhotoService;
import com.fsm.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
public class TimeLogController {

    private final WorkOrderService workOrderService;
    private final TimeLogPhotoService timeLogPhotoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public List<TimeLogResponse> all() {
        return workOrderService.listTimeLogs();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public TimeLogResponse createTimeLog(@Valid @RequestBody TimeLogRequest request) {
        return workOrderService.addTimeLog(request.workOrderId(), request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public TimeLogResponse createTimeLogWithPhotos(
            @RequestParam Long workOrderId,
            @RequestParam Long technicianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String workDescription,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        TimeLogRequest request = new TimeLogRequest(
                workOrderId,
                technicianId,
                startTime,
                endTime,
                workDescription
        );
        List<MultipartFile> files = images == null ? List.of() : images;
        return workOrderService.addTimeLog(workOrderId, request, files);
    }

    @GetMapping("/photos/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public ResponseEntity<Resource> photo(@PathVariable Long id) {
        Resource resource = timeLogPhotoService.loadPhotoResource(id);
        return ResponseEntity.ok()
                .contentType(timeLogPhotoService.mediaTypeFor(id))
                .body(resource);
    }
}
