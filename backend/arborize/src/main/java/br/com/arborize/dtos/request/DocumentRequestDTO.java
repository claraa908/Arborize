package br.com.arborize.dtos.request;

import org.springframework.web.multipart.MultipartFile;

public record DocumentRequestDTO(
        MultipartFile file
) {
}
