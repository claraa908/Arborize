package br.com.arborize.repositories;

import br.com.arborize.utils.CodigoArvore;
import br.com.arborize.entities.Arvore;
import br.com.arborize.enums.CondicaoFitossanitaria;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

/** Critérios compartilhados pela tabela e pelo mapa, executados no banco. */
public final class ArvoreSpecifications {
    private ArvoreSpecifications() { }

    public static Specification<Arvore> filtrar(String pesquisa, Long bairroId, Long especieId,
                                               CondicaoFitossanitaria condicao) {
        return filtrar(pesquisa, null, bairroId, especieId, condicao);
    }

    public static Specification<Arvore> filtrar(String pesquisa, String bairro, Long bairroId, Long especieId,
                                               CondicaoFitossanitaria condicao) {
        String termo = pesquisa == null ? "" : pesquisa.strip().toLowerCase(Locale.ROOT);
        String nomeBairro = bairro == null ? "" : bairro.strip().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            var criterios = new ArrayList<Predicate>();
            if (!termo.isEmpty()) {
                String padrao = padraoParcial(termo);
                var especie = root.join("especie");
                var id = root.<Long>get("id");
                var idTexto = id.cast(String.class);
                var idFormatado = cb.<String>selectCase()
                        .when(cb.lessThan(id, 1_000_000L), cb.function("lpad", String.class,
                                idTexto, cb.literal(CodigoArvore.DIGITOS_MINIMOS), cb.literal("0")))
                        .otherwise(idTexto);
                var codigo = cb.lower(cb.concat(cb.literal(CodigoArvore.PREFIXO.toLowerCase(Locale.ROOT)), idFormatado));
                // JPA 3.2: CAST real, sem depender de SQL nativo ou converter o termo para Long.
                criterios.add(cb.or(cb.like(idTexto, padrao, '\\'), cb.like(codigo, padrao, '\\'),
                        cb.like(cb.lower(especie.get("nomePopular")), padrao, '\\'),
                        cb.like(cb.lower(especie.get("nomeCientifico")), padrao, '\\')));
            }
            if (!nomeBairro.isEmpty()) {
                var bairroLegado = root.join("bairro", JoinType.LEFT);
                var nomePersistido = root.<String>get("bairroNome");
                var nomeLegado = bairroLegado.<String>get("nome");
                var nomeEfetivo = cb.coalesce(nomePersistido, nomeLegado);
                criterios.add(cb.like(cb.lower(nomeEfetivo), padraoParcial(nomeBairro), '\\'));
            }
            if (bairroId != null) criterios.add(cb.equal(root.get("bairro").get("id"), bairroId));
            if (especieId != null) criterios.add(cb.equal(root.get("especie").get("id"), especieId));
            if (condicao != null) criterios.add(cb.equal(root.get("condicaoFitossanitaria"), condicao));
            return cb.and(criterios.toArray(Predicate[]::new));
        };
    }

    private static String padraoParcial(String termo) {
        return "%" + termo.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
