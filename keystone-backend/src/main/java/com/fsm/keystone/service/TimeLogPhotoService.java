package com.fsm.keystone.service;

import com.fsm.keystone.dto.TimeLogPhotoResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.TimeLogPhoto;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessException;
import com.fsm.keystone.repository.TimeLogPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeLogPhotoService {

    private static final int MAX_PHOTOS = 5;

    private final TimeLogPhotoRepository timeLogPhotoRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    public List<TimeLogPhotoResponse> attachPhotos(TimeLog timeLog, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > MAX_PHOTOS) {
            throw new BusinessException("You can upload up to " + MAX_PHOTOS + " images per time log");
        }

        List<TimeLogPhotoResponse> saved = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            String storagePath = fileStorageService.storeTimeLogPhoto(image, timeLog.getId());
            TimeLogPhoto photo = timeLogPhotoRepository.save(TimeLogPhoto.builder()
                    .timeLog(timeLog)
                    .fileName(image.getOriginalFilename() == null ? "work-photo" : image.getOriginalFilename())
                    .contentType(image.getContentType() == null ? "image/jpeg" : image.getContentType())
                    .storagePath(storagePath)
                    .build());
            saved.add(toResponse(photo));
        }
        return saved;
    }

    public List<TimeLogPhotoResponse> mapPhotos(List<TimeLogPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }
        return photos.stream().map(this::toResponse).toList();
    }

    public Resource loadPhotoResource(Long photoId) {
        AppUser actor = currentUserService.requireUser();
        TimeLogPhoto photo = timeLogPhotoRepository.findByIdWithDetails(photoId)
                .orElseThrow(() -> new BusinessException("Photo not found"));

        assertCanViewPhoto(actor, photo);

        Path filePath = fileStorageService.resolve(photo.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException("Photo file not found");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Photo file not readable");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException("Photo file not found");
        }
    }

    public MediaType mediaTypeFor(Long photoId) {
        TimeLogPhoto photo = timeLogPhotoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException("Photo not found"));
        return MediaType.parseMediaType(photo.getContentType());
    }

    private void assertCanViewPhoto(AppUser actor, TimeLogPhoto photo) {
        if (actor.getRole() == Role.MANAGER || actor.getRole() == Role.DISPATCHER) {
            return;
        }
        if (actor.getRole() == Role.TECHNICIAN
                && photo.getTimeLog().getTechnician().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessException("Not allowed to view this photo");
    }

    private TimeLogPhotoResponse toResponse(TimeLogPhoto photo) {
        return new TimeLogPhotoResponse(
                photo.getId(),
                photo.getFileName(),
                photo.getContentType(),
                "/api/time-logs/photos/" + photo.getId()
        );
    }
}
