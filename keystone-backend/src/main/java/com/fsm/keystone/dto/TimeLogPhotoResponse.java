package com.fsm.keystone.dto;

public record TimeLogPhotoResponse(
        Long id,
        String fileName,
        String contentType,
        String url
) {
}
