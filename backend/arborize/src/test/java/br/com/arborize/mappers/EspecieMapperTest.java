package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.entities.Especie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EspecieMapperTest {

    private final EspecieMapper mapper = new EspecieMapper();

    @Test
    void novaEspecieIniciaComEstoqueZero() {
        Especie especie = mapper.toEntity(request("Ipê", "true"));

        assertEquals(0, especie.getMudasDisponiveis());
    }

    @Test
    void atualizacaoPreservaEstoqueExistente() {
        Especie existente = new Especie();
        existente.setMudasDisponiveis(42);

        Especie atualizada = mapper.updateEntity(request("Ipê atualizado", "false"), existente);

        assertEquals("Ipê atualizado", atualizada.getNomePopular());
        assertEquals(42, atualizada.getMudasDisponiveis());
    }

    private EspecieRequestDTO request(String nomePopular, String recomendadaCalcadas) {
        return new EspecieRequestDTO(
                nomePopular,
                "Handroanthus albus",
                null,
                "Nativa da Caatinga",
                "Médio",
                null,
                null,
                recomendadaCalcadas,
                null
        );
    }
}
