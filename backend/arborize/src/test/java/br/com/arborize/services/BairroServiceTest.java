package br.com.arborize.services;

import br.com.arborize.entities.Bairro;
import br.com.arborize.repositories.BairroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BairroServiceTest {

    @Test
    void retornaApenasCatalogoAtivoOrdenadoPeloRepositorio() {
        Bairro centro = bairro(2L, "Centro");
        Bairro planalto = bairro(4L, "Planalto");
        BairroRepository repository = mock(BairroRepository.class);
        when(repository.findAllByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(centro, planalto));

        var resultado = new BairroService(repository).listarDisponiveis();

        assertEquals(List.of("Centro", "Planalto"), resultado.stream().map(item -> item.nome()).toList());
        assertEquals(List.of(2L, 4L), resultado.stream().map(item -> item.id()).toList());
        verify(repository).findAllByAtivoTrueOrderByNomeAsc();
        verifyNoMoreInteractions(repository);
    }

    private Bairro bairro(Long id, String nome) {
        Bairro bairro = new Bairro();
        ReflectionTestUtils.setField(bairro, "id", id);
        bairro.setNome(nome);
        bairro.setAtivo(true);
        return bairro;
    }
}
