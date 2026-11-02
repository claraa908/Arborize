package br.com.arborize.services;

import br.com.arborize.dtos.response.BairroResumoResponseDTO;
import br.com.arborize.repositories.BairroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BairroService {
    private final BairroRepository repository;

    public BairroService(BairroRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BairroResumoResponseDTO> listarDisponiveis() {
        return repository.findAllByAtivoTrueOrderByNomeAsc().stream()
                .map(bairro -> new BairroResumoResponseDTO(bairro.getId(), bairro.getNome(), bairro.getAtivo()))
                .toList();
    }
}
