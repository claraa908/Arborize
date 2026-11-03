package br.com.arborize.services;

import br.com.arborize.dtos.request.AssistentePlantioAnswerRequest;
import br.com.arborize.dtos.request.AssistentePlantioRequest;
import br.com.arborize.dtos.response.EspecieRecomendadaResponse;
import br.com.arborize.entities.Especie;
import br.com.arborize.exceptions.AssistentePlantioValidationException;
import br.com.arborize.repositories.EspecieRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AssistentePlantioService {
    private static final Map<String, Set<String>> ALLOWED_ANSWERS = Map.of(
            "largura_calcada", Set.of("MENOR_1_5M", "ENTRE_1_5M_E_2M", "ENTRE_2M_E_2_4M", "MAIOR_2_4M"),
            "fiacao_eletrica", Set.of("SIM", "NAO"),
            "local_plantio", Set.of("CALCADA", "QUINTAL"),
            "recuo_construcao", Set.of("ATE_2M", "ENTRE_2M_E_4M", "MAIOR_4M"),
            "tipo_solo", Set.of("SECO_RASO", "COM_IRRIGACAO"),
            "objetivo", Set.of("SOMBRA", "FRUTAS", "POLINIZADORES", "RESTAURACAO"),
            "disponibilidade_rega", Set.of("SIM", "NAO"),
            "piso_drenante", Set.of("SIM", "NAO"),
            "substituicao_nim", Set.of("SIM", "NAO"),
            "quantidade_mudas", Set.of("UMA", "DUAS", "TRES")
    );
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("NATIVA DA CAATINGA", "RECOMENDADA", "RECOMENDADAS");
    private static final Map<String, Set<String>> ALLOWED_SIZES_BY_SIDEWALK_WIDTH = Map.of(
            "ENTRE_1_5M_E_2M", Set.of("PEQUENO"),
            "ENTRE_2M_E_2_4M", Set.of("PEQUENO", "MEDIO"),
            "MAIOR_2_4M", Set.of("PEQUENO", "MEDIO", "GRANDE")
    );

    private final EspecieRepository especieRepository;

    public AssistentePlantioService(EspecieRepository especieRepository) {
        this.especieRepository = especieRepository;
    }

    public List<EspecieRecomendadaResponse> recomendar(AssistentePlantioRequest request) {
        Map<String, String> answers = validateAnswers(request.respostas());
        String width = answers.get("largura_calcada");
        String location = answers.get("local_plantio");

        if ("MENOR_1_5M".equals(width) && "CALCADA".equals(location)) {
            return List.of();
        }

        Set<String> allowedSizes = ALLOWED_SIZES_BY_SIDEWALK_WIDTH.get(width);
        boolean overheadWiring = "SIM".equals(answers.get("fiacao_eletrica"));

        return especieRepository.findAll().stream()
                .filter(this::isRecommendedNativeCategory)
                .filter(species -> hasAllowedSize(species, allowedSizes, overheadWiring))
                .sorted(Comparator.comparing(Especie::getNomePopular, String.CASE_INSENSITIVE_ORDER))
                .limit(3)
                .map(this::toResponse)
                .toList();
    }

    private Map<String, String> validateAnswers(List<AssistentePlantioAnswerRequest> answers) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> values = new HashMap<>();
        Set<String> seen = new java.util.HashSet<>();

        if (answers == null) {
            errors.put("respostas", "Informe as 10 respostas do questionário.");
            throw new AssistentePlantioValidationException(errors);
        }

        for (int index = 0; index < answers.size(); index++) {
            AssistentePlantioAnswerRequest answer = answers.get(index);
            String path = "respostas[" + index + "]";
            if (answer == null || answer.perguntaId() == null || answer.perguntaId().isBlank()) {
                errors.put(path + ".perguntaId", "Informe o identificador da pergunta.");
                continue;
            }
            String questionId = answer.perguntaId().trim();
            Set<String> allowedValues = ALLOWED_ANSWERS.get(questionId);
            if (allowedValues == null) {
                errors.put(path + ".perguntaId", "Pergunta inexistente.");
                continue;
            }
            if (!seen.add(questionId)) {
                errors.put(questionId, "A pergunta foi respondida mais de uma vez.");
                continue;
            }
            if (answer.valor() == null || !allowedValues.contains(answer.valor())) {
                errors.put(questionId, "Resposta ausente ou inválida.");
                continue;
            }
            values.put(questionId, answer.valor());
        }

        for (String questionId : ALLOWED_ANSWERS.keySet()) {
            if (!values.containsKey(questionId) && !errors.containsKey(questionId)) {
                errors.put(questionId, "Responda esta pergunta.");
            }
        }
        if (answers.size() != ALLOWED_ANSWERS.size()) {
            errors.putIfAbsent("respostas", "Envie uma resposta única para cada uma das 10 perguntas.");
        }
        if (!errors.isEmpty()) {
            throw new AssistentePlantioValidationException(errors);
        }
        return values;
    }

    private boolean isRecommendedNativeCategory(Especie species) {
        String category = normalize(species.getCategoria());
        return ALLOWED_CATEGORIES.contains(category);
    }

    private boolean hasAllowedSize(Especie species, Set<String> allowedSizes, boolean overheadWiring) {
        String size = normalize(species.getPorteArvore());
        if (overheadWiring && !"PEQUENO".equals(size)) {
            return false;
        }
        return allowedSizes == null || allowedSizes.contains(size);
    }

    private EspecieRecomendadaResponse toResponse(Especie species) {
        return new EspecieRecomendadaResponse(
                species.getId(),
                species.getNomePopular(),
                species.getNomeCientifico(),
                species.getPorteArvore(),
                null
        );
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
