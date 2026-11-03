package br.com.arborize.validation;

import br.com.arborize.dtos.request.*;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.arborize.enums.ClassificacaoTerritorial.*;
import static br.com.arborize.enums.CondicaoFitossanitaria.NAO_AVALIADA;
import static org.junit.jupiter.api.Assertions.*;

class ArvoreRequestValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidadorSemSpring() {
        factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    private DadosArvoreRequest entrada(boolean atualizacao, Long especie, BigDecimal latitude, BigDecimal longitude,
                                      ClassificacaoTerritorial classificacao, Long bairro, BigDecimal dap,
                                      BigDecimal altura, CondicaoFitossanitaria condicao) {
        return atualizacao
                ? new ArvoreAtualizacaoRequestDTO(especie, latitude, longitude, classificacao, bairro,
                    null, null, dap, altura, condicao, null)
                : new ArvoreCadastroRequestDTO(especie, latitude, longitude, classificacao, bairro,
                    null, null, dap, altura, condicao, null);
    }

    private DadosArvoreRequest entradaComBairro(boolean atualizacao, ClassificacaoTerritorial classificacao,
                                               Long bairroId, String bairroNome, String cep) {
        return atualizacao
                ? new ArvoreAtualizacaoRequestDTO(1L, BigDecimal.ZERO, BigDecimal.ZERO, classificacao,
                    bairroId, bairroNome, cep, null, null, null, null, NAO_AVALIADA, null)
                : new ArvoreCadastroRequestDTO(1L, BigDecimal.ZERO, BigDecimal.ZERO, classificacao,
                    bairroId, bairroNome, cep, null, null, null, null, NAO_AVALIADA, null);
    }

    private DadosArvoreRequest entradaNova(boolean atualizacao, Boolean zonaRural, Long bairroId,
                                           String localidadeRural, ClassificacaoTerritorial classificacao) {
        return atualizacao
                ? new ArvoreAtualizacaoRequestDTO(1L, BigDecimal.ZERO, BigDecimal.ZERO, classificacao,
                    bairroId, null, null, null, null, null, null, NAO_AVALIADA, null,
                    zonaRural, localidadeRural)
                : new ArvoreCadastroRequestDTO(1L, BigDecimal.ZERO, BigDecimal.ZERO, classificacao,
                    bairroId, null, null, null, null, null, null, NAO_AVALIADA, null,
                    zonaRural, localidadeRural);
    }

    private Set<String> erros(DadosArvoreRequest entrada) {
        return validator.validate(entrada).stream()
                .map(erro -> erro.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void deveAceitarCadastroMinimoComDadosDesconhecidosENaoAvaliada() {
        var dto = entrada(false, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BAIRRO_NAO_IDENTIFICADO,
                null, null, null, null);
        assertTrue(erros(dto).isEmpty());
        assertEquals(NAO_AVALIADA, dto.condicaoFitossanitaria());
        assertNull(dto.dataPlantio());
        assertNull(dto.dapCm());
        assertNull(dto.alturaEstimadaM());
    }

    @Test
    void deveAceitarPutComCamposOpcionaisNulos() {
        var dto = entrada(true, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BAIRRO_NAO_IDENTIFICADO,
                null, null, null, NAO_AVALIADA);
        assertTrue(erros(dto).isEmpty());
        assertNull(dto.referenciaLocalizacao());
        assertNull(dto.observacoes());
    }

    @Test
    void deveExigirEspecieCoordenadasEClassificacaoEmAmbosContratos() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            assertEquals(Set.of("especieId", "latitude", "longitude", "classificacaoTerritorial"),
                    erros(entrada(atualizacao, null, null, null, null, null, null, null, NAO_AVALIADA)));
        }
    }

    @Test
    void deveAceitarLimitesGlobaisInclusiveNosDoisContratos() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (String latitude : new String[]{"-90", "90"}) {
                for (String longitude : new String[]{"-180", "180"}) {
                    assertTrue(erros(entrada(atualizacao, 1L, new BigDecimal(latitude), new BigDecimal(longitude),
                            BAIRRO_NAO_IDENTIFICADO, null, null, null, NAO_AVALIADA)).isEmpty());
                }
            }
        }
    }

    @Test
    void deveRejeitarLatitudeForaDosLimitesSemArredondamento() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (String latitude : new String[]{"-90.000000001", "90.000000001"}) {
                assertEquals(Set.of("latitude"), erros(entrada(atualizacao, 1L, new BigDecimal(latitude),
                        BigDecimal.ZERO, BAIRRO_NAO_IDENTIFICADO, null, null, null, NAO_AVALIADA)));
            }
        }
    }

    @Test
    void deveRejeitarLongitudeForaDosLimitesSemArredondamento() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (String longitude : new String[]{"-180.000000001", "180.000000001"}) {
                assertEquals(Set.of("longitude"), erros(entrada(atualizacao, 1L, BigDecimal.ZERO,
                        new BigDecimal(longitude), BAIRRO_NAO_IDENTIFICADO, null, null, null, NAO_AVALIADA)));
            }
        }
    }

    @Test
    void naoDeveInventarLimitesMunicipais() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            assertTrue(erros(entrada(atualizacao, 1L, new BigDecimal("80"), new BigDecimal("170"),
                    FORA_DA_AREA_URBANA, null, null, null, NAO_AVALIADA)).isEmpty());
        }
    }

    @Test
    void deveRejeitarIdentificadoresNaoPositivos() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (long id : new long[]{0, -1}) {
                assertEquals(Set.of("especieId", "bairroId"), erros(entrada(atualizacao, id,
                        BigDecimal.ZERO, BigDecimal.ZERO, BAIRRO_IDENTIFICADO, id, null, null, NAO_AVALIADA)));
            }
        }
    }

    @Test
    void deveRejeitarMedidasIguaisAZeroOuNegativas() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (String valor : new String[]{"0", "-0.00001", "-10"}) {
                assertEquals(Set.of("dapCm", "alturaEstimadaM"), erros(entrada(atualizacao, 1L,
                        BigDecimal.ZERO, BigDecimal.ZERO, BAIRRO_NAO_IDENTIFICADO, null,
                        new BigDecimal(valor), new BigDecimal(valor), NAO_AVALIADA)));
            }
        }
    }

    @Test
    void deveAceitarMedidasPositivasSemMaximoInstitucionalInventado() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            assertTrue(erros(entrada(atualizacao, 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                    BAIRRO_NAO_IDENTIFICADO, null, new BigDecimal("0.0000000000001"),
                    new BigDecimal("9999999999999999999999.123456"), NAO_AVALIADA)).isEmpty());
        }
    }

    @Test
    void deveExigirBairroSomenteQuandoIdentificado() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            assertEquals(Set.of("bairroNome"), erros(entrada(atualizacao, 1L, BigDecimal.ZERO,
                    BigDecimal.ZERO, BAIRRO_IDENTIFICADO, null, null, null, NAO_AVALIADA)));
            assertTrue(erros(entrada(atualizacao, 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                    BAIRRO_IDENTIFICADO, 2L, null, null, NAO_AVALIADA)).isEmpty());
        }
    }

    @Test
    void deveAceitarBairroTextualSemCatalogoENormalizarEspacos() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            var dto = entradaComBairro(atualizacao, BAIRRO_IDENTIFICADO, null,
                    "  Bairro confirmado  ", " 06390-000 ");
            assertTrue(erros(dto).isEmpty());
            assertEquals("Bairro confirmado", dto.bairroNome());
            assertEquals("06390-000", dto.cep());
        }
    }

    @Test
    void cepNaoExigeBairroNemAlteraSituacaoTerritorial() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (var situacao : new ClassificacaoTerritorial[]{BAIRRO_NAO_IDENTIFICADO, FORA_DA_AREA_URBANA}) {
                var dto = entradaComBairro(atualizacao, situacao, null, null, "63900-000");
                assertTrue(erros(dto).isEmpty());
                assertEquals(situacao, dto.classificacaoTerritorial());
            }
        }
    }

    @Test
    void rejeitaNomeTerritorialNasSituacoesSemBairroIdentificado() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (var situacao : new ClassificacaoTerritorial[]{BAIRRO_NAO_IDENTIFICADO, FORA_DA_AREA_URBANA}) {
                assertEquals(Set.of("bairroNome"), erros(entradaComBairro(atualizacao,
                        situacao, null, "Bairro informado", null)));
            }
        }
    }

    @Test
    void deveRejeitarBairroNasOutrasSituacoesEPermitirSuaAusencia() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (var classificacao : new ClassificacaoTerritorial[]{BAIRRO_NAO_IDENTIFICADO, FORA_DA_AREA_URBANA}) {
                assertEquals(Set.of("bairroId"), erros(entrada(atualizacao, 1L, BigDecimal.ZERO,
                        BigDecimal.ZERO, classificacao, 2L, null, null, NAO_AVALIADA)));
                assertTrue(erros(entrada(atualizacao, 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                        classificacao, null, null, null, NAO_AVALIADA)).isEmpty());
            }
        }
    }

    @Test
    void deveAceitarSomenteTipoEnumeradoEExigirCondicaoNoPut() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            for (var condicao : CondicaoFitossanitaria.values()) {
                assertTrue(erros(entrada(atualizacao, 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                        BAIRRO_NAO_IDENTIFICADO, null, null, null, condicao)).isEmpty());
            }
        }
        assertEquals(Set.of("condicaoFitossanitaria"), erros(entrada(true, 1L, BigDecimal.ZERO,
                BigDecimal.ZERO, BAIRRO_NAO_IDENTIFICADO, null, null, null, null)));
    }

    @Test
    void validaOsDoisModosNovosSemExigirClassificacaoLegadaNoRequest() {
        for (boolean atualizacao : new boolean[]{false, true}) {
            var urbano = entradaNova(atualizacao, false, 2L, null, null);
            assertTrue(erros(urbano).isEmpty());

            var urbanoSemBairro = entradaNova(atualizacao, false, null, null, null);
            assertEquals(Set.of("bairroId"), erros(urbanoSemBairro));

            var rural = entradaNova(atualizacao, true, null, "  Sítio Boa Vista  ", null);
            assertTrue(erros(rural).isEmpty());
            assertEquals("Sítio Boa Vista", rural.localidadeRural());

            assertEquals(Set.of("localidadeRural"), erros(entradaNova(atualizacao, true, null, "  ", null)));
            assertEquals(Set.of("bairroId"), erros(entradaNova(atualizacao, true, 2L, "Comunidade", null)));
            assertEquals(Set.of("classificacaoTerritorial"),
                    erros(entradaNova(atualizacao, true, null, "Comunidade", BAIRRO_IDENTIFICADO)));
        }
    }
}
