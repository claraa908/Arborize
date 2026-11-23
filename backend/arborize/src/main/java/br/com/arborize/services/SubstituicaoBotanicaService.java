package br.com.arborize.services;

import br.com.arborize.dtos.request.AcompanharSubstituicaoRequest;
import br.com.arborize.dtos.request.DecisaoSubstituicaoRequest;
import br.com.arborize.dtos.request.LaudoSubstituicaoRequest;
import br.com.arborize.dtos.request.ProgressoSubstituicaoRequest;
import br.com.arborize.dtos.request.SolicitacaoSubstituicaoRequest;
import br.com.arborize.dtos.response.SubstituicaoEventoResponse;
import br.com.arborize.dtos.response.SubstituicaoGestaoResponse;
import br.com.arborize.dtos.response.SubstituicaoRastreioResponse;
import br.com.arborize.entities.LaudoSupressao;
import br.com.arborize.entities.SolicitacaoSubstituicao;
import br.com.arborize.entities.SolicitacaoSubstituicaoEvento;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import br.com.arborize.exceptions.SubstituicaoBotanicaException;
import br.com.arborize.exceptions.ArmazenamentoFotografiaException;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.LaudoSupressaoRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoEventoRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoRepository;
import br.com.arborize.repositories.UsuarioRepository;
import br.com.arborize.storage.SubstituicaoBotanicaStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SubstituicaoBotanicaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubstituicaoBotanicaService.class);
    private static final long MAX_FOTO_BYTES = 2L * 1024 * 1024;
    private static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    private static final List<String> PHOTO_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final SolicitacaoSubstituicaoRepository solicitacaoRepository;
    private final SolicitacaoSubstituicaoEventoRepository eventoRepository;
    private final LaudoSupressaoRepository laudoRepository;
    private final EspecieRepository especieRepository;
    private final UsuarioRepository usuarioRepository;
    private final SubstituicaoBotanicaStorage storage;

    public SubstituicaoBotanicaService(
            SolicitacaoSubstituicaoRepository solicitacaoRepository,
            SolicitacaoSubstituicaoEventoRepository eventoRepository,
            LaudoSupressaoRepository laudoRepository,
            EspecieRepository especieRepository,
            UsuarioRepository usuarioRepository,
            SubstituicaoBotanicaStorage storage
    ) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.eventoRepository = eventoRepository;
        this.laudoRepository = laudoRepository;
        this.especieRepository = especieRepository;
        this.usuarioRepository = usuarioRepository;
        this.storage = storage;
    }

    @Transactional
    public SubstituicaoRastreioResponse criar(SolicitacaoSubstituicaoRequest request) {
        String cpf = digitsOnly(request.getCpf());
        String telefone = digitsOnly(request.getTelefone());
        if (!cpfValido(cpf)) throw invalid("cpf", "Informe um CPF válido.");
        if (telefone.length() < 10 || telefone.length() > 11) throw invalid("telefone", "Informe um telefone com DDD.");
        if (normalizeText(request.getNomeSolicitante()).split("\\s+").length < 2) throw invalid("nomeSolicitante", "Informe seu nome completo.");
        MultipartFile foto = request.getFoto();
        String fotoObjeto = null;
        if (foto != null && !foto.isEmpty()) validarFoto(foto);

        SolicitacaoSubstituicao solicitacao = new SolicitacaoSubstituicao();
        solicitacao.setProtocolo("NIM-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        solicitacao.setNomeSolicitante(normalizeText(request.getNomeSolicitante()));
        solicitacao.setCpf(cpf);
        solicitacao.setTelefone(telefone);
        solicitacao.setLogradouro(normalizeText(request.getLogradouro()));
        solicitacao.setNumeroEndereco(normalizeText(request.getNumeroEndereco()));
        solicitacao.setBairro(normalizeText(request.getBairro()));
        solicitacao.setComplemento(normalizeOptionalText(request.getComplemento()));
        solicitacao.setPontoReferencia(normalizeText(request.getPontoReferencia()));
        solicitacao.setDescricaoArvore(normalizeText(request.getDescricaoArvore()));
        solicitacao.setStatus(SubstituicaoBotanicaStatus.RECEBIDA);
        LocalDateTime now = LocalDateTime.now();
        solicitacao.setCriadaEm(now);
        solicitacao.setAtualizadaEm(now);

        if (foto != null && !foto.isEmpty()) {
            String fotoContentType = foto.getContentType().toLowerCase(Locale.ROOT);
            fotoObjeto = storage.guardar("fotos", foto, fotoContentType);
            limparObjetoSeTransacaoFalhar(fotoObjeto);
            solicitacao.setFotoObjeto(fotoObjeto);
            solicitacao.setFotoNomeOriginal(safeFilename(foto.getOriginalFilename()));
            solicitacao.setFotoContentType(fotoContentType);
            solicitacao.setFotoTamanhoBytes(foto.getSize());
        }

        solicitacaoRepository.saveAndFlush(solicitacao);
        registrarEvento(solicitacao, SubstituicaoBotanicaStatus.RECEBIDA,
                "Solicitação recebida pela AMMA.", true, null);
        List<SubstituicaoEventoResponse> eventos = eventoRepository
                .findBySolicitacaoIdAndPublicoTrueOrderByCriadoEmAsc(solicitacao.getId()).stream()
                .map(evento -> new SubstituicaoEventoResponse(evento.getStatus(), evento.getDescricao(), evento.getCriadoEm()))
                .toList();
        return rastreio(solicitacao, eventos, null);
    }

    @Transactional(readOnly = true)
    public SubstituicaoRastreioResponse acompanhar(AcompanharSubstituicaoRequest request) {
        String protocolo = request.protocolo().trim().toUpperCase(Locale.ROOT);
        String cpf = digitsOnly(request.cpf());
        SolicitacaoSubstituicao solicitacao = solicitacaoRepository.findByProtocoloAndCpf(protocolo, cpf)
                .orElseThrow(() -> new SubstituicaoBotanicaException(
                        HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Solicitação não encontrada.", Map.of()));
        List<SubstituicaoEventoResponse> eventos = eventoRepository
                .findBySolicitacaoIdAndPublicoTrueOrderByCriadoEmAsc(solicitacao.getId()).stream()
                .map(evento -> new SubstituicaoEventoResponse(evento.getStatus(), evento.getDescricao(), evento.getCriadoEm()))
                .toList();
        String especie = laudoRepository.findBySolicitacaoId(solicitacao.getId())
                .map(laudo -> laudo.getEspecieReposicao().getNomePopular()).orElse(null);
        return rastreio(solicitacao, eventos, especie);
    }

    @Transactional(readOnly = true)
    public List<SubstituicaoGestaoResponse> listar() {
        return solicitacaoRepository.findAllByOrderByCriadaEmDesc().stream().map(this::gestao).toList();
    }

    @Transactional(readOnly = true)
    public SubstituicaoGestaoResponse buscar(Long id) {
        return gestao(solicitacao(id));
    }

    @Transactional
    public SubstituicaoGestaoResponse iniciarAnalise(Long id, Long usuarioId) {
        SolicitacaoSubstituicao solicitacao = solicitacao(id);
        if (solicitacao.getStatus() != SubstituicaoBotanicaStatus.RECEBIDA) throw conflict("A solicitação não está aguardando análise.");
        atualizarStatus(solicitacao, SubstituicaoBotanicaStatus.EM_ANALISE,
                "A AMMA iniciou a análise do requerimento.", usuario(usuarioId));
        return gestao(solicitacao);
    }

    @Transactional
    public SubstituicaoGestaoResponse decidir(Long id, DecisaoSubstituicaoRequest request, Long usuarioId) {
        SolicitacaoSubstituicao solicitacao = solicitacao(id);
        if (solicitacao.getStatus() != SubstituicaoBotanicaStatus.EM_ANALISE) throw conflict("Inicie a análise antes de decidir a solicitação.");
        if (request.status() != SubstituicaoBotanicaStatus.APROVADA && request.status() != SubstituicaoBotanicaStatus.REJEITADA) {
            throw invalid("status", "A decisão deve aprovar ou rejeitar o requerimento.");
        }
        String descricao = request.justificativa() == null || request.justificativa().isBlank()
                ? (request.status() == SubstituicaoBotanicaStatus.APROVADA ? "Solicitação aprovada pela AMMA." : "Solicitação não aprovada pela AMMA.")
                : normalizeText(request.justificativa());
        atualizarStatus(solicitacao, request.status(), descricao, usuario(usuarioId));
        return gestao(solicitacao);
    }

    @Transactional
    public SubstituicaoGestaoResponse registrarProgresso(Long id, ProgressoSubstituicaoRequest request, Long usuarioId) {
        SolicitacaoSubstituicao solicitacao = solicitacao(id);
        SubstituicaoBotanicaStatus esperado = switch (solicitacao.getStatus()) {
            case LAUDO_EMITIDO -> SubstituicaoBotanicaStatus.RETIRADA_EM_ANDAMENTO;
            case RETIRADA_EM_ANDAMENTO -> SubstituicaoBotanicaStatus.REPLANTIO_EM_ANDAMENTO;
            case REPLANTIO_EM_ANDAMENTO -> SubstituicaoBotanicaStatus.CONCLUIDA;
            default -> throw conflict("A etapa atual não permite registrar este avanço.");
        };
        if (request.status() != esperado) throw invalid("status", "O avanço informado não corresponde à próxima etapa.");
        atualizarStatus(solicitacao, esperado, normalizeText(request.descricao()), usuario(usuarioId));
        return gestao(solicitacao);
    }

    @Transactional
    public SubstituicaoGestaoResponse emitirLaudo(Long id, LaudoSubstituicaoRequest request, Long usuarioId) {
        SolicitacaoSubstituicao solicitacao = solicitacao(id);
        if (solicitacao.getStatus() != SubstituicaoBotanicaStatus.APROVADA) throw conflict("A solicitação precisa estar aprovada para receber um laudo.");
        if (laudoRepository.existsBySolicitacaoId(id)) throw conflict("Já existe um laudo vinculado a esta solicitação.");
        validarPdf(request.getArquivo());
        var especie = especieRepository.findById(request.getEspecieReposicaoId())
                .orElseThrow(() -> invalid("especieReposicaoId", "Selecione uma espécie válida."));
        if (!normalizeCategory(especie.getCategoria()).equals("NATIVA DA CAATINGA")) {
            throw invalid("especieReposicaoId", "Selecione uma espécie nativa da Caatinga.");
        }
        Usuario responsavel = usuario(usuarioId);
        String objeto = storage.guardar("laudos", request.getArquivo(), "application/pdf");
        limparObjetoSeTransacaoFalhar(objeto);
        LaudoSupressao laudo = new LaudoSupressao();
        laudo.setSolicitacao(solicitacao);
        laudo.setEspecieReposicao(especie);
        laudo.setParecer(normalizeText(request.getParecer()));
        laudo.setTipoJustificativa(normalizeText(request.getTipoJustificativa()));
        laudo.setJustificativa(normalizeText(request.getJustificativa()));
        laudo.setDataVistoria(request.getDataVistoria());
        laudo.setPdfObjeto(objeto);
        laudo.setPdfNomeOriginal(safeFilename(request.getArquivo().getOriginalFilename()));
        laudo.setPdfContentType("application/pdf");
        laudo.setPdfTamanhoBytes(request.getArquivo().getSize());
        laudo.setEmitidoPor(responsavel);
        laudo.setCriadoEm(LocalDateTime.now());
        laudoRepository.saveAndFlush(laudo);
        atualizarStatus(solicitacao, SubstituicaoBotanicaStatus.LAUDO_EMITIDO,
                "Laudo técnico de supressão emitido pela AMMA.", responsavel);
        return gestao(solicitacao);
    }

    @Transactional(readOnly = true)
    public byte[] recuperarLaudo(Long id) {
        LaudoSupressao laudo = laudoRepository.findBySolicitacaoId(id)
                .orElseThrow(() -> new SubstituicaoBotanicaException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Laudo não encontrado.", Map.of()));
        return storage.recuperar(laudo.getPdfObjeto(), MAX_PDF_BYTES);
    }

    @Transactional(readOnly = true)
    public FotoResponse recuperarFoto(Long id) {
        SolicitacaoSubstituicao solicitacao = solicitacao(id);
        if (solicitacao.getFotoObjeto() == null) throw new SubstituicaoBotanicaException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Fotografia não encontrada.", Map.of());
        return new FotoResponse(solicitacao.getFotoContentType(), storage.recuperar(solicitacao.getFotoObjeto(), MAX_FOTO_BYTES));
    }

    public record FotoResponse(String contentType, byte[] bytes) {}

    private SubstituicaoGestaoResponse gestao(SolicitacaoSubstituicao solicitacao) {
        LaudoSupressao laudo = laudoRepository.findBySolicitacaoId(solicitacao.getId()).orElse(null);
        return new SubstituicaoGestaoResponse(
                solicitacao.getId(), solicitacao.getProtocolo(), solicitacao.getNomeSolicitante(), solicitacao.getCpf(),
                solicitacao.getTelefone(), solicitacao.getLogradouro(), solicitacao.getNumeroEndereco(), solicitacao.getBairro(),
                solicitacao.getComplemento(), solicitacao.getPontoReferencia(), solicitacao.getDescricaoArvore(),
                solicitacao.getFotoNomeOriginal(), solicitacao.getStatus(), solicitacao.getCriadaEm(), solicitacao.getAtualizadaEm(),
                laudo == null ? null : laudo.getParecer(), laudo == null ? null : laudo.getTipoJustificativa(),
                laudo == null ? null : laudo.getJustificativa(), laudo == null ? null : laudo.getEspecieReposicao().getNomePopular(),
                laudo == null ? null : laudo.getDataVistoria(), laudo == null ? null : laudo.getPdfNomeOriginal());
    }

    private SubstituicaoRastreioResponse rastreio(SolicitacaoSubstituicao solicitacao, List<SubstituicaoEventoResponse> eventos, String especie) {
        return new SubstituicaoRastreioResponse(solicitacao.getProtocolo(), solicitacao.getStatus(), solicitacao.getCriadaEm(),
                solicitacao.getBairro(), solicitacao.getDescricaoArvore(), especie, eventos);
    }

    private SolicitacaoSubstituicao solicitacao(Long id) {
        return solicitacaoRepository.findById(id).orElseThrow(() -> new SubstituicaoBotanicaException(
                HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Solicitação não encontrada.", Map.of()));
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new SubstituicaoBotanicaException(
                HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Sessão inválida.", Map.of()));
    }

    private void atualizarStatus(SolicitacaoSubstituicao solicitacao, SubstituicaoBotanicaStatus status, String descricao, Usuario responsavel) {
        solicitacao.setStatus(status);
        solicitacao.setAtualizadaEm(LocalDateTime.now());
        solicitacaoRepository.save(solicitacao);
        registrarEvento(solicitacao, status, descricao, true, responsavel);
    }

    private void registrarEvento(SolicitacaoSubstituicao solicitacao, SubstituicaoBotanicaStatus status, String descricao, boolean publico, Usuario responsavel) {
        SolicitacaoSubstituicaoEvento evento = new SolicitacaoSubstituicaoEvento();
        evento.setSolicitacao(solicitacao);
        evento.setStatus(status);
        evento.setDescricao(descricao);
        evento.setPublico(publico);
        evento.setResponsavel(responsavel);
        evento.setCriadoEm(LocalDateTime.now());
        eventoRepository.saveAndFlush(evento);
    }

    private void validarFoto(MultipartFile foto) {
        if (foto.getSize() > MAX_FOTO_BYTES) throw invalid("foto", "A fotografia deve ter no máximo 2 MB.");
        String type = foto.getContentType();
        String filename = safeFilename(foto.getOriginalFilename()).toLowerCase(Locale.ROOT);
        boolean validType = type != null && PHOTO_TYPES.contains(type.toLowerCase(Locale.ROOT));
        boolean validExtension = filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".webp");
        if (!validType || !validExtension) throw invalid("foto", "Envie uma imagem JPG, PNG ou WebP.");
    }

    private void validarPdf(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw invalid("arquivo", "Selecione o PDF assinado.");
        if (arquivo.getSize() > MAX_PDF_BYTES) throw invalid("arquivo", "O PDF deve ter no máximo 10 MB.");
        String filename = safeFilename(arquivo.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(arquivo.getContentType())) {
            throw invalid("arquivo", "Envie um arquivo PDF válido.");
        }
        try {
            byte[] header;
            try (var input = arquivo.getInputStream()) {
                header = input.readNBytes(5);
            }
            if (!new String(header, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) throw invalid("arquivo", "O conteúdo do arquivo não corresponde a um PDF.");
        } catch (java.io.IOException ex) {
            throw invalid("arquivo", "Não foi possível ler o PDF enviado.");
        }
    }

    private String normalizeCategory(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) { return value.trim().replaceAll("\\s+", " "); }
    private String normalizeOptionalText(String value) { return value == null || value.isBlank() ? null : normalizeText(value); }
    private String digitsOnly(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private String safeFilename(String value) {
        if (value == null || value.isBlank()) return "documento";
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private boolean cpfValido(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        int first = 0;
        int second = 0;
        for (int index = 0; index < 9; index++) {
            int digit = cpf.charAt(index) - '0';
            first += digit * (10 - index);
            second += digit * (11 - index);
        }
        int firstDigit = (first * 10) % 11;
        if (firstDigit == 10) firstDigit = 0;
        second += firstDigit * 2;
        int secondDigit = (second * 10) % 11;
        if (secondDigit == 10) secondDigit = 0;
        return firstDigit == cpf.charAt(9) - '0' && secondDigit == cpf.charAt(10) - '0';
    }

    private void limparObjetoSeTransacaoFalhar(String objeto) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return;
                try {
                    storage.remover(objeto);
                } catch (ArmazenamentoFotografiaException ex) {
                    LOGGER.warn("Falha ao limpar objeto de substituição após rollback.");
                }
            }
        });
    }

    private SubstituicaoBotanicaException invalid(String field, String message) {
        return new SubstituicaoBotanicaException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, Map.of(field, message));
    }

    private SubstituicaoBotanicaException conflict(String message) {
        return new SubstituicaoBotanicaException(HttpStatus.CONFLICT, "INVALID_TRANSITION", message, Map.of());
    }
}
