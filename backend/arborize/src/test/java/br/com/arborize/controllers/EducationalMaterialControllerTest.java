package br.com.arborize.controllers;

import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.services.EducationalMaterialService;
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