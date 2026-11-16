package br.com.arborize.controllers;

import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.services.EducationalMaterialService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationalMaterialControllerTest {

    @Mock
    private EducationalMaterialService service;

    @InjectMocks
    private EducationalMaterialController controller;

    @Test
    void atualizaStatusDelegandoIdEValorAoServico() {
        EducationalMaterialResponseDTO dto = mock(EducationalMaterialResponseDTO.class);
        when(service.updateStatus(5L, "PUBLICADO")).thenReturn(dto);

        ResponseEntity<EducationalMaterialResponseDTO> response =
                controller.updateStatus(5L, java.util.Map.of("status", "PUBLICADO"));

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
        verify(service).updateStatus(5L, "PUBLICADO");
    }

    @Test
    void downloadDePdfRetornaBytesETipoDeConteudo() {
        byte[] pdf = {7, 8, 9};
        when(service.getFileStream(5L)).thenReturn(pdf);

        ResponseEntity<byte[]> response = controller.downloadPdf(5L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("cartilha_5.pdf", response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(pdf, response.getBody());
    }

    @Test
    void capaRetornaBytesComTipoImagem() {
        byte[] cover = {10, 11};
        when(service.getCoverStream(5L)).thenReturn(cover);

        ResponseEntity<byte[]> response = controller.getCoverPhoto(5L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertArrayEquals(cover, response.getBody());
    }

    @Test
    void exclusaoDeMaterialRetornaSemConteudo() {
        ResponseEntity<Void> response = controller.deleteMaterial(5L);

        assertEquals(204, response.getStatusCode().value());
        verify(service).deleteMaterial(5L);
    }

    @Test
    void deveListarMateriaisPublicadosComSucessoNoController() {
        // Arrange
        EducationalMaterialResponseDTO dtoMock = mock(EducationalMaterialResponseDTO.class);
        when(service.getPublicMaterials(any(), any())).thenReturn(Collections.singletonList(dtoMock));

        // Act
        ResponseEntity<List<EducationalMaterialResponseDTO>> response = controller.getPublicMaterials(null, null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(service, times(1)).getPublicMaterials(any(), any());
    }

    @Test
    void deveListarTodosOsMateriaisParaAdminComSucessoNoController() {
        // Arrange
        EducationalMaterialResponseDTO dtoMock = mock(EducationalMaterialResponseDTO.class);
        when(service.getAllMaterialsForAdmin()).thenReturn(Collections.singletonList(dtoMock));

        // Act
        ResponseEntity<List<EducationalMaterialResponseDTO>> response = controller.getAllMaterialsForAdmin();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(service, times(1)).getAllMaterialsForAdmin();
    }
}
