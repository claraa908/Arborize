package br.com.arborize.services;

import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.dtos.response.ArvoreResponseDTO;
import br.com.arborize.entities.Arvore;
import br.com.arborize.entities.Bairro;
import br.com.arborize.entities.Especie;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.mappers.ArvoreMapper;
import br.com.arborize.repositories.ArvoreRepository;
import br.com.arborize.repositories.BairroRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.FotoArvoreRepository;
import jakarta.persistence.EntityNotFoundException;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.arborize.enums.ClassificacaoTerritorial.*;
import static br.com.arborize.enums.CondicaoFitossanitaria.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArvoreServiceTest {
    private static ValidatorFactory factory;
    private static Validator validator;
    private ArvoreRepository repository;
    private EspecieRepository especieRepository;
    private BairroRepository bairroRepository;
    private FotoArvoreRepository fotoRepository;
    private ArvoreService service;
    private final ArvoreMapper mapper = new ArvoreMapper();
    private Especie especie;
    private final LocalDateTime criacao = LocalDateTime.of(2026, 1, 2, 8, 30);

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

    @BeforeEach
    void preparar() throws Exception {
        // Os repositories são interfaces; mocks por proxy dispensam instrumentação da JVM.
        repository = mock(ArvoreRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        especieRepository = mock(EspecieRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        bairroRepository = mock(BairroRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        fotoRepository = mock(FotoArvoreRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        service = new ArvoreService(repository, especieRepository, bairroRepository, fotoRepository, mapper, validator);
        especie = new Especie();
        especie.setId(7L);
        especie.setNomePopular("Espécie apenas para teste em memória");
        especie.setNomeCientifico("Nome científico de teste");
        especie.setFamiliaBotanica("Família de teste");
        when(especieRepository.findById(7L)).thenReturn(Optional.of(especie));
        when(repository.save(any(Arvore.class))).thenAnswer(invocacao -> {
            Arvore arvore = invocacao.getArgument(0);
            // Simula o retorno da persistência; não exercita geração de ID em banco.
            definirCampo(arvore, "id", 42L);
            definirCampo(arvore, "criadoEm", criacao);
            definirCampo(arvore, "atualizadoEm", criacao);
            return arvore;
        });
    }

    private static void definirCampo(Object objeto, String nome, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(objeto, valor);
    }

    private Bairro bairro(Boolean ativo) throws Exception {
        Bairro bairro = new Bairro();
        definirCampo(bairro, "id", 3L);
        bairro.setNome("Bairro apenas para teste em memória");
        bairro.setAtivo(ativo);
        return bairro;
    }

    private ArvoreCadastroRequestDTO cadastro(ClassificacaoTerritorial classificacao, Long bairroId) {
        return new ArvoreCadastroRequestDTO(7L, new BigDecimal("-4.970123456789"),
                new BigDecimal("-39.010987654321"), classificacao, bairroId,
                "Referência de teste", LocalDate.of(2020, 5, 6), new BigDecimal("31.12345"),
                new BigDecimal("7.12345"), APARENTEMENTE_SAUDAVEL, "Observação de teste");
    }

    private ArvoreCadastroRequestDTO minimo(ClassificacaoTerritorial classificacao) {
        return new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO, classificacao,
                null, null, null, null, null, null, null);
    }

    private Arvore arvorePersistida(Bairro bairro) throws Exception {
        Arvore arvore = mapper.toEntity(cadastro(bairro == null ? BAIRRO_NAO_IDENTIFICADO : BAIRRO_IDENTIFICADO,
                bairro == null ? null : bairro.getId()), especie, bairro);
        definirCampo(arvore, "id", 42L);
        definirCampo(arvore, "criadoEm", criacao);
        definirCampo(arvore, "atualizadoEm", criacao.plusDays(1));
        return arvore;
    }

    @Test
    void deveCadastrarComEspecieEBairroExistentesEDisponiveis() throws Exception {
        Bairro bairro = bairro(true);
        when(bairroRepository.findById(3L)).thenReturn(Optional.of(bairro));
        var dto = cadastro(BAIRRO_IDENTIFICADO, 3L);

        var resposta = service.criarArvore(dto);

        assertEquals(42L, resposta.id());
        assertEquals(7L, resposta.especie().id());
        assertEquals(3L, resposta.bairro().id());
        assertTrue(resposta.bairro().ativo());
        assertEquals(dto.latitude(), resposta.latitude());
        assertEquals(dto.longitude(), resposta.longitude());
        assertEquals(dto.dataPlantio(), resposta.dataPlantio());
        assertEquals(dto.dapCm(), resposta.dapCm());
        assertEquals(dto.alturaEstimadaM(), resposta.alturaEstimadaM());
        assertEquals(dto.referenciaLocalizacao(), resposta.referenciaLocalizacao());
        assertEquals(dto.observacoes(), resposta.observacoes());
        assertEquals(criacao, resposta.criadoEm());
        verify(especieRepository).findById(7L);
        verify(bairroRepository).findById(3L);
        var captor = org.mockito.ArgumentCaptor.forClass(Arvore.class);
        verify(repository).save(captor.capture());
        assertSame(especie, captor.getValue().getEspecie());
        assertSame(bairro, captor.getValue().getBairro());
        verifyNoMoreInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void deveRejeitarEspecieInexistenteSemGravar() {
        when(especieRepository.findById(7L)).thenReturn(Optional.empty());

        var erro = assertThrows(EntityNotFoundException.class,
                () -> service.criarArvore(cadastro(BAIRRO_IDENTIFICADO, 3L)));

        assertTrue(erro.getMessage().contains("Espécie"));
        verifyNoInteractions(repository, bairroRepository);
        verify(especieRepository).findById(7L);
    }

    @Test
    void deveCadastrarSemBairroIdentificadoSemConsultarCatalogoTerritorial() {
        var resposta = service.criarArvore(cadastro(BAIRRO_NAO_IDENTIFICADO, null));

        assertEquals(BAIRRO_NAO_IDENTIFICADO, resposta.classificacaoTerritorial());
        assertNull(resposta.bairro());
        verifyNoInteractions(bairroRepository);
        verify(repository).save(any(Arvore.class));
    }

    @Test
    void deveCadastrarComBairroTextualECepSemConsultarCatalogo() {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                BAIRRO_IDENTIFICADO, null, "Bairro confirmado", "06390-000", null,
                null, null, null, NAO_AVALIADA, null);

        var resposta = service.criarArvore(dto);

        assertNull(resposta.bairro());
        assertEquals("Bairro confirmado", resposta.bairroNome());
        assertEquals("06390-000", resposta.cep());
        var captor = org.mockito.ArgumentCaptor.forClass(Arvore.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getBairro());
        assertEquals("Bairro confirmado", captor.getValue().getBairroNome());
        assertEquals("06390-000", captor.getValue().getCep());
        verifyNoInteractions(bairroRepository);
    }

    @Test
    void putTextualPreservaVinculoLegadoQuandoOClienteNaoEnviaBairroId() throws Exception {
        Bairro legado = bairro(true);
        Arvore existente = arvorePersistida(legado);
        existente.setCep("06390-000");
        when(repository.findById(42L)).thenReturn(Optional.of(existente));
        var dto = new br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO(
                7L, BigDecimal.ZERO, BigDecimal.ZERO, BAIRRO_IDENTIFICADO, null,
                "Bairro confirmado pelo operador", null, null, null, null, null,
                NAO_AVALIADA, null);

        var resposta = service.atualizarArvore(42L, dto);

        assertEquals(3L, resposta.bairro().id());
        assertEquals("Bairro confirmado pelo operador", resposta.bairroNome());
        assertNull(resposta.cep());
        assertSame(legado, existente.getBairro());
        assertEquals("Bairro confirmado pelo operador", existente.getBairroNome());
        verify(repository).findById(42L);
        verify(repository).save(existente);
        verify(repository).flush();
        verifyNoInteractions(bairroRepository);
    }

    @Test
    void deveCadastrarForaDaAreaUrbanaSemConsultarBairros() {
        var resposta = service.criarArvore(cadastro(FORA_DA_AREA_URBANA, null));

        assertEquals(FORA_DA_AREA_URBANA, resposta.classificacaoTerritorial());
        assertNull(resposta.bairro());
        verifyNoInteractions(bairroRepository);
    }

    @Test
    void deveRejeitarBairroInexistenteSemGravar() {
        when(bairroRepository.findById(3L)).thenReturn(Optional.empty());

        var erro = assertThrows(EntityNotFoundException.class,
                () -> service.criarArvore(cadastro(BAIRRO_IDENTIFICADO, 3L)));

        assertTrue(erro.getMessage().contains("Bairro"));
        verifyNoInteractions(repository);
        verify(bairroRepository).findById(3L);
    }

    @Test
    void novoContratoUrbanoPersisteAssociacaoAtivaEDerivaClassificacao() throws Exception {
        Bairro bairro = bairro(true);
        when(bairroRepository.findById(3L)).thenReturn(Optional.of(bairro));
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ONE, null, 3L,
                null, null, null, null, null, null, NAO_AVALIADA, null, false, null);

        var resposta = service.criarArvore(dto);

        assertEquals(BAIRRO_IDENTIFICADO, resposta.classificacaoTerritorial());
        assertEquals(false, resposta.zonaRural());
        assertEquals(3L, resposta.bairro().id());
        assertEquals(bairro.getNome(), resposta.bairroNome());
        assertNull(resposta.localidadeRural());
    }

    @Test
    void novoContratoRuralPersisteLocalidadeSemAssociarBairro() {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ONE, null, null,
                null, "06390-000", "Estrada municipal", null, null, null,
                NAO_AVALIADA, null, true, "Sítio Boa Vista");

        var resposta = service.criarArvore(dto);

        assertEquals(FORA_DA_AREA_URBANA, resposta.classificacaoTerritorial());
        assertEquals(true, resposta.zonaRural());
        assertEquals("Sítio Boa Vista", resposta.localidadeRural());
        assertNull(resposta.bairro());
        assertNull(resposta.bairroNome());
        verifyNoInteractions(bairroRepository);
    }

    @Test
    void novoContratoUrbanoRejeitaBairroInativo() throws Exception {
        when(bairroRepository.findById(3L)).thenReturn(Optional.of(bairro(false)));
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ONE, null, 3L,
                null, null, null, null, null, null, NAO_AVALIADA, null, false, null);

        assertThrows(EntradaInventarioInvalidaException.class, () -> service.criarArvore(dto));
        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarBairroInativoSemGravar() throws Exception {
        when(bairroRepository.findById(3L)).thenReturn(Optional.of(bairro(false)));

        assertThrows(EntradaInventarioInvalidaException.class,
                () -> service.criarArvore(cadastro(BAIRRO_IDENTIFICADO, 3L)));

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarBairroSemDisponibilidadeDefinida() throws Exception {
        when(bairroRepository.findById(3L)).thenReturn(Optional.of(bairro(null)));

        assertThrows(EntradaInventarioInvalidaException.class,
                () -> service.criarArvore(cadastro(BAIRRO_IDENTIFICADO, 3L)));

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarTerritorioIncompativelAntesDeConsultarRepositories() {
        for (var dto : new ArvoreCadastroRequestDTO[]{cadastro(BAIRRO_IDENTIFICADO, null),
                cadastro(BAIRRO_NAO_IDENTIFICADO, 3L), cadastro(FORA_DA_AREA_URBANA, 3L)}) {
            var erro = assertThrows(ConstraintViolationException.class, () -> service.criarArvore(dto));
            String campo = dto.classificacaoTerritorial() == BAIRRO_IDENTIFICADO ? "bairroNome" : "bairroId";
            assertTrue(erro.getConstraintViolations().stream()
                    .anyMatch(violacao -> violacao.getPropertyPath().toString().equals(campo)));
        }
        verifyNoInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void deveValidarObrigatoriosECoordenadasNoService() {
        for (var dto : new ArvoreCadastroRequestDTO[]{
                new ArvoreCadastroRequestDTO(null, null, null, null, null, null, null, null, null, null, null),
                new ArvoreCadastroRequestDTO(7L, new BigDecimal("90.0001"), new BigDecimal("-180.0001"),
                        BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, null, null),
                new ArvoreCadastroRequestDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO,
                        BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, null, null)}) {
            assertThrows(ConstraintViolationException.class, () -> service.criarArvore(dto));
        }
        verifyNoInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void deveValidarMedidasPositivasNoService() {
        for (String valor : new String[]{"0", "-0.01"}) {
            var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                    BAIRRO_NAO_IDENTIFICADO, null, null, null,
                    new BigDecimal(valor), new BigDecimal(valor), null, null);
            assertThrows(ConstraintViolationException.class, () -> service.criarArvore(dto));
        }
        verifyNoInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void devePreservarDadosTecnicosDesconhecidosComoNulos() {
        var resposta = service.criarArvore(minimo(BAIRRO_NAO_IDENTIFICADO));

        assertEquals(42L, resposta.id());
        assertEquals(NAO_AVALIADA, resposta.condicaoFitossanitaria());
        assertNull(resposta.dataPlantio());
        assertNull(resposta.dapCm());
        assertNull(resposta.alturaEstimadaM());
        assertNull(resposta.referenciaLocalizacao());
        assertNull(resposta.observacoes());
        assertNull(resposta.bairro());
        verifyNoInteractions(bairroRepository);
    }

    @Test
    void deveConsultarFichaCompletaComDadosBotanicosETerritoriais() throws Exception {
        var arvore = arvorePersistida(bairro(true));
        when(repository.findById(42L)).thenReturn(Optional.of(arvore));

        var resposta = service.buscarPorId(42L);

        assertEquals(mapper.toResponseDTO(arvore), resposta);
        assertEquals(especie.getNomePopular(), resposta.especie().nomePopular());
        assertEquals(especie.getNomeCientifico(), resposta.especie().nomeCientifico());
        assertEquals(especie.getFamiliaBotanica(), resposta.especie().familiaBotanica());
        assertEquals(3L, resposta.bairro().id());
        verify(repository).findById(42L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(especieRepository, bairroRepository);
    }

    @Test
    void deveInformarArvoreInexistente() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        var erro = assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(42L));

        assertTrue(erro.getMessage().contains("Árvore"));
        verify(repository).findById(42L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(especieRepository, bairroRepository);
    }

    @Test
    void deveRejeitarIdentificadorDeConsultaInvalido() {
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThrows(EntradaInventarioInvalidaException.class, () -> service.buscarPorId(id));
        }
        verifyNoInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void devePermitirCadastroDeArvoreMortaSemRemocao() {
        var dto = new ArvoreCadastroRequestDTO(7L, BigDecimal.ZERO, BigDecimal.ZERO,
                BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, MORTA, null);

        var resposta = service.criarArvore(dto);

        assertEquals(MORTA, resposta.condicaoFitossanitaria());
        assertEquals(42L, resposta.id());
        verify(repository).save(any(Arvore.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deveRetornarArvoreMortaSemAlterarRegistro() throws Exception {
        var arvore = arvorePersistida(null);
        arvore.setCondicaoFitossanitaria(MORTA);
        when(repository.findById(42L)).thenReturn(Optional.of(arvore));
        var atualizadoEm = arvore.getAtualizadoEm();

        assertEquals(MORTA, service.buscarPorId(42L).condicaoFitossanitaria());

        assertEquals(atualizadoEm, arvore.getAtualizadoEm());
        assertEquals(MORTA, arvore.getCondicaoFitossanitaria());
        verify(repository).findById(42L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deveManterConsultaDeVinculoHistoricoComBairroIndisponivel() throws Exception {
        when(repository.findById(42L)).thenReturn(Optional.of(arvorePersistida(bairro(false))));

        var resposta = service.buscarPorId(42L);

        assertEquals(3L, resposta.bairro().id());
        assertFalse(resposta.bairro().ativo());
        verifyNoInteractions(bairroRepository);
        verify(repository, never()).save(any(Arvore.class));
    }

    @Test
    void devePropagarFalhaDePersistenciaSemInformarSucesso() {
        RuntimeException falha = new RuntimeException("Falha simulada de persistência");
        when(repository.save(any(Arvore.class))).thenThrow(falha);

        assertSame(falha, assertThrows(RuntimeException.class,
                () -> service.criarArvore(minimo(BAIRRO_NAO_IDENTIFICADO))));

        verify(repository).save(any(Arvore.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deveDeclararTransacoesDeEscritaELeitura() throws Exception {
        var cadastro = ArvoreService.class.getMethod("criarArvore", ArvoreCadastroRequestDTO.class)
                .getAnnotation(Transactional.class);
        var consulta = ArvoreService.class.getMethod("buscarPorId", Long.class).getAnnotation(Transactional.class);

        assertNotNull(cadastro);
        assertFalse(cadastro.readOnly());
        assertNotNull(consulta);
        assertTrue(consulta.readOnly());
        assertEquals(ArvoreResponseDTO.class, ArvoreService.class.getMethod("buscarPorId", Long.class).getReturnType());
    }

    @Test
    void deveLimitarContratosDosRepositoriesAoEscopoAtual() {
        Set<String> metodosArvore = Arrays.stream(ArvoreRepository.class.getMethods())
                .map(metodo -> metodo.getName()).collect(Collectors.toSet());
        Set<String> metodosBairro = Arrays.stream(BairroRepository.class.getMethods())
                .map(metodo -> metodo.getName()).collect(Collectors.toSet());

        assertEquals(Set.of("save", "flush", "findById", "findAll", "findByIdParaFotografias"), metodosArvore);
        assertEquals(Set.of("findById", "findAllByAtivoTrueOrderByNomeAsc"), metodosBairro);
        assertTrue(org.springframework.data.repository.Repository.class.isAssignableFrom(ArvoreRepository.class));
    }

    @Test
    void deveRejeitarCadastroNuloAntesDeConsultarRepositories() {
        assertThrows(EntradaInventarioInvalidaException.class, () -> service.criarArvore(null));
        verifyNoInteractions(repository, especieRepository, bairroRepository);
    }

    @Test
    void deveAceitarLimitesGlobaisSemAplicarLimitesMunicipaisPresumidos() {
        for (BigDecimal latitude : new BigDecimal[]{new BigDecimal("-90"), new BigDecimal("90")}) {
            for (BigDecimal longitude : new BigDecimal[]{new BigDecimal("-180"), new BigDecimal("180")}) {
                var dto = new ArvoreCadastroRequestDTO(7L, latitude, longitude,
                        FORA_DA_AREA_URBANA, null, null, null, null, null, null, null);
                assertEquals(latitude, service.criarArvore(dto).latitude());
            }
        }
        verifyNoInteractions(bairroRepository);
    }
}
