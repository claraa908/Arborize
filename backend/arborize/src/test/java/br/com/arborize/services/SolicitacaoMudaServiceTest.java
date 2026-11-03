package br.com.arborize.services;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
import br.com.arborize.dtos.response.ConversaoReciclagemResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.enums.SolicitacaoMudaStatus;
import br.com.arborize.exceptions.SolicitacaoMudaException;
import br.com.arborize.mappers.SolicitacaoMudaMapper;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoMudaServiceTest {
    @Mock
    private EspecieRepository especieRepository;
    @Mock
    private SolicitacaoMudaRepository solicitacaoRepository;

    private SolicitacaoMudaService service;
    private Especie especie;

    @BeforeEach
    void setUp() {
        service = new SolicitacaoMudaService(especieRepository, solicitacaoRepository, new SolicitacaoMudaMapper(), 3, 5);
        especie = new Especie();
        especie.setId(8L);
        especie.setNomePopular("Ipê-amarelo");
        lenient().when(especieRepository.findById(8L)).thenReturn(Optional.of(especie));
    }

    @Test
    void conversionUsesConfiguredBottleRatio() {
        ConversaoReciclagemResponse response = service.calcularConversao(3);

        assertEquals(5, response.garrafasPorMuda());
        assertEquals(15, response.totalGarrafas());
    }

    @Test
    void quantityOutsideMaximumIsRejected() {
        SolicitacaoMudaException exception = assertThrows(
                SolicitacaoMudaException.class,
                () -> service.calcularConversao(4)
        );

        assertEquals("Informe uma quantidade entre 1 e 3.", exception.getErrors().get("quantidadeMudas"));
    }

    @Test
    void createLinksSpeciesAndReturnsProtocolWithoutPersonalIdentifiers() {
        when(solicitacaoRepository.save(any(SolicitacaoMuda.class))).thenAnswer(invocation -> {
            SolicitacaoMuda saved = invocation.getArgument(0);
            saved.setId(27L);
            return saved;
        });

        SolicitacaoMudaResponse response = service.criar(validRequest());

        assertEquals("SOL-" + response.criadaEm().getYear() + "-0027", response.protocolo());
        assertEquals("PENDENTE_RECICLAVEIS", response.status());
        assertEquals(10, response.totalRecicaveis());
        assertEquals(8L, response.especie().id());
        assertEquals("Ipê-amarelo", response.especie().nomePopular());
        assertEquals("Maria Silva", response.nomeSolicitante());
        verify(solicitacaoRepository).save(any(SolicitacaoMuda.class));
    }

    @Test
    void missingSpeciesDoesNotPersistRequest() {
        when(especieRepository.findById(99L)).thenReturn(Optional.empty());

        SolicitacaoMudaRequest request = new SolicitacaoMudaRequest(
                99L, 1, "Rua A", "1", "Centro", null, "Praça", "Maria Silva",
                "52998224725", "85999999999"
        );

        SolicitacaoMudaException exception = assertThrows(SolicitacaoMudaException.class, () -> service.criar(request));

        assertEquals("ESPECIE_NOT_FOUND", exception.getCode());
    }

    private SolicitacaoMudaRequest validRequest() {
        return new SolicitacaoMudaRequest(
                8L, 2, "  Rua   das Flores ", " 25 ", " Centro ", " ap 2 ", "Praça central",
                " Maria   Silva ", "52998224725", "85999999999"
        );
    }
}
