package br.com.arborize.services;

import br.com.arborize.dtos.request.MovimentacaoMudaRequestDTO;
import br.com.arborize.dtos.response.MovimentacaoMudaResumoResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.MovimentacaoMuda;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.exceptions.EstoqueInsuficienteException;
import br.com.arborize.exceptions.MovimentacaoMudaInvalidaException;
import br.com.arborize.mappers.MovimentacaoMudaMapper;
import br.com.arborize.repositories.DocumentRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MovimentacaoMudaRepository;
import br.com.arborize.validation.ComprovanteMovimentacaoValidator;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class MovimentacaoMudaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovimentacaoMudaService.class);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Fortaleza");

    private final MovimentacaoMudaRepository movimentacaoRepository;
    private final EspecieRepository especieRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final ComprovanteMovimentacaoValidator comprovanteValidator;
    private final MovimentacaoMudaMapper mapper;
    private final Clock clock;

    public MovimentacaoMudaService(
            MovimentacaoMudaRepository movimentacaoRepository,
            EspecieRepository especieRepository,
            DocumentRepository documentRepository,
            DocumentService documentService,
            ComprovanteMovimentacaoValidator comprovanteValidator,
            MovimentacaoMudaMapper mapper) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.especieRepository = especieRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.comprovanteValidator = comprovanteValidator;
        this.mapper = mapper;
        this.clock = Clock.system(ZONA_NEGOCIO);
    }

    @Transactional(readOnly = true)
    public Page<br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO> listar(
            TipoMovimentacaoMuda tipo,
            Long especieId,
            LocalDate dataInicio,
            LocalDate dataFim,
            String busca,
            Pageable pageable) {
        validarPeriodo(dataInicio, dataFim);
        Specification<MovimentacaoMuda> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (tipo != null) predicates.add(builder.equal(root.get("tipo"), tipo));
            if (especieId != null) predicates.add(builder.equal(root.get("especie").get("id"), especieId));
            if (dataInicio != null) predicates.add(builder.greaterThanOrEqualTo(root.get("data"), dataInicio));
            if (dataFim != null) predicates.add(builder.lessThanOrEqualTo(root.get("data"), dataFim));
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("especie").get("nomePopular")), termo),
                        builder.like(builder.lower(root.get("especie").get("nomeCientifico")), termo),
                        builder.like(builder.lower(root.get("destino")), termo),
                        builder.like(builder.lower(root.get("responsavel")), termo)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return movimentacaoRepository.findAll(specification, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO buscar(Long id) {
        return mapper.toResponse(movimentacaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimentação não encontrada.")));
    }

    @Transactional(readOnly = true)
    public MovimentacaoMudaResumoResponseDTO resumo() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
        long saidas = movimentacaoRepository.somarQuantidadeNoPeriodo(TipoMovimentacaoMuda.SAIDA_PLANTIO, inicio, fim);
        long perdas = movimentacaoRepository.somarQuantidadeNoPeriodo(TipoMovimentacaoMuda.BAIXA_PERDA, inicio, fim);
        return new MovimentacaoMudaResumoResponseDTO(saidas, perdas);
    }

    @Transactional
    public br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO criar(MovimentacaoMudaRequestDTO request) {
        validar(request);
        Especie especie = bloquearEspecies(List.of(request.getEspecieId())).get(request.getEspecieId());
        aplicarSaldo(especie, efeito(request.getTipo(), request.getQuantidade()));

        DocumentEntities comprovante = enviarComprovante(request.getComprovante());
        if (comprovante != null) agendarLimpezaComprovantes(comprovante, null);
        MovimentacaoMuda movimento = new MovimentacaoMuda();
        preencher(movimento, request, especie, comprovante);
        MovimentacaoMuda criada = movimentacaoRepository.saveAndFlush(movimento);
        return mapper.toResponse(criada);
    }

    @Transactional
    public br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO atualizar(Long id, MovimentacaoMudaRequestDTO request) {
        validar(request);
        MovimentacaoMuda movimento = movimentacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimentação não encontrada."));
        Long especieAntigaId = movimento.getEspecie().getId();
        Long especieNovaId = request.getEspecieId();
        Map<Long, Especie> especies = bloquearEspecies(List.of(especieAntigaId, especieNovaId));
        Especie especieNova = especies.get(especieNovaId);

        Map<Long, Long> deltas = new LinkedHashMap<>();
        deltas.merge(especieAntigaId, -efeito(movimento.getTipo(), movimento.getQuantidade()), Long::sum);
        deltas.merge(especieNovaId, efeito(request.getTipo(), request.getQuantidade()), Long::sum);
        for (Map.Entry<Long, Long> delta : deltas.entrySet()) {
            aplicarSaldo(especies.get(delta.getKey()), delta.getValue());
        }

        DocumentEntities comprovanteAnterior = movimento.getComprovante();
        DocumentEntities novoComprovante = enviarComprovante(request.getComprovante());
        if (novoComprovante != null) {
            agendarLimpezaComprovantes(novoComprovante, comprovanteAnterior);
            movimento.setComprovante(novoComprovante);
        }
        preencher(movimento, request, especieNova, movimento.getComprovante());
        MovimentacaoMuda atualizada = movimentacaoRepository.saveAndFlush(movimento);
        return mapper.toResponse(atualizada);
    }

    @Transactional(readOnly = true)
    public DocumentoComprovante buscarComprovante(Long id) {
        MovimentacaoMuda movimento = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimentação não encontrada."));
        DocumentEntities documento = movimento.getComprovante();
        if (documento == null) throw new EntityNotFoundException("Comprovante não encontrado.");
        return new DocumentoComprovante(
                documentService.getFileBytes(documento.getFilePath()),
                documento.getContentType(),
                documento.getOriginalFilename());
    }

    private Map<Long, Especie> bloquearEspecies(List<Long> ids) {
        Map<Long, Especie> bloqueadas = new LinkedHashMap<>();
        for (Long id : new TreeSet<>(ids)) {
            Especie especie = especieRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new EntityNotFoundException("Espécie não encontrada."));
            bloqueadas.put(id, especie);
        }
        return bloqueadas;
    }

    private void validar(MovimentacaoMudaRequestDTO request) {
        if (request.getTipo() == null || request.getEspecieId() == null || request.getQuantidade() == null
                || request.getQuantidade() <= 0 || request.getData() == null) {
            throw new MovimentacaoMudaInvalidaException("Informe tipo, espécie, quantidade positiva e data válida.");
        }
        if (request.getTipo() == TipoMovimentacaoMuda.SAIDA_PLANTIO
                && (request.getDestino() == null || request.getDestino().isBlank())) {
            throw new MovimentacaoMudaInvalidaException("Informe a finalidade ou o destino da saída para plantio.");
        }
        comprovanteValidator.validar(request.getComprovante());
    }

    private void aplicarSaldo(Especie especie, long delta) {
        long atual = especie.getMudasDisponiveis() == null ? 0L : especie.getMudasDisponiveis();
        long novoSaldo = atual + delta;
        if (novoSaldo < 0) throw new EstoqueInsuficienteException();
        if (novoSaldo > Integer.MAX_VALUE) throw new MovimentacaoMudaInvalidaException("A quantidade excede o limite do estoque.");
        especie.setMudasDisponiveis((int) novoSaldo);
    }

    private long efeito(TipoMovimentacaoMuda tipo, Integer quantidade) {
        return tipo == TipoMovimentacaoMuda.ENTRADA_GERMINACAO ? quantidade.longValue() : -quantidade.longValue();
    }

    private void preencher(MovimentacaoMuda movimento, MovimentacaoMudaRequestDTO request, Especie especie, DocumentEntities comprovante) {
        movimento.setTipo(request.getTipo());
        movimento.setEspecie(especie);
        movimento.setQuantidade(request.getQuantidade());
        movimento.setData(request.getData());
        movimento.setDestino(normalizar(request.getDestino()));
        movimento.setResponsavel(normalizar(request.getResponsavel()));
        movimento.setObservacoes(normalizar(request.getObservacoes()));
        movimento.setComprovante(comprovante);
    }

    private DocumentEntities enviarComprovante(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;
        DocumentEntities documento = null;
        try {
            documento = documentService.uploadAndCreateDocument(arquivo);
            return documentRepository.save(documento);
        } catch (RuntimeException exception) {
            if (documento != null && documento.getFilePath() != null) {
                try {
                    documentService.deleteStoredObject(documento.getFilePath());
                } catch (RuntimeException cleanupException) {
                    LOGGER.warn("Falha ao limpar comprovante após erro de persistência.");
                }
            }
            throw new IllegalStateException("Não foi possível armazenar o comprovante.", exception);
        }
    }

    private void agendarLimpezaComprovantes(DocumentEntities novo, DocumentEntities anterior) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED && anterior != null) {
                    removerComprovante(anterior);
                } else if (status != STATUS_COMMITTED) {
                    try {
                        documentService.deleteStoredObject(novo.getFilePath());
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Falha ao limpar objeto de comprovante após rollback.");
                    }
                }
            }
        });
    }

    private void removerComprovante(DocumentEntities anterior) {
        try {
            documentRepository.deleteById(anterior.getId());
            documentService.deleteStoredObject(anterior.getFilePath());
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha ao limpar comprovante substituído após commit.");
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new MovimentacaoMudaInvalidaException("A data inicial do filtro deve ser anterior ou igual à data final.");
        }
    }

    public record DocumentoComprovante(byte[] conteudo, String contentType, String nomeArquivo) { }
}
