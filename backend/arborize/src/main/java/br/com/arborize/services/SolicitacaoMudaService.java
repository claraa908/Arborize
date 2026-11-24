package br.com.arborize.services;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
import br.com.arborize.dtos.request.AcompanharSolicitacaoMudaRequest;
import br.com.arborize.dtos.response.ConversaoReciclagemResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.enums.SolicitacaoMudaStatus;
import br.com.arborize.exceptions.SolicitacaoMudaException;
import br.com.arborize.mappers.SolicitacaoMudaMapper;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SolicitacaoMudaService {
    private static final Pattern PROTOCOLO_PATTERN = Pattern.compile("SOL-\\d{4}-(\\d{4,})");
    private final EspecieRepository especieRepository;
    private final SolicitacaoMudaRepository solicitacaoRepository;
    private final SolicitacaoMudaMapper mapper;
    private final int maxMudas;
    private final int garrafasPorMuda;

    public SolicitacaoMudaService(
            EspecieRepository especieRepository,
            SolicitacaoMudaRepository solicitacaoRepository,
            SolicitacaoMudaMapper mapper,
            @Value("${arborize.solicitacao.max-mudas:3}") int maxMudas,
            @Value("${arborize.solicitacao.garrafas-por-muda:5}") int garrafasPorMuda
    ) {
        this.especieRepository = especieRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.mapper = mapper;
        this.maxMudas = Math.min(maxMudas, 3);
        this.garrafasPorMuda = garrafasPorMuda;
    }

    public ConversaoReciclagemResponse calcularConversao(int quantidade) {
        validateQuantity(quantidade);
        return new ConversaoReciclagemResponse(quantidade, garrafasPorMuda, Math.multiplyExact(quantidade, garrafasPorMuda));
    }

    @Transactional
    public SolicitacaoMudaResponse criar(SolicitacaoMudaRequest request) {
        validateQuantity(request.quantidadeMudas());
        Especie especie = especieRepository.findById(request.especieId())
                .orElseThrow(() -> new SolicitacaoMudaException(
                        HttpStatus.NOT_FOUND,
                        "ESPECIE_NOT_FOUND",
                        "A espécie selecionada não foi encontrada.",
                        Map.of("especieId", "Selecione uma espécie válida.")
                ));

        LocalDateTime createdAt = LocalDateTime.now();
        SolicitacaoMuda solicitacao = new SolicitacaoMuda();
        solicitacao.setEspecie(especie);
        solicitacao.setQuantidadeMudas(request.quantidadeMudas());
        solicitacao.setTotalRecicaveis(Math.multiplyExact(request.quantidadeMudas(), garrafasPorMuda));
        solicitacao.setLogradouro(normalizeText(request.logradouro()));
        solicitacao.setNumeroEndereco(normalizeText(request.numeroEndereco()));
        solicitacao.setBairro(normalizeText(request.bairro()));
        solicitacao.setComplemento(normalizeOptionalText(request.complemento()));
        solicitacao.setPontoReferencia(normalizeText(request.pontoReferencia()));
        solicitacao.setNomeSolicitante(normalizeText(request.nomeSolicitante()));
        solicitacao.setCpf(digitsOnly(request.cpf()));
        solicitacao.setTelefone(digitsOnly(request.telefone()));
        solicitacao.setStatus(SolicitacaoMudaStatus.PENDENTE_RECICLAVEIS);
        solicitacao.setCriadaEm(createdAt);

        SolicitacaoMuda saved = solicitacaoRepository.save(solicitacao);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SolicitacaoMudaResponse acompanhar(AcompanharSolicitacaoMudaRequest request) {
        String protocolo = request.protocolo().trim().toUpperCase(Locale.ROOT);
        Matcher matcher = PROTOCOLO_PATTERN.matcher(protocolo);
        if (!matcher.matches()) throw solicitacaoNaoEncontrada();

        long id;
        try {
            id = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw solicitacaoNaoEncontrada();
        }

        SolicitacaoMuda solicitacao = solicitacaoRepository.findById(id)
                .filter(item -> mapper.toResponse(item).protocolo().equals(protocolo))
                .filter(item -> item.getCpf().equals(digitsOnly(request.cpf())))
                .orElseThrow(this::solicitacaoNaoEncontrada);
        return mapper.toResponse(solicitacao);
    }

    private SolicitacaoMudaException solicitacaoNaoEncontrada() {
        return new SolicitacaoMudaException(
                HttpStatus.NOT_FOUND,
                "SOLICITACAO_NOT_FOUND",
                "NÃ£o encontramos uma solicitaÃ§Ã£o com esses dados.",
                Map.of()
        );
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > maxMudas) {
            throw new SolicitacaoMudaException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_QUANTITY",
                    "A quantidade de mudas precisa estar dentro do limite permitido.",
                    Map.of("quantidadeMudas", "Informe uma quantidade entre 1 e " + maxMudas + ".")
            );
        }
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : normalizeText(value);
    }

    private String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }
}
