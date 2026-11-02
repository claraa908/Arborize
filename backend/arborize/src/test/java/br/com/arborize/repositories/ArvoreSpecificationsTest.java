package br.com.arborize.repositories;

import br.com.arborize.entities.Arvore;
import br.com.arborize.enums.CondicaoFitossanitaria;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/** Verifica a construção Criteria; não substitui execução de integração em banco. */
@SuppressWarnings({"unchecked", "rawtypes"})
class ArvoreSpecificationsTest {
    private Root<Arvore> root;
    private CriteriaBuilder cb;
    private CriteriaQuery<?> query;
    private Path<Long> id;
    private Path<Object> bairro;
    private Path<Object> bairroId;
    private Path<Object> especie;
    private Path<Object> especieId;
    private Path<Object> condicao;
    private Join<Object, Object> join;
    private Path<String> popular;
    private Path<String> cientifico;
    private Expression<String> textoId;
    private Expression<Integer> larguraId;
    private Expression<String> zero;
    private Expression<String> prefixo;
    private Expression<String> idComZeros;
    private Expression<String> codigoCompleto;
    private Expression<String> codigoConcatenado;
    private Expression<String> lowerPopular;
    private Expression<String> lowerCientifico;
    private Expression<String> lowerCodigo;
    private CriteriaBuilder.Case<String> idFormatado;
    private Predicate condicaoId, porId, porCodigo, porPopular, porCientifico, texto, porBairro, porEspecie, porCondicao;

    @BeforeEach
    void prepare() {
        root = mock(Root.class); cb = mock(CriteriaBuilder.class); query = mock(CriteriaQuery.class);
        id = mock(Path.class); bairro = mock(Path.class); bairroId = mock(Path.class);
        especie = mock(Path.class); especieId = mock(Path.class); condicao = mock(Path.class);
        join = mock(Join.class); popular = mock(Path.class); cientifico = mock(Path.class);
        textoId = mock(Expression.class); larguraId = mock(Expression.class); zero = mock(Expression.class);
        prefixo = mock(Expression.class); idComZeros = mock(Expression.class); codigoCompleto = mock(Expression.class);
        codigoConcatenado = mock(Expression.class);
        lowerPopular = mock(Expression.class); lowerCientifico = mock(Expression.class); lowerCodigo = mock(Expression.class);
        idFormatado = mock(CriteriaBuilder.Case.class);
        condicaoId = mock(Predicate.class); porId=mock(Predicate.class); porCodigo=mock(Predicate.class);
        porPopular=mock(Predicate.class); porCientifico=mock(Predicate.class);
        texto=mock(Predicate.class); porBairro=mock(Predicate.class); porEspecie=mock(Predicate.class); porCondicao=mock(Predicate.class);
        when(root.<Long>get("id")).thenReturn(id); when(id.cast(String.class)).thenReturn(textoId);
        when(root.get("bairro")).thenReturn(bairro); when(bairro.get("id")).thenReturn(bairroId);
        when(root.get("especie")).thenReturn(especie); when(especie.get("id")).thenReturn(especieId);
        when(root.get("condicaoFitossanitaria")).thenReturn(condicao);
        when(root.join("especie")).thenReturn(join);
        when(join.<String>get("nomePopular")).thenReturn(popular);
        when(join.<String>get("nomeCientifico")).thenReturn(cientifico);
        when(cb.<String>selectCase()).thenReturn(idFormatado);
        when(cb.lessThan(id, 1_000_000L)).thenReturn(condicaoId);
        when(cb.literal(6)).thenReturn(larguraId);
        when(cb.literal("0")).thenReturn(zero);
        when(cb.literal("arv-qxd-")).thenReturn(prefixo);
        when(cb.function("lpad", String.class, textoId, larguraId, zero)).thenReturn(idComZeros);
        when(idFormatado.when(condicaoId, idComZeros)).thenReturn(idFormatado);
        when(idFormatado.otherwise(textoId)).thenReturn(codigoCompleto);
        when(cb.concat(prefixo, codigoCompleto)).thenReturn(codigoConcatenado);
        when(cb.lower(codigoConcatenado)).thenReturn(lowerCodigo);
        when(cb.lower(popular)).thenReturn(lowerPopular); when(cb.lower(cientifico)).thenReturn(lowerCientifico);
        when(cb.like(eq(textoId), anyString(), eq('\\'))).thenReturn(porId);
        when(cb.like(eq(lowerCodigo), anyString(), eq('\\'))).thenReturn(porCodigo);
        when(cb.like(eq(lowerPopular), anyString(), eq('\\'))).thenReturn(porPopular);
        when(cb.like(eq(lowerCientifico), anyString(), eq('\\'))).thenReturn(porCientifico);
        when(cb.or(porId, porCodigo, porPopular, porCientifico)).thenReturn(texto);
        when(cb.equal(bairroId, 2L)).thenReturn(porBairro);
        when(cb.equal(especieId, 7L)).thenReturn(porEspecie);
        when(cb.equal(condicao, CondicaoFitossanitaria.MORTA)).thenReturn(porCondicao);
    }

    @Test
    void ausenciaDePesquisaNaoAdicionaJoinOuRestricoes() {
        for (String pesquisa : new String[]{null, "", "   "}) {
            clearInvocations(root, cb);
            ArvoreSpecifications.filtrar(pesquisa,null,null,null).toPredicate(root,query,cb);
            verify(cb).and(new Predicate[0]); verifyNoInteractions(root);
        }
    }

    @Test
    void pesquisaParcialNormalizaEProcuraIdCodigoPopularECientificoPorUniao() {
        for (String pesquisa : new String[]{" 12 ", " CAJU ", " Anacardium ", " ARV-QXD-000001 "}) {
            clearInvocations(cb, id);
            ArvoreSpecifications.filtrar(pesquisa,null,null,null).toPredicate(root,query,cb);
            String padrao="%"+pesquisa.strip().toLowerCase(java.util.Locale.ROOT)+"%";
            verify(id).cast(String.class);
            verify(cb).like(textoId,padrao,'\\');
            verify(cb).like(lowerCodigo,padrao,'\\');
            verify(cb).like(lowerPopular,padrao,'\\');
            verify(cb).like(lowerCientifico,padrao,'\\');
            verify(cb).or(porId,porCodigo,porPopular,porCientifico);
            verify(cb).and(new Predicate[]{texto});
        }
    }

    @Test
    void curingasDaPesquisaSaoTextoLiteral() {
        ArvoreSpecifications.filtrar("%_\\",null,null,null).toPredicate(root,query,cb);
        verify(cb).like(textoId,"%\\%\\_\\\\%",'\\');
        verify(cb).like(lowerCodigo,"%\\%\\_\\\\%",'\\');
    }

    @Test
    void filtroBairroUsaReferenciaRealSemClassificacaoArtificial() {
        ArvoreSpecifications.filtrar(null,2L,null,null).toPredicate(root,query,cb);
        verify(cb).equal(bairroId,2L); verify(cb).and(new Predicate[]{porBairro});
        verify(root,never()).get("classificacaoTerritorial");
    }

    @Test
    void filtroEspecie() {
        ArvoreSpecifications.filtrar(null,null,7L,null).toPredicate(root,query,cb);
        verify(cb).equal(especieId,7L); verify(cb).and(new Predicate[]{porEspecie});
    }

    @Test
    void filtroCondicao() {
        ArvoreSpecifications.filtrar(null,null,null,CondicaoFitossanitaria.MORTA).toPredicate(root,query,cb);
        verify(cb).and(new Predicate[]{porCondicao});
    }

    @Test
    void filtrosCombinadosUsamIntersecao() {
        ArvoreSpecifications.filtrar(null,2L,7L,CondicaoFitossanitaria.MORTA).toPredicate(root,query,cb);
        verify(cb).and(new Predicate[]{porBairro,porEspecie,porCondicao});
    }

    @Test
    void pesquisaEFiltrosUsamIntersecao() {
        ArvoreSpecifications.filtrar("caju",2L,7L,CondicaoFitossanitaria.MORTA).toPredicate(root,query,cb);
        verify(cb).and(new Predicate[]{texto,porBairro,porEspecie,porCondicao});
    }

    @Test
    void consultasPaginadaEMapaCarregamSomenteRelacoesNecessariasNoGrafo() throws Exception {
        for (Class<?> paginacao : new Class[]{org.springframework.data.domain.Pageable.class,org.springframework.data.domain.Sort.class}) {
            // Mesmas assinaturas da implementação base, sem expor delete/update por Specification.
            org.junit.jupiter.api.Assertions.assertNotNull(org.springframework.data.jpa.repository.support.SimpleJpaRepository.class
                    .getMethod("findAll", org.springframework.data.jpa.domain.Specification.class, paginacao));
            var metodo=ArvoreRepository.class.getMethod("findAll",org.springframework.data.jpa.domain.Specification.class,paginacao);
            org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"especie","bairro"},
                    metodo.getAnnotation(org.springframework.data.jpa.repository.EntityGraph.class).attributePaths());
        }
    }
}
