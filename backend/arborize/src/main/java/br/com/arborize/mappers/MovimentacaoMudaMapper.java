package br.com.arborize.mappers;

import br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO;
import br.com.arborize.entities.MovimentacaoMuda;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoMudaMapper {
    public MovimentacaoMudaResponseDTO toResponse(MovimentacaoMuda movimento) {
        return new MovimentacaoMudaResponseDTO(
                movimento.getId(),
                movimento.getTipo(),
                movimento.getEspecie().getId(),
                movimento.getEspecie().getNomePopular(),
                movimento.getEspecie().getNomeCientifico(),
                movimento.getQuantidade(),
                movimento.getData(),
                movimento.getDestino(),
                movimento.getResponsavel(),
                movimento.getObservacoes(),
                movimento.getComprovante() == null ? null : movimento.getComprovante().getOriginalFilename()
        );
    }
}
