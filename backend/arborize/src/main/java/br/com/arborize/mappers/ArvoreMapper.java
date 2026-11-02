package br.com.arborize.mappers;

import br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO;
import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.dtos.request.DadosArvoreRequest;
import br.com.arborize.dtos.response.*;
import br.com.arborize.entities.Arvore;
import br.com.arborize.entities.Bairro;
import br.com.arborize.entities.Especie;
import br.com.arborize.utils.CodigoArvore;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Mapeamento sem persistência. A futura camada de serviço deverá validar os DTOs,
 * carregar as relações e verificar sua existência e disponibilidade institucional.
 * Os mapeamentos de resposta devem ocorrer enquanto as relações estiverem acessíveis.
 */
@Component
public class ArvoreMapper {

    public Arvore toEntity(ArvoreCadastroRequestDTO dto, Especie especie, Bairro bairro) {
        Arvore arvore = new Arvore();
        preencherCamposEditaveis(arvore, dto, especie, bairro);
        return arvore;
    }

    /** PUT completo: campos opcionais nulos limpam os valores existentes. */
    public void updateEntity(Arvore arvore, ArvoreAtualizacaoRequestDTO dto, Especie especie, Bairro bairro) {
        Objects.requireNonNull(arvore, "A árvore deve ser informada.");
        preencherCamposEditaveis(arvore, dto, especie, bairro);
    }

    private void preencherCamposEditaveis(Arvore arvore, DadosArvoreRequest dto, Especie especie, Bairro bairro) {
        Objects.requireNonNull(dto, "Os dados da árvore devem ser informados.");
        Objects.requireNonNull(especie, "A espécie deve ser carregada pelo serviço.");
        if (dto.especieId() == null || !dto.especieId().equals(especie.getId())) {
            throw new IllegalArgumentException("A espécie carregada não corresponde ao identificador informado.");
        }
        if ((dto.bairroId() != null && (bairro == null || !dto.bairroId().equals(bairro.getId())))
                || (classificacaoEfetiva(dto) != br.com.arborize.enums.ClassificacaoTerritorial.BAIRRO_IDENTIFICADO
                && bairro != null)) {
            throw new IllegalArgumentException("O bairro carregado não corresponde ao identificador informado.");
        }

        arvore.setEspecie(especie);
        arvore.setLatitude(dto.latitude());
        arvore.setLongitude(dto.longitude());
        arvore.setClassificacaoTerritorial(classificacaoEfetiva(dto));
        arvore.setBairro(bairro);
        arvore.setBairroNome(dto.zonaRural() == null
                ? dto.bairroNome() != null ? dto.bairroNome() : bairro == null ? null : bairro.getNome()
                : dto.zonaRural() ? null : bairro == null ? null : bairro.getNome());
        arvore.setCep(dto.cep());
        arvore.setLocalidadeRural(dto.zonaRural() == null ? dto.localidadeRural()
                : dto.zonaRural() ? dto.localidadeRural() : null);
        arvore.setReferenciaLocalizacao(dto.referenciaLocalizacao());
        arvore.setDataPlantio(dto.dataPlantio());
        arvore.setDapCm(dto.dapCm());
        arvore.setAlturaEstimadaM(dto.alturaEstimadaM());
        arvore.setCondicaoFitossanitaria(dto.condicaoFitossanitaria());
        arvore.setObservacoes(dto.observacoes());
        // ID, metadados e associações de arquivos não fazem parte deste mapeamento.
    }

    public ArvoreResponseDTO toResponseDTO(Arvore arvore) {
        Objects.requireNonNull(arvore, "A árvore deve ser informada.");
        return new ArvoreResponseDTO(
                arvore.getId(), CodigoArvore.formatar(arvore.getId()), especieDTO(arvore.getEspecie()),
                arvore.getLatitude(), arvore.getLongitude(), arvore.getClassificacaoTerritorial(),
                bairroDTO(arvore.getBairro()), nomeTerritorial(arvore), arvore.getCep(),
                arvore.getReferenciaLocalizacao(), arvore.getDataPlantio(),
                arvore.getDapCm(), arvore.getAlturaEstimadaM(), arvore.getCondicaoFitossanitaria(),
                arvore.getObservacoes(), arvore.getCriadoEm(), arvore.getAtualizadoEm(),
                zonaRural(arvore), arvore.getLocalidadeRural()
        );
    }

    public ArvoreResumoResponseDTO toResumoDTO(Arvore arvore) {
        return toResumoDTO(arvore, null);
    }

    public ArvoreResumoResponseDTO toResumoDTO(Arvore arvore, FotoCapaResponseDTO fotoCapa) {
        Objects.requireNonNull(arvore, "A árvore deve ser informada.");
        return new ArvoreResumoResponseDTO(
                arvore.getId(), CodigoArvore.formatar(arvore.getId()), especieDTO(arvore.getEspecie()),
                arvore.getLatitude(), arvore.getLongitude(), arvore.getClassificacaoTerritorial(),
                bairroDTO(arvore.getBairro()), nomeTerritorial(arvore), arvore.getCep(),
                arvore.getReferenciaLocalizacao(), arvore.getCondicaoFitossanitaria(),
                fotoCapa, zonaRural(arvore), arvore.getLocalidadeRural()
        );
    }

    private br.com.arborize.enums.ClassificacaoTerritorial classificacaoEfetiva(DadosArvoreRequest dto) {
        if (dto.zonaRural() == null) return dto.classificacaoTerritorial();
        return dto.zonaRural()
                ? br.com.arborize.enums.ClassificacaoTerritorial.FORA_DA_AREA_URBANA
                : br.com.arborize.enums.ClassificacaoTerritorial.BAIRRO_IDENTIFICADO;
    }

    private Boolean zonaRural(Arvore arvore) {
        return switch (arvore.getClassificacaoTerritorial()) {
            case BAIRRO_IDENTIFICADO -> false;
            case FORA_DA_AREA_URBANA -> true;
            case BAIRRO_NAO_IDENTIFICADO -> null;
        };
    }

    private String nomeTerritorial(Arvore arvore) {
        return arvore.getBairroNome() != null ? arvore.getBairroNome()
                : arvore.getBairro() == null ? null : arvore.getBairro().getNome();
    }

    private EspecieIdentificacaoResponseDTO especieDTO(Especie especie) {
        Objects.requireNonNull(especie, "A árvore deve possuir espécie.");
        return new EspecieIdentificacaoResponseDTO(
                especie.getId(), especie.getNomePopular(), especie.getNomeCientifico(), especie.getFamiliaBotanica()
        );
    }

    private BairroResumoResponseDTO bairroDTO(Bairro bairro) {
        return bairro == null ? null : new BairroResumoResponseDTO(bairro.getId(), bairro.getNome(), bairro.getAtivo());
    }
}
