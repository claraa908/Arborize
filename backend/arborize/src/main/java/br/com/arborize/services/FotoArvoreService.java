package br.com.arborize.services;

import br.com.arborize.dtos.response.*;
import br.com.arborize.entities.*;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.repositories.*;
import br.com.arborize.storage.FotoArvoreStorage;
import br.com.arborize.validation.FotoArvoreValidator;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Service
public class FotoArvoreService {
    private static final Logger LOG = LoggerFactory.getLogger(FotoArvoreService.class);
    private final ArvoreRepository arvores;
    private final FotoArvoreRepository fotos;
    private final FotoArvoreStorage storage;
    private final FotoArvoreValidator validator;

    public FotoArvoreService(ArvoreRepository arvores, FotoArvoreRepository fotos,
                            FotoArvoreStorage storage, FotoArvoreValidator validator) {
        this.arvores = arvores;
        this.fotos = fotos;
        this.storage = storage;
        this.validator = validator;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FotoArvoreResponseDTO adicionar(Long arvoreId, MultipartFile arquivo) {
        exigirTransacao();
        validarId(arvoreId);
        var imagem = validator.validar(arquivo);
        Arvore arvore = bloquearArvore(arvoreId);
        if (fotos.countByArvoreId(arvoreId) >= 3) {
            throw new EntradaInventarioInvalidaException("fotografias", "Cada árvore pode possuir até três fotografias.");
        }
        String objeto = "inventario/arvores/" + arvoreId + "/" + UUID.randomUUID() + "." + imagem.extensao();
        // Registrado antes do envio: um timeout pode ocorrer depois de o objeto ter sido recebido.
        compensarRollback(() -> storage.remover(objeto), objeto);
        storage.enviar(objeto, imagem.conteudo(), imagem.contentType());
        return dto(fotos.saveAndFlush(new FotoArvore(arvore, objeto, imagem.contentType(), imagem.conteudo().length)));
    }

    @Transactional(readOnly = true)
    public List<FotoArvoreResponseDTO> listar(Long arvoreId) {
        verificarArvore(arvoreId);
        return fotos.findAllByArvoreIdOrderByIdAsc(arvoreId).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public FotoArvoreConteudoDTO recuperar(Long arvoreId, Long fotoId) {
        verificarArvore(arvoreId);
        FotoArvore foto = localizar(arvoreId, fotoId);
        return new FotoArvoreConteudoDTO(dto(foto), storage.recuperar(foto.getObjeto()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remover(Long arvoreId, Long fotoId) {
        exigirTransacao();
        validarId(arvoreId);
        bloquearArvore(arvoreId);
        FotoArvore foto = localizar(arvoreId, fotoId);
        // Backup limitado a 2 MB para restaurar o objeto se o banco fizer rollback após a remoção.
        byte[] backup = storage.recuperar(foto.getObjeto());
        compensarRollback(() -> storage.enviar(foto.getObjeto(), backup, foto.getContentType()), foto.getObjeto());
        storage.remover(foto.getObjeto());
        fotos.delete(foto);
        fotos.flush();
    }

    private void verificarArvore(Long arvoreId) {
        validarId(arvoreId);
        arvores.findById(arvoreId).orElseThrow(() -> new EntityNotFoundException("Árvore não encontrada."));
    }

    private Arvore bloquearArvore(Long arvoreId) {
        return arvores.findByIdParaFotografias(arvoreId)
                .orElseThrow(() -> new EntityNotFoundException("Árvore não encontrada."));
    }

    private FotoArvore localizar(Long arvoreId, Long fotoId) {
        validarId(fotoId);
        return fotos.findByIdAndArvoreId(fotoId, arvoreId)
                .orElseThrow(() -> new EntityNotFoundException("Fotografia não encontrada para esta árvore."));
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new EntradaInventarioInvalidaException("id", "O identificador deve ser positivo.");
    }

    private FotoArvoreResponseDTO dto(FotoArvore foto) {
        return new FotoArvoreResponseDTO(foto.getId(), foto.getArvore().getId(), foto.getContentType(),
                foto.getSizeBytes(), foto.getCriadoEm());
    }

    private void exigirTransacao() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !java.util.Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),
                        org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED)) {
            throw new IllegalStateException("A operação de fotografias exige uma transação de escrita READ_COMMITTED ativa.");
        }
    }

    private void compensarRollback(Runnable compensacao, String objeto) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        compensacao.run();
                    } catch (RuntimeException ex) {
                        // Chave gerada internamente permite reconciliação operacional; nunca retorna ao cliente.
                        LOG.error("Compensação de fotografia pendente para objeto {} ({})", objeto, ex.getClass().getSimpleName());
                    }
                } else if (status == STATUS_UNKNOWN) {
                    // Não excluir cegamente: o commit do banco pode ter ocorrido.
                    LOG.error("Resultado transacional desconhecido; reconciliar fotografia {}", objeto);
                }
            }
        });
    }
}
