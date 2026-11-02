package br.com.arborize.mappers;

import br.com.arborize.dtos.request.*;
import br.com.arborize.dtos.response.*;
import br.com.arborize.entities.*;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.arborize.enums.ClassificacaoTerritorial.*;
import static br.com.arborize.enums.CondicaoFitossanitaria.*;
import static org.junit.jupiter.api.Assertions.*;

class ArvoreMapperTest {
    private final ArvoreMapper mapper = new ArvoreMapper();
    private final LocalDateTime criacao = LocalDateTime.of(2026, 1, 2, 8, 30);
    private final LocalDateTime atualizacao = criacao.plusDays(1);

    private static void definirCampo(Object entidade, String nome, Object valor) throws Exception {
        var campo = entidade.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(entidade, valor);
    }

    private Especie especie(long id) {
        Especie especie = new Especie();
        especie.setId(id);
        especie.setNomePopular("Nome popular de teste");
        especie.setNomeCientifico("Nome científico de teste");
        especie.setFamiliaBotanica("Família de teste");
        return especie;
    }

    private Bairro bairro(long id) throws Exception {
        Bairro bairro = new Bairro();
        definirCampo(bairro, "id", id);
        bairro.setNome("Nome apenas para teste em memória");
        return bairro;
    }

    private ArvoreCadastroRequestDTO cadastro(ClassificacaoTerritorial classificacao, Long bairro) {
        return new ArvoreCadastroRequestDTO(7L, new BigDecimal("-4.970123456789"),
                new BigDecimal("-39.010987654321"), classificacao, bairro, "Referência de teste",
                LocalDate.of(2020, 5, 6), new BigDecimal("31.12345"), new BigDecimal("7.12345"),
                SINAIS_COMPROMETIMENTO, "Observação de teste");
    }

    private Arvore arvoreCompleta() throws Exception {
        Arvore arvore = mapper.toEntity(cadastro(BAIRRO_IDENTIFICADO, 3L), especie(7L), bairro(3L));
        definirCampo(arvore, "id", 42L);
        definirCampo(arvore, "criadoEm", criacao);
        definirCampo(arvore, "atualizadoEm", atualizacao);
        return arvore;
    }

    private ArvoreAtualizacaoRequestDTO atualizacaoMinima() {
        return new ArvoreAtualizacaoRequestDTO(8L, new BigDecimal("10"), new BigDecimal("20"),
                FORA_DA_AREA_URBANA, null, null, null, null, null, MORTA, null);
    }

    @Test
    void deveMapearCadastroCompletoSemGerarIdOuMetadados() throws Exception {
        var dto = cadastro(BAIRRO_IDENTIFICADO, 3L);
        var especie = especie(7L);
        var bairro = bairro(3L);
        var arvore = mapper.toEntity(dto, especie, bairro);

        assertSame(especie, arvore.getEspecie());
        assertSame(bairro, arvore.getBairro());
        assertEquals(dto.latitude(), arvore.getLatitude());
        assertEquals(dto.longitude(), arvore.getLongitude());
        assertEquals(dto.classificacaoTerritorial(), arvore.getClassificacaoTerritorial());
        assertEquals(dto.referenciaLocalizacao(), arvore.getReferenciaLocalizacao());
        assertEquals(dto.dataPlantio(), arvore.getDataPlantio());
        assertEquals(dto.dapCm(), arvore.getDapCm());
        assertEquals(dto.alturaEstimadaM(), arvore.getAlturaEstimadaM());
        assertEquals(dto.condicaoFitossanitaria(), arvore.getCondicaoFitossanitaria());
        assertEquals(dto.observacoes(), arvore.getObservacoes());
        assertNull(arvore.getId());
        assertNull(arvore.getCriadoEm());
        assertNull(arvore.getAtualizadoEm());
    }

    @Test
    void deveManterOpcionaisNulosECondicaoInicialNoCadastro() {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, null, null);
        var arvore = mapper.toEntity(dto, especie(7L), null);

        assertNull(arvore.getBairro());
        assertNull(arvore.getReferenciaLocalizacao());
        assertNull(arvore.getDataPlantio());
        assertNull(arvore.getDapCm());
        assertNull(arvore.getAlturaEstimadaM());
        assertNull(arvore.getObservacoes());
        assertEquals(NAO_AVALIADA, arvore.getCondicaoFitossanitaria());
    }

    @Test
    void resumoIncluiMetadadosDaCapaSemFotoOuConteudo() throws Exception {
        var arvore = arvoreCompleta();
        var semFoto = mapper.toResumoDTO(arvore);
        var capa = new FotoCapaResponseDTO(8L, "image/webp");
        var comFoto = mapper.toResumoDTO(arvore, capa);

        assertNull(semFoto.fotoCapa());
        assertEquals(capa, comFoto.fotoCapa());
        assertEquals(42L, comFoto.id());
        assertEquals("ARV-QXD-000042", comFoto.codigo());
        assertEquals("Nome apenas para teste em memória", comFoto.bairroNome());
    }

    @Test
    void mapeiaBairroTextualCepEFallbackLegadoNasRespostas() throws Exception {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                BAIRRO_IDENTIFICADO, null, "  Centro  ", " 06390-000 ", null,
                null, null, null, NAO_AVALIADA, null);
        var arvore = mapper.toEntity(dto, especie(7L), null);
        definirCampo(arvore, "id", 43L);

        assertNull(arvore.getBairro());
        assertEquals("Centro", arvore.getBairroNome());
        assertEquals("06390-000", arvore.getCep());
        assertEquals("Centro", mapper.toResponseDTO(arvore).bairroNome());
        assertEquals("06390-000", mapper.toResponseDTO(arvore).cep());
        assertEquals("Centro", mapper.toResumoDTO(arvore).bairroNome());

        var legado = arvoreCompleta();
        legado.setBairroNome(null);
        assertEquals(legado.getBairro().getNome(), mapper.toResponseDTO(legado).bairroNome());
        assertEquals(legado.getBairro().getNome(), mapper.toResumoDTO(legado).bairroNome());
    }

    @Test
    void deveLimparOpcionaisNoPutEPreservarIdEMetadados() throws Exception {
        var arvore = arvoreCompleta();
        var especie = especie(8L);
        var codigoAntesDaAtualizacao = mapper.toResponseDTO(arvore).codigo();

        mapper.updateEntity(arvore, atualizacaoMinima(), especie, null);

        assertEquals(42L, arvore.getId());
        assertEquals(codigoAntesDaAtualizacao, mapper.toResponseDTO(arvore).codigo());
        assertEquals(criacao, arvore.getCriadoEm());
        assertEquals(atualizacao, arvore.getAtualizadoEm());
        assertSame(especie, arvore.getEspecie());
        assertNull(arvore.getBairro());
        assertNull(arvore.getReferenciaLocalizacao());
        assertNull(arvore.getDataPlantio());
        assertNull(arvore.getDapCm());
        assertNull(arvore.getAlturaEstimadaM());
        assertNull(arvore.getObservacoes());
        assertEquals(MORTA, arvore.getCondicaoFitossanitaria());
        assertEquals(FORA_DA_AREA_URBANA, arvore.getClassificacaoTerritorial());
    }

    @Test
    void deveReenviarDadosNaoAlteradosNoPutCompleto() throws Exception {
        var arvore = arvoreCompleta();
        var dto = new ArvoreAtualizacaoRequestDTO(7L, arvore.getLatitude(), arvore.getLongitude(),
                arvore.getClassificacaoTerritorial(), 3L, arvore.getReferenciaLocalizacao(),
                arvore.getDataPlantio(), arvore.getDapCm(), arvore.getAlturaEstimadaM(),
                APARENTEMENTE_SAUDAVEL, arvore.getObservacoes());
        var antes = mapper.toResponseDTO(arvore);

        mapper.updateEntity(arvore, dto, arvore.getEspecie(), arvore.getBairro());

        assertEquals(antes.latitude(), arvore.getLatitude());
        assertEquals(antes.longitude(), arvore.getLongitude());
        assertEquals(antes.referenciaLocalizacao(), arvore.getReferenciaLocalizacao());
        assertEquals(antes.dataPlantio(), arvore.getDataPlantio());
        assertEquals(antes.dapCm(), arvore.getDapCm());
        assertEquals(antes.alturaEstimadaM(), arvore.getAlturaEstimadaM());
        assertEquals(antes.observacoes(), arvore.getObservacoes());
        assertEquals(APARENTEMENTE_SAUDAVEL, arvore.getCondicaoFitossanitaria());
    }

    @Test
    void deveRejeitarEspecieCarregadaDivergenteAntesDeAlterarEntidade() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(cadastro(BAIRRO_NAO_IDENTIFICADO, null), especie(8L), null));
        var arvore = arvoreCompleta();
        var antes = mapper.toResponseDTO(arvore);

        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateEntity(arvore, atualizacaoMinima(), especie(7L), null));
        assertEquals(antes, mapper.toResponseDTO(arvore));
    }

    @Test
    void deveRejeitarBairroNaoCarregadoOuDivergenteSemPersistencia() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(cadastro(BAIRRO_IDENTIFICADO, 3L), especie(7L), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(cadastro(BAIRRO_IDENTIFICADO, 3L), especie(7L), bairro(4L)));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(cadastro(BAIRRO_NAO_IDENTIFICADO, null), especie(7L), bairro(3L)));
        var arvore = arvoreCompleta();
        var antes = mapper.toResponseDTO(arvore);
        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateEntity(arvore, atualizacaoMinima(), especie(8L), bairro(3L)));
        assertEquals(antes, mapper.toResponseDTO(arvore));
    }

    @Test
    void deveConverterFichaEmRecordsSemExporEntidadesOuDocumentos() throws Exception {
        var arvore = arvoreCompleta();
        var dto = mapper.toResponseDTO(arvore);

        assertEquals(42L, dto.id());
        assertEquals("ARV-QXD-000042", dto.codigo());
        assertEquals(7L, dto.especie().id());
        assertEquals(arvore.getEspecie().getNomePopular(), dto.especie().nomePopular());
        assertEquals(arvore.getEspecie().getNomeCientifico(), dto.especie().nomeCientifico());
        assertEquals(arvore.getEspecie().getFamiliaBotanica(), dto.especie().familiaBotanica());
        assertEquals(3L, dto.bairro().id());
        assertEquals(arvore.getBairro().getNome(), dto.bairro().nome());
        assertEquals(arvore.getBairro().getAtivo(), dto.bairro().ativo());
        assertEquals(arvore.getLatitude(), dto.latitude());
        assertEquals(arvore.getLongitude(), dto.longitude());
        assertEquals(arvore.getClassificacaoTerritorial(), dto.classificacaoTerritorial());
        assertEquals(arvore.getReferenciaLocalizacao(), dto.referenciaLocalizacao());
        assertEquals(arvore.getDataPlantio(), dto.dataPlantio());
        assertEquals(arvore.getDapCm(), dto.dapCm());
        assertEquals(arvore.getAlturaEstimadaM(), dto.alturaEstimadaM());
        assertEquals(arvore.getCondicaoFitossanitaria(), dto.condicaoFitossanitaria());
        assertEquals(arvore.getObservacoes(), dto.observacoes());
        assertEquals(criacao, dto.criadoEm());
        assertEquals(atualizacao, dto.atualizadoEm());
        verificarSemEntidades(ArvoreResponseDTO.class);
    }

    @Test
    void deveUsarMesmoResumoParaListagemEMapaSemDadosExtensos() throws Exception {
        var arvore = arvoreCompleta();
        var dto = mapper.toResumoDTO(arvore);
        var detalhes = mapper.toResponseDTO(arvore);

        assertEquals(detalhes.id(), dto.id());
        assertEquals(detalhes.codigo(), dto.codigo());
        assertEquals(detalhes.especie(), dto.especie());
        assertEquals(detalhes.bairro(), dto.bairro());
        assertEquals(detalhes.latitude(), dto.latitude());
        assertEquals(detalhes.longitude(), dto.longitude());
        assertEquals(detalhes.referenciaLocalizacao(), dto.referenciaLocalizacao());
        assertEquals(detalhes.condicaoFitossanitaria(), dto.condicaoFitossanitaria());
        assertEquals(detalhes.classificacaoTerritorial(), dto.classificacaoTerritorial());
        assertFalse(componentes(ArvoreResumoResponseDTO.class).contains("observacoes"));
        assertFalse(componentes(ArvoreResumoResponseDTO.class).contains("dataPlantio"));
        verificarSemEntidades(ArvoreResumoResponseDTO.class);
    }

    @Test
    void deveManterAusenciasNosDtosDeRespostaSemValoresArtificiais() throws Exception {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, null, null);
        var arvore = mapper.toEntity(dto, especie(7L), null);
        definirCampo(arvore, "id", 1L);
        var resposta = mapper.toResponseDTO(arvore);

        assertNull(resposta.bairro());
        assertNull(resposta.referenciaLocalizacao());
        assertNull(resposta.dataPlantio());
        assertNull(resposta.dapCm());
        assertNull(resposta.alturaEstimadaM());
        assertNull(resposta.observacoes());
        assertNull(mapper.toResumoDTO(arvore).bairro());
    }

    @Test
    void contratosDeEntradaNaoDevemReceberIdMetadadosOuArquivos() {
        for (var tipo : new Class<?>[]{ArvoreCadastroRequestDTO.class, ArvoreAtualizacaoRequestDTO.class}) {
            assertTrue(tipo.isRecord());
            for (String protegido : Set.of("id", "criadoEm", "atualizadoEm", "fotografias", "documents")) {
                assertFalse(componentes(tipo).contains(protegido));
            }
        }
    }

    private Set<String> componentes(Class<?> tipo) {
        return Arrays.stream(tipo.getRecordComponents()).map(campo -> campo.getName()).collect(Collectors.toSet());
    }

    private void verificarSemEntidades(Class<?> tipo) {
        for (var campo : tipo.getRecordComponents()) {
            assertNull(campo.getType().getAnnotation(Entity.class));
            if (campo.getType().isRecord()) {
                verificarSemEntidades(campo.getType());
            }
        }
    }
}
