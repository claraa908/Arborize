package br.com.arborize.services;

import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO;
import br.com.arborize.dtos.request.DadosArvoreRequest;
import br.com.arborize.dtos.response.ArvoreResponseDTO;
import br.com.arborize.dtos.response.FotoCapaResponseDTO;
import br.com.arborize.entities.Arvore;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.entities.Bairro;
import br.com.arborize.entities.Especie;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.mappers.ArvoreMapper;
import br.com.arborize.repositories.ArvoreRepository;
import br.com.arborize.repositories.BairroRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.FotoArvoreRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import br.com.arborize.dtos.response.ArvoreResumoResponseDTO;
import br.com.arborize.enums.CondicaoFitossanitaria;
import br.com.arborize.repositories.ArvoreSpecifications;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArvoreService {
    private final ArvoreRepository repository;
    private final EspecieRepository especieRepository;
    private final BairroRepository bairroRepository;
    private final FotoArvoreRepository fotoRepository;
    private final ArvoreMapper mapper;
    private final Validator validator;

    public ArvoreService(ArvoreRepository repository, EspecieRepository especieRepository,
                        BairroRepository bairroRepository, FotoArvoreRepository fotoRepository,
                        ArvoreMapper mapper, Validator validator) {
        this.repository = repository;
        this.especieRepository = especieRepository;
        this.bairroRepository = bairroRepository;
        this.fotoRepository = fotoRepository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    public ArvoreResponseDTO criarArvore(ArvoreCadastroRequestDTO dto) {
        if (dto == null) {
            throw new EntradaInventarioInvalidaException("corpo", "Os dados de cadastro da árvore devem ser informados.");
        }
        var violacoes = validator.validate(dto);
        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException("Dados de cadastro da árvore inválidos.", violacoes);
        }

        Especie especie = especieRepository.findById(dto.especieId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Espécie não encontrada com o identificador: " + dto.especieId()));
        Bairro bairro = resolverBairro(dto, null);
        Arvore arvore = mapper.toEntity(dto, especie, bairro);
        Arvore salva = repository.save(arvore);
        return mapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public ArvoreResponseDTO buscarPorId(Long id) {
        if (id == null || id <= 0) {
            throw new EntradaInventarioInvalidaException("id", "O identificador da árvore deve ser positivo.");
        }
        Arvore arvore = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Árvore não encontrada com o identificador: " + id));
        // Converte as relações LAZY enquanto a transação de leitura está ativa.
        return mapper.toResponseDTO(arvore);
    }

    @Transactional(readOnly = true)
    public Page<ArvoreResumoResponseDTO> listar(String pesquisa, Long bairroId, Long especieId,
                                               CondicaoFitossanitaria condicao, Pageable pageable) {
        return listar(pesquisa, null, bairroId, especieId, condicao, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ArvoreResumoResponseDTO> listar(String pesquisa, String bairro, Long bairroId, Long especieId,
                                               CondicaoFitossanitaria condicao, Pageable pageable) {
        validarFiltros(bairroId, especieId);
        if (pageable == null || pageable.isUnpaged()) {
            throw new EntradaInventarioInvalidaException("paginacao", "Informe página e tamanho para a listagem.");
        }
        // Ordem total por chave única; não aceita ordenação arbitrária de atributos JPA.
        var pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        var resultado = repository.findAll(ArvoreSpecifications.filtrar(pesquisa, bairro, bairroId, especieId, condicao), pagina);
        var capas = carregarCapas(resultado.getContent());
        return resultado.map(arvore -> mapper.toResumoDTO(arvore,
                capas.getOrDefault(arvore.getId(), null)));
    }

    @Transactional(readOnly = true)
    public List<ArvoreResumoResponseDTO> consultarMapa(String pesquisa, Long bairroId, Long especieId,
                                                      CondicaoFitossanitaria condicao) {
        return consultarMapa(pesquisa, null, bairroId, especieId, condicao);
    }

    @Transactional(readOnly = true)
    public List<ArvoreResumoResponseDTO> consultarMapa(String pesquisa, String bairro, Long bairroId,
                                                      Long especieId, CondicaoFitossanitaria condicao) {
        validarFiltros(bairroId, especieId);
        // Conjunto completo filtrado, independente da página da tabela. Sem truncamento silencioso.
        // Antes da exposição HTTP, validar volume: esta operação materializa o resultado em memória.
        var resultado = repository.findAll(ArvoreSpecifications.filtrar(pesquisa, bairro, bairroId, especieId, condicao), Sort.by("id"));
        var capas = carregarCapas(resultado);
        return resultado.stream().map(arvore -> mapper.toResumoDTO(arvore,
                capas.getOrDefault(arvore.getId(), null))).toList();
    }

    private Map<Long, FotoCapaResponseDTO> carregarCapas(List<Arvore> arvores) {
        if (arvores.isEmpty()) return Map.of();
        var ids = arvores.stream().map(Arvore::getId).toList();
        return fotoRepository.findCapasByArvoreIds(ids).stream().collect(Collectors.toMap(
                FotoArvoreRepository.FotoCapaProjecao::getArvoreId,
                foto -> new FotoCapaResponseDTO(foto.getId(), foto.getContentType())
        ));
    }

    private void validarFiltros(Long bairroId, Long especieId) {
        if (bairroId != null && bairroId <= 0) {
            throw new EntradaInventarioInvalidaException("bairroId", "O identificador do bairro deve ser positivo.");
        }
        if (especieId != null && especieId <= 0) {
            throw new EntradaInventarioInvalidaException("especieId", "O identificador da espécie deve ser positivo.");
        }
    }

    @Transactional
    public ArvoreResponseDTO atualizarArvore(Long id, ArvoreAtualizacaoRequestDTO dto) {
        if (id == null || id <= 0) {
            throw new EntradaInventarioInvalidaException("id", "O identificador da árvore deve ser positivo.");
        }
        if (dto == null) {
            throw new EntradaInventarioInvalidaException("corpo", "Os dados de atualização da árvore devem ser informados.");
        }
        var violacoes = validator.validate(dto);
        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException("Dados de atualização da árvore inválidos.", violacoes);
        }
        Arvore arvore = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Árvore não encontrada com o identificador: " + id));
        Especie especie = especieRepository.findById(dto.especieId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Espécie não encontrada com o identificador: " + dto.especieId()));
        Bairro bairro = resolverBairro(dto, arvore.getBairro());
        // Todas as rejeições esperadas são verificadas antes de modificar a entidade existente.
        mapper.updateEntity(arvore, dto, especie, bairro);
        Arvore salva = repository.save(arvore);
        // Executa @PreUpdate antes do mapeamento, preservando os metadados retornados ao cliente.
        repository.flush();
        return mapper.toResponseDTO(salva);
    }

    private Bairro resolverBairro(DadosArvoreRequest dto, Bairro bairroAtual) {
        if (dto.zonaRural() != null) {
            if (dto.zonaRural()) return null;
            if (dto.bairroId() == null) {
                throw new EntradaInventarioInvalidaException("bairroId", "Selecione um bairro ativo para a árvore urbana.");
            }
        } else if (dto.classificacaoTerritorial() != ClassificacaoTerritorial.BAIRRO_IDENTIFICADO) {
            return null;
        }
        if (dto.bairroId() == null) return bairroAtual;
        Bairro bairro = bairroRepository.findById(dto.bairroId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Bairro não encontrado com o identificador: " + dto.bairroId()));
        if (!Boolean.TRUE.equals(bairro.getAtivo())) {
            throw new EntradaInventarioInvalidaException("bairroId", "O bairro informado não está disponível para associação.");
        }
        // A aprovação da fonte territorial continua pendente da AMMA; não se cria catálogo aqui.
        return bairro;
    }
}
