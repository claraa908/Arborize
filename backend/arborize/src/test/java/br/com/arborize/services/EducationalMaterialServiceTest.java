package br.com.arborize.services;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.EducationalMaterialEntities;
import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.enums.MaterialType;
import br.com.arborize.mappers.EducationalMaterialMapper;
import br.com.arborize.repositories.EducationalMaterialRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationalMaterialServiceTest {

    @Mock
    private EducationalMaterialRepository repository;

    @Mock
    private EducationalMaterialMapper mapper;

    @Mock
    private DocumentService documentService;

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private EducationalMaterialService service;

    private EducationalMaterialEntities materialPublicado;
    private EducationalMaterialEntities materialRascunho;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
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

    @Test
    void combinaBuscaECategoriaSomenteEntreMateriaisPublicados() {
        when(repository.findByStatusAndCategoryAndTitleContainingIgnoreCase(
                "PUBLICADO", MaterialCategory.ARBORIZACAO_URBANA, "Caatinga"
        )).thenReturn(List.of(materialPublicado));
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        List<EducationalMaterialResponseDTO> result = service.getPublicMaterials(
                "Caatinga", MaterialCategory.ARBORIZACAO_URBANA
        );

        assertEquals(List.of(response), result);
        verify(repository).findByStatusAndCategoryAndTitleContainingIgnoreCase(
                "PUBLICADO", MaterialCategory.ARBORIZACAO_URBANA, "Caatinga"
        );
        verify(repository, never()).findAllByOrderByIdDesc();
    }

    @Test
    void atualizaStatusPermitidoERecusaValorDesconhecido() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        when(repository.save(materialPublicado)).thenReturn(materialPublicado);
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        assertEquals(response, service.updateStatus(1L, "RASCUNHO"));
        assertEquals("RASCUNHO", materialPublicado.getStatus());
        verify(repository).save(materialPublicado);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(1L, "ARQUIVADO"));
        verify(repository, times(1)).save(materialPublicado);
    }

    @Test
    void criaMaterialVinculandoPdfECapaAoMaterialSalvo() {
        MockMultipartFile cover = new MockMultipartFile("coverPhoto", "capa.png", "image/png", new byte[]{1});
        MockMultipartFile pdf = new MockMultipartFile("file", "guia.pdf", "application/pdf", new byte[]{2});
        EducationalMaterialRequestDTO request = new EducationalMaterialRequestDTO(
                "Guia novo", MaterialType.GUIA, MaterialCategory.CAATINGA, "Descrição", "RASCUNHO", cover, pdf
        );
        EducationalMaterialEntities entity = new EducationalMaterialEntities();
        DocumentEntities pdfDocument = new DocumentEntities();
        DocumentEntities coverDocument = new DocumentEntities();
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(documentService.uploadAndCreateDocument(pdf)).thenReturn(pdfDocument);
        when(documentService.uploadAndCreateDocument(cover)).thenReturn(coverDocument);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(response);

        assertEquals(response, service.createMaterial(request));

        assertEquals(List.of(pdfDocument, coverDocument), entity.getDocuments());
        assertSame(entity, pdfDocument.getEducationalMaterial());
        assertSame(entity, coverDocument.getEducationalMaterial());
        verify(repository).save(entity);
    }

    @Test
    void selecionaConsultaPublicaPorTextoQuandoCategoriaNaoFoiInformada() {
        when(repository.findByStatusAndTitleContainingIgnoreCase("PUBLICADO", "Caatinga"))
                .thenReturn(List.of(materialPublicado));
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        assertEquals(List.of(response), service.getPublicMaterials("Caatinga", null));

        verify(repository).findByStatusAndTitleContainingIgnoreCase("PUBLICADO", "Caatinga");
        verify(repository, never()).findByStatusAndCategory(any(), any());
    }

    @Test
    void selecionaConsultaPublicaPorCategoriaQuandoTextoEstaEmBranco() {
        when(repository.findByStatusAndCategory("PUBLICADO", MaterialCategory.CAATINGA))
                .thenReturn(List.of(materialPublicado));
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        assertEquals(List.of(response), service.getPublicMaterials("  ", MaterialCategory.CAATINGA));

        verify(repository).findByStatusAndCategory("PUBLICADO", MaterialCategory.CAATINGA);
        verify(repository, never()).findByStatusAndCategoryAndTitleContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void atualizaCamposDoMaterialSemAlterarDocumentos() {
        MockMultipartFile cover = new MockMultipartFile("coverPhoto", "capa.png", "image/png", new byte[]{3});
        MockMultipartFile pdf = new MockMultipartFile("file", "manual.pdf", "application/pdf", new byte[]{4});
        EducationalMaterialRequestDTO request = new EducationalMaterialRequestDTO(
                "Manual atualizado", MaterialType.MANUAL, MaterialCategory.LEGISLACAO,
                "Texto atualizado", "PUBLICADO", cover, pdf
        );
        DocumentEntities existingDocument = new DocumentEntities();
        materialPublicado.setDocuments(new ArrayList<>(List.of(existingDocument)));
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        when(repository.save(materialPublicado)).thenReturn(materialPublicado);
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        assertEquals(response, service.updateMaterial(1L, request));

        assertEquals("Manual atualizado", materialPublicado.getTitle());
        assertEquals(MaterialType.MANUAL, materialPublicado.getType());
        assertEquals(MaterialCategory.LEGISLACAO, materialPublicado.getCategory());
        assertEquals("Texto atualizado", materialPublicado.getDescription());
        assertEquals("PUBLICADO", materialPublicado.getStatus());
        assertEquals(List.of(existingDocument), materialPublicado.getDocuments());
        verify(documentService, never()).uploadAndCreateDocument(any());
    }

    @Test
    void baixaDocumentoDoMaterialPeloBucketEChaveAssociados() throws Exception {
        DocumentEntities document = new DocumentEntities();
        document.setFilePath("materiais/guia.pdf");
        materialPublicado.setDocuments(List.of(document));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        GetObjectResponse stream = mock(GetObjectResponse.class);
        when(stream.readAllBytes()).thenReturn(new byte[]{5, 6, 7});
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(stream);

        assertArrayEquals(new byte[]{5, 6, 7}, service.getFileStream(1L));

        ArgumentCaptor<GetObjectArgs> args = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(args.capture());
        assertEquals("test-bucket", args.getValue().bucket());
        assertEquals("materiais/guia.pdf", args.getValue().object());
    }

    @Test
    void baixaCapaDaImagemAssociadaMesmoQuandoPdfVemPrimeiro() throws Exception {
        DocumentEntities pdf = new DocumentEntities();
        pdf.setFilePath("materiais/guia.pdf");
        pdf.setContentType("application/pdf");
        DocumentEntities cover = new DocumentEntities();
        cover.setFilePath("materiais/capa.png");
        cover.setContentType("image/png");
        materialPublicado.setDocuments(List.of(pdf, cover));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        GetObjectResponse stream = mock(GetObjectResponse.class);
        when(stream.readAllBytes()).thenReturn(new byte[]{8, 9});
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(stream);

        assertArrayEquals(new byte[]{8, 9}, service.getCoverStream(1L));

        ArgumentCaptor<GetObjectArgs> args = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(args.capture());
        assertEquals("materiais/capa.png", args.getValue().object());
    }

    @Test
    void rejeitaDownloadQuandoMaterialNaoTemArquivoOuCapa() {
        materialPublicado.setDocuments(List.of());
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));

        assertThrows(RuntimeException.class, () -> service.getFileStream(1L));
        assertThrows(RuntimeException.class, () -> service.getCoverStream(1L));

        verifyNoInteractions(minioClient);
    }

    @Test
    void buscaMaterialPorIdERejeitaIdInexistente() {
        EducationalMaterialResponseDTO response = mock(EducationalMaterialResponseDTO.class);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        when(repository.findById(404L)).thenReturn(java.util.Optional.empty());
        when(mapper.toResponseDTO(materialPublicado)).thenReturn(response);

        assertEquals(response, service.getMaterialById(1L));
        assertThrows(RuntimeException.class, () -> service.getMaterialById(404L));

        verify(mapper).toResponseDTO(materialPublicado);
    }

    @Test
    void exclusaoDoMaterialContinuaMesmoQuandoMinioFalhaAoRemoverArquivo() throws Exception {
        DocumentEntities document = new DocumentEntities();
        document.setFilePath("materiais/arquivo-com-falha");
        materialPublicado.setDocuments(List.of(document));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        doThrow(new IOException("MinIO indisponível"))
                .when(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));

        service.deleteMaterial(1L);

        verify(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));
        verify(repository).delete(materialPublicado);
    }

    @Test
    void informaFalhaAoRecuperarPdfDoMinio() throws Exception {
        DocumentEntities document = new DocumentEntities();
        document.setFilePath("materiais/guia-indisponivel.pdf");
        materialPublicado.setDocuments(List.of(document));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new IOException("conexão encerrada"));

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.getFileStream(1L));

        assertTrue(error.getMessage().contains("Erro ao recuperar arquivo"));
        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    void usaSegundoDocumentoComoCapaQuandoNenhumTemMimeDeImagem() throws Exception {
        DocumentEntities first = new DocumentEntities();
        first.setFilePath("materiais/arquivo-1");
        first.setContentType("application/pdf");
        DocumentEntities second = new DocumentEntities();
        second.setFilePath("materiais/arquivo-2");
        second.setContentType("application/octet-stream");
        materialPublicado.setDocuments(List.of(first, second));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(materialPublicado));
        GetObjectResponse stream = mock(GetObjectResponse.class);
        when(stream.readAllBytes()).thenReturn(new byte[]{10});
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(stream);

        assertArrayEquals(new byte[]{10}, service.getCoverStream(1L));

        ArgumentCaptor<GetObjectArgs> args = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(args.capture());
        assertEquals("materiais/arquivo-2", args.getValue().object());
    }

    @Test
    void buscaPorTextoEmBrancoSemCategoriaUsaListagemDePublicados() {
        when(repository.findByStatus("PUBLICADO")).thenReturn(List.of());

        assertTrue(service.getPublicMaterials("  ", null).isEmpty());

        verify(repository).findByStatus("PUBLICADO");
        verify(repository, never()).findByStatusAndTitleContainingIgnoreCase(any(), any());
    }
}
