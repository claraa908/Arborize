package br.com.arborize.services;

import br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO;
import br.com.arborize.entities.*;
import br.com.arborize.enums.*;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.mappers.ArvoreMapper;
import br.com.arborize.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArvoreAtualizacaoServiceTest {
    private ArvoreRepository repository;
    private EspecieRepository especies;
    private BairroRepository bairros;
    private FotoArvoreRepository fotos;
    private ArvoreMapper mapper;
    private ArvoreService service;
    private Arvore existente;
    private Especie novaEspecie;
    private final LocalDateTime criado=LocalDateTime.of(2025,1,1,10,0);
    private final LocalDateTime atualizado=LocalDateTime.of(2025,2,1,10,0);

    @BeforeEach
    void prepare() {
        repository=mock(ArvoreRepository.class);especies=mock(EspecieRepository.class);bairros=mock(BairroRepository.class);fotos=mock(FotoArvoreRepository.class);
        mapper=spy(new ArvoreMapper());
        var validator=new LocalValidatorFactoryBean();validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();service=new ArvoreService(repository,especies,bairros,fotos,mapper,validator);
        var especie=new Especie();especie.setId(7L);especie.setNomePopular("Espécie de teste");
        existente=new Arvore();existente.setEspecie(especie);
        existente.setLatitude(new BigDecimal("-4.97"));existente.setLongitude(new BigDecimal("-39.01"));
        existente.setReferenciaLocalizacao("Referência anterior");existente.setDataPlantio(LocalDate.of(2020,1,1));
        existente.setDapCm(BigDecimal.TEN);existente.setAlturaEstimadaM(BigDecimal.ONE);existente.setObservacoes("Observação anterior");
        ReflectionTestUtils.setField(existente,"id",42L);ReflectionTestUtils.setField(existente,"criadoEm",criado);
        ReflectionTestUtils.setField(existente,"atualizadoEm",atualizado);
        novaEspecie=new Especie();novaEspecie.setId(8L);novaEspecie.setNomePopular("Outra espécie de teste");
        when(repository.findById(42L)).thenReturn(Optional.of(existente));
        when(especies.findById(8L)).thenReturn(Optional.of(novaEspecie));
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
    }

    private ArvoreAtualizacaoRequestDTO dto(ClassificacaoTerritorial classificacao, Long bairroId) {
        return new ArvoreAtualizacaoRequestDTO(8L,new BigDecimal("-4.98"),new BigDecimal("-39.02"),
                classificacao,bairroId,null,null,null,null,CondicaoFitossanitaria.MORTA,null);
    }

    private Bairro bairro(Boolean ativo) {
        var bairro=new Bairro();ReflectionTestUtils.setField(bairro,"id",2L);bairro.setNome("Referência de teste");bairro.setAtivo(ativo);return bairro;
    }

    @Test
    void atualizaExistentePreservaIdCriacaoECorrigeLocalizacaoEspecieECondicao() {
        var entrada=dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null);
        var resposta=service.atualizarArvore(42L,entrada);
        assertEquals(42L,resposta.id());assertEquals(criado,resposta.criadoEm());
        assertEquals(8L,resposta.especie().id());assertSame(novaEspecie,existente.getEspecie());
        assertEquals(entrada.latitude(),resposta.latitude());assertEquals(entrada.longitude(),resposta.longitude());
        assertEquals(CondicaoFitossanitaria.MORTA,resposta.condicaoFitossanitaria());
        verify(repository).save(same(existente));
        var ordem=inOrder(repository,mapper);
        ordem.verify(repository).findById(42L);
        ordem.verify(mapper).updateEntity(existente,entrada,novaEspecie,null);
        ordem.verify(repository).save(existente);ordem.verify(repository).flush();
        ordem.verify(mapper).toResponseDTO(existente);
        // Não se simula callback: execução real do @PreUpdate permanece pendente de integração.
        assertEquals(atualizado,existente.getAtualizadoEm());verifyNoInteractions(bairros);
    }

    @Test
    void limpaCamposOpcionaisSemValoresFicticios() {
        service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null));
        assertNull(existente.getReferenciaLocalizacao());assertNull(existente.getDataPlantio());
        assertNull(existente.getDapCm());assertNull(existente.getAlturaEstimadaM());assertNull(existente.getObservacoes());
    }

    @Test
    void preservaValoresReenviadosSemAlteracao() {
        var entrada=new ArvoreAtualizacaoRequestDTO(8L,existente.getLatitude(),existente.getLongitude(),
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,existente.getReferenciaLocalizacao(),existente.getDataPlantio(),
                existente.getDapCm(),existente.getAlturaEstimadaM(),existente.getCondicaoFitossanitaria(),existente.getObservacoes());
        var resposta=service.atualizarArvore(42L,entrada);
        assertEquals(entrada.referenciaLocalizacao(),resposta.referenciaLocalizacao());assertEquals(entrada.dataPlantio(),resposta.dataPlantio());
        assertEquals(entrada.dapCm(),resposta.dapCm());assertEquals(entrada.alturaEstimadaM(),resposta.alturaEstimadaM());
        assertEquals(entrada.observacoes(),resposta.observacoes());
    }

    @Test
    void associaBairroIdentificadoExistenteEDisponivel() {
        var bairro=bairro(true);when(bairros.findById(2L)).thenReturn(Optional.of(bairro));
        var resposta=service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,2L));
        assertSame(bairro,existente.getBairro());assertEquals(2L,resposta.bairro().id());
        assertEquals(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,resposta.classificacaoTerritorial());
    }

    @Test
    void trocaDeBairroIdentificadoParaSituacoesSemBairroLimpaAssociacao() {
        for (var classificacao : new ClassificacaoTerritorial[]{ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                ClassificacaoTerritorial.FORA_DA_AREA_URBANA}) {
            existente.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);existente.setBairro(bairro(true));
            var resposta=service.atualizarArvore(42L,dto(classificacao,null));
            assertNull(existente.getBairro());assertNull(resposta.bairro());assertEquals(classificacao,resposta.classificacaoTerritorial());
        }
        verifyNoInteractions(bairros);
    }

    @Test
    void rejeitaArvoreInexistenteSemConsultarRelacoesOuSalvar() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,()->service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null)));
        verifyNoInteractions(especies,bairros,mapper);verify(repository,never()).save(any());verify(repository,never()).flush();
    }

    @Test
    void rejeitaEspecieInexistenteAntesDeModificarEntidade() {
        when(especies.findById(8L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,()->service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null)));
        assertEquals(7L,existente.getEspecie().getId());assertEquals(new BigDecimal("-4.97"),existente.getLatitude());
        verifyNoInteractions(mapper,bairros);verify(repository,never()).save(any());
    }

    @Test
    void rejeitaBairroInexistenteAntesDeModificarEntidade() {
        when(bairros.findById(2L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,()->service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,2L)));
        assertEquals(7L,existente.getEspecie().getId());verifyNoInteractions(mapper);verify(repository,never()).save(any());
    }

    @Test
    void rejeitaBairroIndisponivelAntesDeModificarEntidade() {
        for (Boolean ativo : new Boolean[]{false,null}) {
            when(bairros.findById(2L)).thenReturn(Optional.of(bairro(ativo)));
            assertThrows(EntradaInventarioInvalidaException.class,()->service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,2L)));
            assertEquals(7L,existente.getEspecie().getId());verifyNoInteractions(mapper);verify(repository,never()).save(any());
        }
    }

    @Test
    void rejeitaDadosInvalidosAntesDeConsultarRepositorio() {
        for (String latitude : new String[]{"91","-91"}) {
            var entrada=new ArvoreAtualizacaoRequestDTO(8L,new BigDecimal(latitude),BigDecimal.ZERO,
                    ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,null,null,null,null,CondicaoFitossanitaria.NAO_AVALIADA,null);
            assertThrows(ConstraintViolationException.class,()->service.atualizarArvore(42L,entrada));
        }
        for (var entrada : new ArvoreAtualizacaoRequestDTO[]{
                dto(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,null),dto(ClassificacaoTerritorial.FORA_DA_AREA_URBANA,2L),
                new ArvoreAtualizacaoRequestDTO(null,BigDecimal.ZERO,new BigDecimal("181"),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                        null,null,null,BigDecimal.ZERO,new BigDecimal("-1"),null,null)}) {
            assertThrows(ConstraintViolationException.class,()->service.atualizarArvore(42L,entrada));
        }
        verifyNoInteractions(repository,especies,bairros,mapper);
    }

    @Test
    void rejeitaIdOuCorpoInvalidosAntesDeConsultarRepositorio() {
        for (Long id : new Long[]{null,0L,-1L}) {
            assertThrows(EntradaInventarioInvalidaException.class,()->service.atualizarArvore(id,dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null)));
        }
        assertThrows(EntradaInventarioInvalidaException.class,()->service.atualizarArvore(42L,null));
        verifyNoInteractions(repository,especies,bairros,mapper);
    }

    @Test
    void falhaDePersistenciaNaoRetornaConfirmacaoDeSucesso() {
        doThrow(new IllegalStateException("Falha simulada")).when(repository).flush();
        assertThrows(IllegalStateException.class,()->service.atualizarArvore(42L,dto(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null)));
        verify(mapper,never()).toResponseDTO(any());
    }

    @Test
    void atualizacaoDeclaraTransacaoDeEscrita() throws Exception {
        assertFalse(ArvoreService.class.getMethod("atualizarArvore",Long.class,ArvoreAtualizacaoRequestDTO.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
    }
}
