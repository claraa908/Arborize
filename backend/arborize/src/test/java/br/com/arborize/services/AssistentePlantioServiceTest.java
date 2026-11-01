package br.com.arborize.services;

import br.com.arborize.dtos.request.AssistentePlantioAnswerRequest;
import br.com.arborize.dtos.request.AssistentePlantioRequest;
import br.com.arborize.dtos.response.EspecieRecomendadaResponse;
import br.com.arborize.entities.Especie;
import br.com.arborize.exceptions.AssistentePlantioValidationException;
import br.com.arborize.repositories.EspecieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AssistentePlantioServiceTest {
    @Mock
    private EspecieRepository especieRepository;

    private AssistentePlantioService service;
    private List<Especie> species;

    @BeforeEach
    void setUp() {
        service = new AssistentePlantioService(especieRepository);
        species = List.of(
                species(1L, "Aroeira", "Nativa da Caatinga", "Pequeno"),
                species(2L, "Baraúna", "Nativa da Caatinga", "Médio"),
                species(3L, "Cedro", "Recomendada", "Grande"),
                species(4L, "Nim", "Exótica", "Pequeno"),
                species(5L, "Invadente", "Invasora", "Pequeno"),
                species(6L, "Frutífera fora da lista", "Frutífera", "Pequeno")
        );
        lenient().when(especieRepository.findAll()).thenReturn(species);
    }

    @Test
    void rejectsMissingOrUnknownAnswersWithFieldErrors() {
        AssistentePlantioRequest invalid = new AssistentePlantioRequest(List.of(
                new AssistentePlantioAnswerRequest("largura_calcada", "VALOR_INVALIDO")
        ));

        AssistentePlantioValidationException exception = assertThrows(
                AssistentePlantioValidationException.class,
                () -> service.recomendar(invalid)
        );

        assertEquals("Resposta ausente ou inválida.", exception.getErrors().get("largura_calcada"));
        assertEquals("Responda esta pergunta.", exception.getErrors().get("fiacao_eletrica"));
    }

    @Test
    void returnsNoSidewalkRecommendationsForNarrowSidewalk() {
        List<EspecieRecomendadaResponse> result = service.recomendar(request("MENOR_1_5M", "NAO", "CALCADA"));

        assertEquals(List.of(), result);
    }

    @Test
    void overheadWiringRestrictsRecommendationsToSmallTrees() {
        List<EspecieRecomendadaResponse> result = service.recomendar(request("MAIOR_2_4M", "SIM", "CALCADA"));

        assertEquals(List.of("Aroeira"), result.stream().map(EspecieRecomendadaResponse::nomePopular).toList());
    }

    @Test
    void sidewalkWidthAllowsOnlySizesDefinedForThatWidth() {
        List<EspecieRecomendadaResponse> result = service.recomendar(request("ENTRE_2M_E_2_4M", "NAO", "CALCADA"));

        assertEquals(List.of("Aroeira", "Baraúna"), result.stream().map(EspecieRecomendadaResponse::nomePopular).toList());
    }

    @Test
    void wideSidewalkAllowsLargeNativeRecommendedSpecies() {
        List<EspecieRecomendadaResponse> result = service.recomendar(request("MAIOR_2_4M", "NAO", "CALCADA"));

        assertEquals(List.of("Aroeira", "Baraúna", "Cedro"), result.stream().map(EspecieRecomendadaResponse::nomePopular).toList());
    }

    @Test
    void quintalDoesNotApplySidewalkWidthRulesAndExcludesExoticAndInvasiveCategories() {
        List<EspecieRecomendadaResponse> result = service.recomendar(request("MENOR_1_5M", "NAO", "QUINTAL"));

        assertEquals(List.of("Aroeira", "Baraúna", "Cedro"), result.stream().map(EspecieRecomendadaResponse::nomePopular).toList());
    }

    private AssistentePlantioRequest request(String width, String wiring, String location) {
        Map<String, String> values = Map.of(
                "largura_calcada", width,
                "fiacao_eletrica", wiring,
                "local_plantio", location,
                "recuo_construcao", "MAIOR_4M",
                "tipo_solo", "SECO_RASO",
                "objetivo", "SOMBRA",
                "disponibilidade_rega", "SIM",
                "piso_drenante", "SIM",
                "substituicao_nim", "NAO",
                "quantidade_mudas", "UMA"
        );
        List<AssistentePlantioAnswerRequest> answers = new ArrayList<>();
        values.forEach((question, value) -> answers.add(new AssistentePlantioAnswerRequest(question, value)));
        return new AssistentePlantioRequest(answers);
    }

    private Especie species(Long id, String popularName, String category, String size) {
        Especie species = new Especie();
        species.setId(id);
        species.setNomePopular(popularName);
        species.setNomeCientifico(popularName + "us exemplo");
        species.setCategoria(category);
        species.setPorteArvore(size);
        return species;
    }
}
