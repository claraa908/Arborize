package br.com.arborize.services;

import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.EducationalMaterialEntities;
import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.mappers.EducationalMaterialMapper;
import br.com.arborize.repositories.EducationalMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationalMaterialServiceTest {

    @Mock
    private EducationalMaterialRepository repository;

    @Mock
    private EducationalMaterialMapper mapper;

    @InjectMocks
    private EducationalMaterialService service;

    private EducationalMaterialEntities materialPublicado;
    private EducationalMaterialEntities materialRascunho;

    @BeforeEach
    void setUp() {
        materialPublicado = new EducationalMaterialEntities();
        materialPublicado.setId(1L);
        materialPublicado.setTitle("Guia de Plantio");
        materialPublicado.setStatus("PUBLICADO");
        materialPublicado.setCategory(MaterialCategory.ARBORIZACAO_URBANA);

        materialRascunho = new EducationalMaterialEntities();
        materialRascunho.setId(2L);
        materialRascunho.setTitle("Rascunho Interno");
        materialRascunho.setStatus("RASCUNHO");
        materialRascunho.setCategory(MaterialCategory.CAATINGA);
    }

    @Test
    void deveRetornarApenasMateriaisPublicadosParaOPublico() {
        // Arrange
        when(repository.findByStatus("PUBLICADO")).thenReturn(Arrays.asList(materialPublicado));

        // Mock adaptado para o construtor do seu DTO (substitua os tipos conforme a assinatura real do seu projeto se necessário)
        EducationalMaterialResponseDTO dtoMock = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(dtoMock);

        // Act
        List<EducationalMaterialResponseDTO> resultado = service.getPublicMaterials(null, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(repository, times(1)).findByStatus("PUBLICADO");
    }

    @Test
    void deveRetornarTodosOsMateriaisParaOAdmin() {
        // Arrange
        when(repository.findAllByOrderByIdDesc()).thenReturn(Arrays.asList(materialPublicado, materialRascunho));

        EducationalMaterialResponseDTO dtoMock1 = mock(EducationalMaterialResponseDTO.class);
        EducationalMaterialResponseDTO dtoMock2 = mock(EducationalMaterialResponseDTO.class);

        when(mapper.toResponseDTO(materialPublicado)).thenReturn(dtoMock1);
        when(mapper.toResponseDTO(materialRascunho)).thenReturn(dtoMock2);

        // Act
        List<EducationalMaterialResponseDTO> resultado = service.getAllMaterialsForAdmin();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(repository, times(1)).findAllByOrderByIdDesc();
    }
}