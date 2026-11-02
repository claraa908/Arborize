package br.com.arborize.entities;

import br.com.arborize.enums.ClassificacaoTerritorial;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepresentacaoTerritorialTest {

    @Test
    void devePermitirBairroNaoIdentificadoSemCriarBairroArtificial() {
        Arvore arvore = new Arvore();

        assertEquals(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, arvore.getClassificacaoTerritorial());
        assertNull(arvore.getBairro());
        assertDoesNotThrow(arvore::aoCriar);
    }

    @Test
    void devePermitirAreaForaDaZonaUrbanaSemBairro() {
        Arvore arvore = new Arvore();
        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.FORA_DA_AREA_URBANA);

        assertDoesNotThrow(arvore::aoCriar);
        assertDoesNotThrow(arvore::aoAtualizar);
        assertNull(arvore.getBairro());
    }

    @Test
    void deveExigirReferenciaQuandoBairroForIdentificado() {
        Arvore arvore = new Arvore();
        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);

        assertThrows(IllegalStateException.class, arvore::aoCriar);
        assertThrows(IllegalStateException.class, arvore::aoAtualizar);
        assertNull(arvore.getCriadoEm());

        Bairro bairro = new Bairro();
        arvore.setBairro(bairro);
        assertDoesNotThrow(arvore::aoCriar);
        assertSame(bairro, arvore.getBairro());
    }

    @Test
    void deveAceitarNomeTextualSemAssociacaoAoCatalogo() {
        Arvore arvore = new Arvore();
        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
        arvore.setBairroNome("Bairro confirmado pelo operador");
        arvore.setCep("63900-000");

        assertDoesNotThrow(arvore::aoCriar);
        assertNull(arvore.getBairro());
        assertEquals("Bairro confirmado pelo operador", arvore.getBairroNome());
        assertEquals("63900-000", arvore.getCep());
    }

    @Test
    void deveRejeitarBairroAssociadoNasDemaisSituacoes() {
        for (ClassificacaoTerritorial situacao : new ClassificacaoTerritorial[]{
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                ClassificacaoTerritorial.FORA_DA_AREA_URBANA}) {
            Arvore arvore = new Arvore();
            arvore.setClassificacaoTerritorial(situacao);
            arvore.setBairro(new Bairro());

            assertThrows(IllegalStateException.class, arvore::aoCriar);
            assertThrows(IllegalStateException.class, arvore::aoAtualizar);
        }
    }

    @Test
    void deveRejeitarNomeTextualNasSituacoesSemBairroIdentificado() {
        for (ClassificacaoTerritorial situacao : new ClassificacaoTerritorial[]{
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                ClassificacaoTerritorial.FORA_DA_AREA_URBANA}) {
            Arvore arvore = new Arvore();
            arvore.setClassificacaoTerritorial(situacao);
            arvore.setBairroNome("Bairro não confirmado");

            assertThrows(IllegalStateException.class, arvore::aoCriar);
            assertThrows(IllegalStateException.class, arvore::aoAtualizar);
        }
    }

    @Test
    void deveRejeitarSituacaoTerritorialNula() {
        Arvore arvore = new Arvore();
        arvore.setClassificacaoTerritorial(null);

        assertThrows(IllegalStateException.class, arvore::aoCriar);
        assertThrows(IllegalStateException.class, arvore::aoAtualizar);
    }

    @Test
    void devePermitirTransicaoExplicitaParaLocalSemBairro() {
        Arvore arvore = new Arvore();
        arvore.setBairro(new Bairro());
        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
        arvore.aoCriar();
        var criadoEm = arvore.getCriadoEm();

        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.FORA_DA_AREA_URBANA);
        arvore.setBairro(null);

        assertDoesNotThrow(arvore::aoAtualizar);
        assertEquals(criadoEm, arvore.getCriadoEm());
        assertNull(arvore.getBairro());
    }

    @Test
    void deveMapearAssociacaoOpcionalSemCascataEEnumeracaoComoTexto() throws Exception {
        var campo = Arvore.class.getDeclaredField("bairro");
        var relacionamento = campo.getAnnotation(ManyToOne.class);
        assertEquals(Bairro.class, campo.getType());
        assertTrue(relacionamento.optional());
        assertEquals(FetchType.LAZY, relacionamento.fetch());
        assertEquals(0, relacionamento.cascade().length);
        assertTrue(campo.getAnnotation(JoinColumn.class).nullable());
        assertEquals("bairro_id", campo.getAnnotation(JoinColumn.class).name());
        assertEquals("bairro_id", Arvore.class.getAnnotation(Table.class).indexes()[0].columnList());

        var classificacao = Arvore.class.getDeclaredField("classificacaoTerritorial");
        assertEquals(EnumType.STRING, classificacao.getAnnotation(Enumerated.class).value());
        assertFalse(classificacao.getAnnotation(Column.class).nullable());
        assertEquals(3, ClassificacaoTerritorial.values().length);

        var id = Bairro.class.getDeclaredField("id");
        assertNotNull(id.getAnnotation(Id.class));
        assertEquals(GenerationType.IDENTITY, id.getAnnotation(GeneratedValue.class).strategy());
        assertThrows(NoSuchMethodException.class, () -> Bairro.class.getMethod("setId", Long.class));
        assertFalse(Bairro.class.getDeclaredField("nome").getAnnotation(Column.class).nullable());
    }

    @Test
    void devePreservarVinculoAoAlterarOuIndisponibilizarBairro() {
        Bairro bairro = new Bairro();
        Arvore arvore = new Arvore();
        arvore.setBairro(bairro);
        arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
        arvore.aoCriar();

        bairro.setAtivo(false);

        assertDoesNotThrow(arvore::aoAtualizar);
        assertSame(bairro, arvore.getBairro());
        assertFalse(arvore.getBairro().getAtivo());
        assertEquals(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, arvore.getClassificacaoTerritorial());
    }
}
