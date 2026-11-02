package br.com.arborize.services;

import br.com.arborize.entities.*;
import br.com.arborize.enums.*;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.mappers.ArvoreMapper;
import br.com.arborize.repositories.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ArvoreConsultaServiceTest {
    private ArvoreRepository repository;
    private FotoArvoreRepository fotoRepository;
    private ArvoreService service;

    @BeforeEach
    void prepare() {
        repository=mock(ArvoreRepository.class);
        fotoRepository=mock(FotoArvoreRepository.class);
        service=new ArvoreService(repository,mock(EspecieRepository.class),mock(BairroRepository.class),
                fotoRepository,new ArvoreMapper(),mock(Validator.class));
    }

    private Arvore arvore(long id) throws Exception {
        var especie=new Especie(); especie.setId(7L); especie.setNomePopular("Espécie de teste");
        especie.setNomeCientifico("Nome científico de teste");
        var a=new Arvore();
        org.springframework.test.util.ReflectionTestUtils.setField(a,"id",id);
        a.setEspecie(especie);a.setLatitude(new BigDecimal("-4.97"));a.setLongitude(new BigDecimal("-39.01"));
        return a;
    }

    @Test
    void listagemSemFiltrosPreservaPaginacaoTotalEOrdemPorId() throws Exception {
        var pagina=PageRequest.of(1,2,Sort.by("id"));
        when(repository.findAll(any(Specification.class),eq(pagina)))
                .thenReturn(new PageImpl<>(List.of(arvore(3),arvore(4)),pagina,5));
        var resultado=service.listar(null,null,null,null,PageRequest.of(1,2,Sort.by("observacoes")));
        assertEquals(1,resultado.getNumber());assertEquals(2,resultado.getSize());
        assertEquals(5,resultado.getTotalElements());assertEquals(3,resultado.getTotalPages());
        assertEquals(List.of(3L,4L),resultado.stream().map(a->a.id()).toList());
        verify(repository).findAll(any(Specification.class),eq(pagina));verifyNoMoreInteractions(repository);
    }

    @Test
    void mapaRetornaTodosOsResumosIndependenteDaPaginaDaTabela() throws Exception {
        when(repository.findAll(any(Specification.class),eq(Sort.by("id"))))
                .thenReturn(List.of(arvore(1),arvore(2),arvore(3)));
        when(fotoRepository.findCapasByArvoreIds(List.of(1L,2L,3L))).thenReturn(List.of(new FotoArvoreRepository.FotoCapaProjecao() {
            public Long getArvoreId() { return 2L; }
            public Long getId() { return 9L; }
            public String getContentType() { return "image/webp"; }
        }));
        var resultado=service.consultarMapa("caju",null,7L,CondicaoFitossanitaria.NAO_AVALIADA);
        assertEquals(3,resultado.size());assertEquals(7L,resultado.getFirst().especie().id());
        assertEquals(new BigDecimal("-4.97"),resultado.getFirst().latitude());
        assertEquals(new BigDecimal("-39.01"),resultado.getFirst().longitude());
        assertEquals(CondicaoFitossanitaria.NAO_AVALIADA,resultado.getFirst().condicaoFitossanitaria());
        assertNull(resultado.getFirst().fotoCapa());
        assertEquals(9L,resultado.get(1).fotoCapa().id());
        assertEquals("image/webp",resultado.get(1).fotoCapa().contentType());
        assertNull(resultado.get(2).fotoCapa());
        verify(repository).findAll(any(Specification.class),eq(Sort.by("id")));verifyNoMoreInteractions(repository);
        verify(fotoRepository).findCapasByArvoreIds(List.of(1L,2L,3L));
    }

    @Test
    void pesquisaSemCorrespondenciasEInventarioVazioRetornamResultadosVazios() {
        when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0,10)));
        when(repository.findAll(any(Specification.class),any(Sort.class))).thenReturn(List.of());
        for (String pesquisa : new String[]{null,"sem correspondencia","99999999999999999999999999999"}) {
            var resultado=service.listar(pesquisa,null,null,null,PageRequest.of(0,10));
            assertTrue(resultado.isEmpty());assertEquals(0,resultado.getTotalElements());
            assertEquals(0,resultado.getTotalPages());assertTrue(service.consultarMapa(pesquisa,null,null,null).isEmpty());
        }
        verifyNoInteractions(fotoRepository);
    }

    @Test
    void filtrosInvalidosOuListagemSemPaginacaoNaoConsultamRepositorio() {
        assertThrows(EntradaInventarioInvalidaException.class,()->service.listar(null,0L,null,null,PageRequest.of(0,10)));
        assertThrows(EntradaInventarioInvalidaException.class,()->service.consultarMapa(null,null,-1L,null));
        assertThrows(EntradaInventarioInvalidaException.class,()->service.listar(null,null,null,null,null));
        assertThrows(EntradaInventarioInvalidaException.class,()->service.listar(null,null,null,null,Pageable.unpaged()));
        verifyNoInteractions(repository);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapaETabelaConstroemOsMesmosPredicadosParaPesquisaEFiltros() {
        when(repository.findAll(any(Specification.class),any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0,10)));
        when(repository.findAll(any(Specification.class),any(Sort.class))).thenReturn(List.of());
        service.listar(" CAJU "," centro ",2L,7L,CondicaoFitossanitaria.MORTA,PageRequest.of(0,10));
        service.consultarMapa(" CAJU "," centro ",2L,7L,CondicaoFitossanitaria.MORTA);
        var tabela=org.mockito.ArgumentCaptor.forClass(Specification.class);
        var mapa=org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(tabela.capture(),any(Pageable.class));
        verify(repository).findAll(mapa.capture(),any(Sort.class));
        var root=mock(jakarta.persistence.criteria.Root.class,RETURNS_DEEP_STUBS);
        var cb=mock(jakarta.persistence.criteria.CriteriaBuilder.class,RETURNS_DEEP_STUBS);
        var query=mock(jakarta.persistence.criteria.CriteriaQuery.class);
        tabela.getValue().toPredicate(root,query,cb);
        var criteriosTabela=mockingDetails(cb).getInvocations().stream()
                .map(i->i.getMethod().getName()+java.util.Arrays.deepToString(i.getArguments())).toList();
        clearInvocations(cb);
        mapa.getValue().toPredicate(root,query,cb);
        var criteriosMapa=mockingDetails(cb).getInvocations().stream()
                .map(i->i.getMethod().getName()+java.util.Arrays.deepToString(i.getArguments())).toList();
        assertEquals(criteriosTabela,criteriosMapa);
    }

    @Test
    void consultasDeclaramTransacoesSomenteDeLeitura() throws Exception {
        assertTrue(ArvoreService.class.getMethod("listar",String.class,Long.class,Long.class,
                CondicaoFitossanitaria.class,Pageable.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
        assertTrue(ArvoreService.class.getMethod("consultarMapa",String.class,Long.class,Long.class,
                CondicaoFitossanitaria.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
    }
}
