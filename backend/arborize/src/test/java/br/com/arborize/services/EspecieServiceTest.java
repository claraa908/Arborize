package br.com.arborize.services;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Especie;
import br.com.arborize.mappers.EspecieMapper;
import br.com.arborize.repositories.EspecieRepository;
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

    @Test
    void deveListarTodasAsEspecies() {
        Especie especie = new Especie();
        EspecieResponseDTO dto = new EspecieResponseDTO(1L, "Ipê Amarelo", null, null, null, null, null, null, null, new ArrayList<>());

        when(repository.findAll()).thenReturn(List.of(especie));
        when(mapper.toResponseDTO(especie)).thenReturn(dto);

        List<EspecieResponseDTO> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Ipê Amarelo", resultado.get(0).nomePopular());
    }

    @Test
    void deveCriarEspecieComFoto() {
        MockMultipartFile fotoFalsa = new MockMultipartFile("foto", "ipe.png", "image/png", "conteudo".getBytes());
        EspecieRequestDTO requestDTO = new EspecieRequestDTO("Ipê", "Handroanthus", null, null, null, null, null, null, fotoFalsa);

        Especie especieMapeada = new Especie();
        especieMapeada.setDocuments(new ArrayList<>());

        DocumentEntities documentoSalvo = new DocumentEntities();
        EspecieResponseDTO responseDTO = new EspecieResponseDTO(1L, "Ipê", null, null, null, null, null, null, null, new ArrayList<>());

        when(mapper.toEntity(requestDTO)).thenReturn(especieMapeada);
        when(documentService.uploadAndCreateDocument(fotoFalsa)).thenReturn(documentoSalvo);
        when(repository.save(any(Especie.class))).thenReturn(especieMapeada);
        when(mapper.toResponseDTO(especieMapeada)).thenReturn(responseDTO);

        EspecieResponseDTO resultado = service.criarEspecie(requestDTO);

        assertNotNull(resultado);
        verify(documentService, times(1)).uploadAndCreateDocument(fotoFalsa);
        verify(repository, times(1)).save(especieMapeada);
    }
}