package br.com.arborize.services;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
import br.com.arborize.dtos.request.AcompanharSolicitacaoMudaRequest;
import br.com.arborize.dtos.response.ConversaoReciclagemResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaGestaoResponse;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.enums.SolicitacaoMudaStatus;
import br.com.arborize.exceptions.SolicitacaoMudaException;
import br.com.arborize.mappers.SolicitacaoMudaMapper;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
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
        if (request.localPlantio() != null) {
            solicitacao.setLocalPlantio(SolicitacaoMuda.LocalPlantio.valueOf(request.localPlantio()));
        }
        solicitacao.setStatus(SolicitacaoMudaStatus.PENDENTE_RECICLAVEIS);
        solicitacao.setCriadaEm(createdAt);

        SolicitacaoMuda saved = solicitacaoRepository.save(solicitacao);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SolicitacaoMudaResponse acompanhar(AcompanharSolicitacaoMudaRequest request) {
        SolicitacaoMuda solicitacao = buscarPorProtocolo(request.protocolo(), false)
                .filter(item -> item.getCpf().equals(digitsOnly(request.cpf())))
                .orElseThrow(this::solicitacaoNaoEncontrada);
        return mapper.toResponse(solicitacao);
    }

    @Transactional(readOnly = true)
    public SolicitacaoMudaGestaoResponse.Pagina listar(String protocolo, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new SolicitacaoMudaException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "Informe página a partir de zero e tamanho entre 1 e 100.", Map.of());
        }
        if (protocolo != null && !protocolo.isBlank()) {
            var item = buscarPorProtocolo(protocolo, false).map(mapper::toCartao);
            return new SolicitacaoMudaGestaoResponse.Pagina(page == 0 ? item.stream().toList() : List.of(),
                    page, item.isPresent() ? 1 : 0);
        }
        Page<SolicitacaoMudaGestaoResponse.Cartao> resultado = solicitacaoRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadaEm", "id")))
                .map(mapper::toCartao);
        return SolicitacaoMudaGestaoResponse.Pagina.de(resultado);
    }

    @Transactional(readOnly = true)
    public SolicitacaoMudaGestaoResponse detalhar(String protocolo) {
        return buscarPorProtocolo(protocolo, false).map(mapper::toGestao).orElseThrow(this::solicitacaoNaoEncontrada);
    }

    public record DocumentoRetirada(String protocolo, byte[] pdf) {}

    @Transactional(readOnly = true)
    public DocumentoRetirada emitirDocumento(String protocolo) {
        SolicitacaoMuda item = buscarPorProtocolo(protocolo, false).orElseThrow(this::solicitacaoNaoEncontrada);
        if (item.getStatus() != SolicitacaoMudaStatus.APROVADA) {
            throw new SolicitacaoMudaException(HttpStatus.CONFLICT, "DOCUMENTO_INDISPONIVEL",
                    "O documento de retirada está disponível para solicitações aprovadas e aguardando retirada.", Map.of());
        }
        String identificador = mapper.protocolo(item);
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            pdf.addPage(pagina);
            try (PDPageContentStream texto = new PDPageContentStream(pdf, pagina)) {
                texto.beginText();
                texto.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 17);
                texto.newLineAtOffset(48, 780);
                texto.showText("AMMA de Quixadá");
                texto.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13);
                texto.setLeading(23);
                for (String linha : List.of("Autorização de retirada de mudas", "Protocolo: " + identificador,
                        "Quantidade de mudas autorizada: " + item.getQuantidadeMudas(),
                        "Apresente este documento no laboratório do IFCE.",
                        "O IFCE deve conferir a solicitação pelo protocolo antes da entrega.")) {
                    texto.newLine();
                    texto.showText(linha);
                }
                texto.endText();
            }
            pdf.save(output);
            return new DocumentoRetirada(identificador, output.toByteArray());
        } catch (IOException exception) {
            throw new SolicitacaoMudaException(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENTO_FALHA",
                    "Não foi possível emitir o documento de retirada.", Map.of());
        }
    }

    @Transactional
    public SolicitacaoMudaGestaoResponse registrarRecebimento(String protocolo, int quantidadeRecebida) {
        SolicitacaoMuda item = buscarPendenteParaDecisao(protocolo);
        if (quantidadeRecebida < item.getTotalRecicaveis()) {
            String mensagem = "A quantidade recebida deve atingir a quantidade esperada de garrafas PET.";
            throw new SolicitacaoMudaException(HttpStatus.BAD_REQUEST, "QUANTIDADE_INSUFICIENTE",
                    mensagem, Map.of("quantidadeRecebida", mensagem));
        }
        item.setQuantidadeRecebida(quantidadeRecebida);
        item.setStatus(SolicitacaoMudaStatus.APROVADA);
        return mapper.toGestao(item);
    }

    @Transactional
    public SolicitacaoMudaGestaoResponse rejeitar(String protocolo, String motivo) {
        SolicitacaoMuda item = buscarPendenteParaDecisao(protocolo);
        item.setMotivoRejeicao(motivo.strip());
        item.setStatus(SolicitacaoMudaStatus.REJEITADA);
        return mapper.toGestao(item);
    }

    private SolicitacaoMuda buscarPendenteParaDecisao(String protocolo) {
        SolicitacaoMuda item = buscarPorProtocolo(protocolo, true).orElseThrow(this::solicitacaoNaoEncontrada);
        if (item.getStatus() != SolicitacaoMudaStatus.PENDENTE_RECICLAVEIS) {
            throw new SolicitacaoMudaException(HttpStatus.CONFLICT, "SOLICITACAO_JA_PROCESSADA",
                    "Esta solicitação já foi processada. Recarregue os detalhes para ver a situação atual.", Map.of());
        }
        return item;
    }

    private Optional<SolicitacaoMuda> buscarPorProtocolo(String informado, boolean paraDecisao) {
        String protocolo = informado.strip().toUpperCase(Locale.ROOT);
        Matcher matcher = PROTOCOLO_PATTERN.matcher(protocolo);
        if (!matcher.matches()) return Optional.empty();
        long id;
        try {
            id = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        return (paraDecisao ? solicitacaoRepository.buscarParaDecisao(id) : solicitacaoRepository.findById(id))
                .filter(item -> mapper.protocolo(item).equals(protocolo));
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
