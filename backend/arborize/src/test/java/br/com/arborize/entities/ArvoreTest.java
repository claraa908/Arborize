package br.com.arborize.entities;

import br.com.arborize.enums.CondicaoFitossanitaria;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArvoreTest {

    @Test
    void deveIniciarNaoAvaliadaEManterDadosDesconhecidosNulos() {
        Arvore arvore = new Arvore();

        assertEquals(CondicaoFitossanitaria.NAO_AVALIADA, arvore.getCondicaoFitossanitaria());
        assertNull(arvore.getId());
        assertNull(arvore.getDataPlantio());
        assertNull(arvore.getDapCm());
        assertNull(arvore.getAlturaEstimadaM());
        assertNull(arvore.getObservacoes());
        assertNull(arvore.getBairroNome());
        assertNull(arvore.getCep());
    }

    @Test
    void deveDeclararIdentificadorGeradoEEspecieObrigatoriaSemCascata() throws Exception {
        var id = Arvore.class.getDeclaredField("id");
        assertNotNull(id.getAnnotation(Id.class));
        assertEquals(GenerationType.IDENTITY, id.getAnnotation(GeneratedValue.class).strategy());
        assertThrows(NoSuchMethodException.class, () -> Arvore.class.getMethod("setId", Long.class));

        var especie = Arvore.class.getDeclaredField("especie");
        var relacionamento = especie.getAnnotation(ManyToOne.class);
        assertEquals(Especie.class, especie.getType());
        assertFalse(relacionamento.optional());
        assertEquals(FetchType.LAZY, relacionamento.fetch());
        assertEquals(0, relacionamento.cascade().length);
        assertFalse(especie.getAnnotation(JoinColumn.class).nullable());

        assertFalse(Arvore.class.getDeclaredField("latitude").getAnnotation(Column.class).nullable());
        assertFalse(Arvore.class.getDeclaredField("longitude").getAnnotation(Column.class).nullable());
        assertEquals(EnumType.STRING, Arvore.class.getDeclaredField("condicaoFitossanitaria")
                .getAnnotation(Enumerated.class).value());
    }

    @Test
    void deveInicializarMetadadosECondicaoAusenteAntesDePersistir() throws Exception {
        Arvore arvore = new Arvore();
        arvore.setCondicaoFitossanitaria(null);
        LocalDateTime inicio = LocalDateTime.now();

        arvore.aoCriar();

        assertNotNull(arvore.getCriadoEm());
        assertFalse(arvore.getCriadoEm().isBefore(inicio));
        assertEquals(arvore.getCriadoEm(), arvore.getAtualizadoEm());
        assertEquals(CondicaoFitossanitaria.NAO_AVALIADA, arvore.getCondicaoFitossanitaria());
        assertNotNull(Arvore.class.getDeclaredMethod("aoCriar").getAnnotation(PrePersist.class));
        assertNotNull(Arvore.class.getDeclaredMethod("aoAtualizar").getAnnotation(PreUpdate.class));
        assertFalse(Arvore.class.getDeclaredField("criadoEm").getAnnotation(Column.class).updatable());
    }

    @Test
    void deveAtualizarMetadadoSemAlterarCriacaoOuCondicaoMorta() {
        Arvore arvore = new Arvore();
        arvore.setCondicaoFitossanitaria(CondicaoFitossanitaria.MORTA);
        arvore.aoCriar();
        LocalDateTime criacao = arvore.getCriadoEm();
        LocalDateTime inicioAtualizacao = LocalDateTime.now();

        arvore.aoAtualizar();

        assertEquals(criacao, arvore.getCriadoEm());
        assertFalse(arvore.getAtualizadoEm().isBefore(inicioAtualizacao));
        assertEquals(CondicaoFitossanitaria.MORTA, arvore.getCondicaoFitossanitaria());
    }

    @Test
    void deveMapearDecimaisSemEscalaFixaECamposTecnicosOpcionais() throws Exception {
        for (String nome : new String[]{"latitude", "longitude", "dapCm", "alturaEstimadaM"}) {
            var campo = Arvore.class.getDeclaredField(nome);
            assertEquals(BigDecimal.class, campo.getType());
            assertEquals("NUMERIC", campo.getAnnotation(Column.class).columnDefinition());
        }
        for (String nome : new String[]{"dataPlantio", "dapCm", "alturaEstimadaM", "observacoes"}) {
            assertTrue(Arvore.class.getDeclaredField(nome).getAnnotation(Column.class).nullable());
        }
        for (String nome : new String[]{"bairroNome", "cep"}) {
            var campo = Arvore.class.getDeclaredField(nome);
            assertEquals(String.class, campo.getType());
            assertTrue(campo.getAnnotation(Column.class).nullable());
            assertEquals("TEXT", campo.getAnnotation(Column.class).columnDefinition());
        }
        assertEquals("bairro_nome", Arvore.class.getDeclaredField("bairroNome").getAnnotation(Column.class).name());
        assertEquals("cep", Arvore.class.getDeclaredField("cep").getAnnotation(Column.class).name());
    }
}
