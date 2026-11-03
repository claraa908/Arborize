package br.com.arborize.services;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
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

@Service
public class SolicitacaoMudaService {
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
