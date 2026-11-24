package br.com.arborize.services;

import br.com.arborize.dtos.request.TermoCompensacaoRequestDTO;
import br.com.arborize.dtos.response.CompensacaoResponseDTO;
import br.com.arborize.entities.EntregaCompensacao;
import br.com.arborize.entities.TermoCompensacao;
import br.com.arborize.enums.CompensacaoStatus;
import br.com.arborize.exceptions.CompensacaoException;
import br.com.arborize.repositories.EntregaCompensacaoRepository;
import br.com.arborize.repositories.TermoCompensacaoRepository;
import br.com.arborize.storage.CompensacaoStorage;
import br.com.arborize.validation.CompensacaoArquivoValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CompensacaoService {
    private static final Logger LOG = LoggerFactory.getLogger(CompensacaoService.class);
    private final TermoCompensacaoRepository termos;
    private final EntregaCompensacaoRepository entregas;
    private final CompensacaoStorage storage;
    private final CompensacaoArquivoValidator validator;

    public CompensacaoService(TermoCompensacaoRepository termos, EntregaCompensacaoRepository entregas,
                              CompensacaoStorage storage, CompensacaoArquivoValidator validator) {
        this.termos = termos;
        this.entregas = entregas;
        this.storage = storage;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<CompensacaoResponseDTO> listar() {
        return termos.findAllByOrderByCriadoEmDescIdDesc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CompensacaoResponseDTO consultar(long id) {
        return dto(buscar(id));
    }

    @Transactional
    public CompensacaoResponseDTO criar(TermoCompensacaoRequestDTO dados, MultipartFile arquivo) {
        var termoPdf = validator.validarTermo(arquivo);
        String key = "compensacao/termos/" + UUID.randomUUID() + ".pdf";
        prepararCompensacaoRollback(key);
        storage.enviar(key, termoPdf.conteudo(), termoPdf.contentType());
        var termo = new TermoCompensacao(normalizar(dados.razaoSocial()), dados.cnpj(), normalizarOpcional(dados.numeroTermo()),
                dados.dataAssinatura(), dados.prazoEntrega(), dados.quantidadeAcordada(), key,
                termoPdf.contentType(), termoPdf.nome(), termoPdf.conteudo().length);
        return dto(termos.saveAndFlush(termo));
    }

    @Transactional
    public CompensacaoResponseDTO atualizar(long id, TermoCompensacaoRequestDTO dados) {
        var termo = bloquear(id);
        termo.atualizarDados(normalizar(dados.razaoSocial()), dados.cnpj(), normalizarOpcional(dados.numeroTermo()),
                dados.dataAssinatura(), dados.prazoEntrega(), dados.quantidadeAcordada());
        return dto(termos.saveAndFlush(termo));
    }

    @Transactional
    public CompensacaoResponseDTO atualizarStatus(long id, CompensacaoStatus status) {
        if (status == null) throw new CompensacaoException(HttpStatus.BAD_REQUEST, "status-invalido", "Selecione um estado válido.");
        var termo = bloquear(id);
        termo.atualizarStatus(status);
        return dto(termos.saveAndFlush(termo));
    }

    @Transactional
    public CompensacaoResponseDTO adicionarEntrega(long id, Integer quantidade, MultipartFile recibo) {
        if (quantidade == null || quantidade <= 0) throw new CompensacaoException(HttpStatus.BAD_REQUEST, "quantidade-invalida", "A quantidade da entrega deve ser um inteiro maior que zero.");
        var arquivo = validator.validarRecibo(recibo);
        var termo = bloquear(id);
        String key = "compensacao/recibos/" + id + "/" + UUID.randomUUID() + extensao(arquivo.contentType());
        prepararCompensacaoRollback(key);
        storage.enviar(key, arquivo.conteudo(), arquivo.contentType());
        entregas.saveAndFlush(new EntregaCompensacao(termo, quantidade, key, arquivo.nome(), arquivo.contentType(), arquivo.conteudo().length));
        long acumulado = entregas.somarQuantidadesPorTermo(id);
        CompensacaoStatus estado = acumulado <= 0 ? CompensacaoStatus.EM_ABERTO
                : acumulado < termo.getQuantidadeAcordada() ? CompensacaoStatus.PARCIALMENTE_DOADO
                : CompensacaoStatus.TOTALMENTE_DOADO;
        termo.atualizarStatus(estado);
        return dto(termos.saveAndFlush(termo));
    }

    @Transactional(readOnly = true)
    public Documento recuperarTermo(long id) {
        var termo = buscar(id);
        return new Documento(storage.recuperar(termo.getTermoObjeto(), CompensacaoArquivoValidator.TERMO_MAX_BYTES),
                termo.getTermoContentType(), termo.getTermoNome());
    }

    @Transactional(readOnly = true)
    public Documento recuperarRecibo(long id, long entregaId) {
        buscar(id);
        var entrega = entregas.findByIdAndTermoId(entregaId, id)
                .orElseThrow(() -> new EntityNotFoundException("Recibo não encontrado."));
        return new Documento(storage.recuperar(entrega.getReciboObjeto(), CompensacaoArquivoValidator.RECIBO_MAX_BYTES),
                entrega.getReciboContentType(), entrega.getReciboNome());
    }

    private CompensacaoResponseDTO dto(TermoCompensacao termo) {
        var historico = entregas.findAllByTermoIdOrderByCriadoEmAscIdAsc(termo.getId()).stream()
                .map(e -> new CompensacaoResponseDTO.EntregaResponseDTO(e.getId(), e.getQuantidade(), e.getReciboNome(),
                        e.getReciboContentType(), e.getReciboSizeBytes(), e.getCriadoEm())).toList();
        long total = historico.stream().mapToLong(CompensacaoResponseDTO.EntregaResponseDTO::quantidade).sum();
        return new CompensacaoResponseDTO(termo.getId(), termo.getRazaoSocial(), termo.getCnpj(), termo.getNumeroTermo(),
                termo.getDataAssinatura(), termo.getPrazoEntrega(), termo.getQuantidadeAcordada(), total,
                termo.getStatus(), termo.getTermoNome(), termo.getCriadoEm(), historico);
    }

    private TermoCompensacao buscar(long id) {
        if (id <= 0) throw new CompensacaoException(HttpStatus.BAD_REQUEST, "id-invalido", "O identificador deve ser positivo.");
        return termos.findById(id).orElseThrow(() -> new EntityNotFoundException("Termo de compensação não encontrado."));
    }

    private TermoCompensacao bloquear(long id) {
        if (id <= 0) throw new CompensacaoException(HttpStatus.BAD_REQUEST, "id-invalido", "O identificador deve ser positivo.");
        return termos.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Termo de compensação não encontrado."));
    }

    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizarOpcional(String value) {
        String clean = normalizar(value);
        return clean == null || clean.isBlank() ? null : clean;
    }

    private String extensao(String contentType) {
        return switch (contentType) { case "application/pdf" -> ".pdf"; case "image/png" -> ".png"; default -> ".jpg"; };
    }

    private void prepararCompensacaoRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try { storage.remover(key); }
                    catch (RuntimeException ex) { LOG.error("Compensação de armazenamento pendente ({})", ex.getClass().getSimpleName()); }
                } else if (status == STATUS_UNKNOWN) {
                    LOG.error("Resultado transacional desconhecido; reconciliar documento da compensação");
                }
            }
        });
    }

    public record Documento(byte[] conteudo, String contentType, String nome) { }
}
