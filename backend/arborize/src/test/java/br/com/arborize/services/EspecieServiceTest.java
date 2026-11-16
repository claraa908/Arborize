package br.com.arborize.services;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Especie;
import br.com.arborize.mappers.EspecieMapper;
import br.com.arborize.repositories.EspecieRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspecieServiceTest {

    @InjectMocks
    private EspecieService service;

    @Mock
    private EspecieRepository repository;

    @Mock
    private EspecieMapper mapper;

    @Mock
    private DocumentService documentService;

    @Mock
    private MinioClient minioClient;

    @Test
    void deveListarTodasAsEspecies() {
        Especie especie = new Especie();
        EspecieResponseDTO dto = new EspecieResponseDTO(1L, "Ipê Amarelo", null, null, null, null, null, null, null, 0, new ArrayList<>());

        when(repository.findAll()).thenReturn(List.of(especie));
        when(mapper.toResponseDTO(especie)).thenReturn(dto);

        List<EspecieResponseDTO> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Ipê Amarelo", resultado.get(0).nomePopular());
    }

    @Test
    void deveCriarEspecieComFoto() {
        MockMultipartFile fotoFalsa = new MockMultipartFile("foto", "ipe.png", "image/png", "conteudo".getBytes());
        EspecieRequestDTO requestDTO = new EspecieRequestDTO("Ipê", "Handroanthus", null, null, null, null, null, null, null, fotoFalsa);

        Especie especieMapeada = new Especie();
        especieMapeada.setDocuments(new ArrayList<>());

        DocumentEntities documentoSalvo = new DocumentEntities();
        EspecieResponseDTO responseDTO = new EspecieResponseDTO(1L, "Ipê", null, null, null, null, null, null, null, 0, new ArrayList<>());

        when(mapper.toEntity(requestDTO)).thenReturn(especieMapeada);
        when(documentService.uploadAndCreateDocument(fotoFalsa)).thenReturn(documentoSalvo);
        when(repository.save(any(Especie.class))).thenReturn(especieMapeada);
        when(mapper.toResponseDTO(especieMapeada)).thenReturn(responseDTO);

        EspecieResponseDTO resultado = service.criarEspecie(requestDTO);

        assertNotNull(resultado);
        verify(documentService, times(1)).uploadAndCreateDocument(fotoFalsa);
        verify(repository, times(1)).save(especieMapeada);
    }

    @Test
    void deveCriarEspecieSemFotoSemInvocarArmazenamentoDeArquivos() {
        EspecieRequestDTO request = new EspecieRequestDTO(
                "Juazeiro", "Ziziphus joazeiro", null, "Nativa", "Médio", null,
                null, null, 0, null
        );
        Especie especie = new Especie();
        EspecieResponseDTO response = new EspecieResponseDTO(
                2L, "Juazeiro", "Ziziphus joazeiro", null, "Nativa", "Médio", null,
                null, null, 0, new ArrayList<>()
        );
        when(mapper.toEntity(request)).thenReturn(especie);
        when(repository.save(especie)).thenReturn(especie);
        when(mapper.toResponseDTO(especie)).thenReturn(response);

        assertEquals(response, service.criarEspecie(request));

        verify(documentService, never()).uploadAndCreateDocument(any());
        verify(repository).save(especie);
    }

    @Test
    void deveMapearEspecieEncontradaPorId() {
        Especie especie = new Especie();
        EspecieResponseDTO response = new EspecieResponseDTO(
                3L, "Juazeiro", "Ziziphus joazeiro", null, "Nativa", "Médio", null,
                null, true, 0, new ArrayList<>()
        );
        when(repository.findById(3L)).thenReturn(java.util.Optional.of(especie));
        when(mapper.toResponseDTO(especie)).thenReturn(response);

        assertEquals(response, service.buscarPorId(3L));

        verify(mapper).toResponseDTO(especie);
    }

    @Test
    void deveRejeitarBuscaDeEspecieInexistente() {
        when(repository.findById(404L)).thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.buscarPorId(404L));

        assertTrue(exception.getMessage().contains("404"));
        verify(mapper, never()).toResponseDTO(any(Especie.class));
    }

    @Test
    void deveRecuperarImagemPelaChaveDoDocumentoAssociado() {
        DocumentEntities imagem = new DocumentEntities();
        imagem.setFilePath("especies/ipe-teste");
        Especie especie = new Especie();
        especie.setDocuments(new ArrayList<>(List.of(imagem)));
        byte[] bytes = {4, 5, 6};
        when(repository.findById(8L)).thenReturn(java.util.Optional.of(especie));
        when(documentService.getFileBytes("especies/ipe-teste")).thenReturn(bytes);

        assertArrayEquals(bytes, service.obterBytesDaImagem(8L));

        verify(documentService).getFileBytes("especies/ipe-teste");
    }

    @Test
    void deveAtualizarDadosESubstituirImagemQuandoNovaFotoForEnviada() {
        MockMultipartFile novaFoto = new MockMultipartFile("foto", "nova.png", "image/png", new byte[]{7});
        EspecieRequestDTO request = new EspecieRequestDTO(
                "Ipê atualizado", "Handroanthus albus", null, "Nativa", "Médio", null,
                "20", "true", 12, novaFoto
        );
        DocumentEntities imagemAntiga = new DocumentEntities();
        DocumentEntities imagemNova = new DocumentEntities();
        Especie existente = new Especie();
        existente.setDocuments(new ArrayList<>(List.of(imagemAntiga)));
        EspecieResponseDTO response = new EspecieResponseDTO(
                11L, "Ipê atualizado", null, null, null, null, null, null, true, 12, new ArrayList<>()
        );
        when(repository.findById(11L)).thenReturn(java.util.Optional.of(existente));
        when(documentService.uploadAndCreateDocument(novaFoto)).thenReturn(imagemNova);
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDTO(existente)).thenReturn(response);

        assertEquals(response, service.atualizarEspecie(11L, request));

        verify(mapper).updateEntity(request, existente);
        assertEquals(List.of(imagemNova), existente.getDocuments());
        assertSame(existente, imagemNova.getEspecie());
        verify(repository).save(existente);
    }

    @Test
    void falhaAoRemoverImagemNoMinioNaoImpedeExclusaoDaEspecie() throws Exception {
        DocumentEntities imagem = new DocumentEntities();
        imagem.setFilePath("especies/imagem-com-falha");
        Especie existente = new Especie();
        existente.setDocuments(new ArrayList<>(List.of(imagem)));
        when(repository.findById(12L)).thenReturn(java.util.Optional.of(existente));
        doThrow(new RuntimeException("MinIO indisponível"))
                .when(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));

        service.deletarEspecie(12L);

        verify(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));
        verify(repository).delete(existente);
    }

    @Test
    void deveRejeitarLeituraQuandoEspecieNaoTemImagem() {
        Especie especieSemImagem = new Especie();
        especieSemImagem.setDocuments(new ArrayList<>());
        when(repository.findById(13L)).thenReturn(java.util.Optional.of(especieSemImagem));

        assertThrows(RuntimeException.class, () -> service.obterBytesDaImagem(13L));

        verifyNoInteractions(documentService);
    }
}
