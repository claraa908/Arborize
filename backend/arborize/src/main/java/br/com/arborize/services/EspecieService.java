package br.com.arborize.services;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Especie;
import br.com.arborize.mappers.EspecieMapper;
import br.com.arborize.repositories.EspecieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspecieService {

    @Autowired
    private EspecieRepository repository;

    @Autowired
    private EspecieMapper mapper;

    @Autowired
    private DocumentService documentService;

    @Transactional
    public EspecieResponseDTO criarEspecie(EspecieRequestDTO dto) {
        Especie especie = mapper.toEntity(dto);

        // Se o front-end enviou uma foto
        if (dto.foto() != null && !dto.foto().isEmpty()) {
            DocumentEntities fotoDoc = documentService.uploadAndCreateDocument(dto.foto());
            fotoDoc.setEspecie(especie); // Diz ao documento a qual espécie ele pertence
            especie.getDocuments().add(fotoDoc); // Adiciona o documento à espécie
        }

        Especie especieSalva = repository.save(especie);
        return mapper.toResponseDTO(especieSalva);
    }

    @Transactional(readOnly = true) // Importante para evitar o "no session" ao listar os documentos
    public List<EspecieResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}