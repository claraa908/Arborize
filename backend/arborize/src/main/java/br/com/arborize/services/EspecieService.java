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

    @Autowired
    private io.minio.MinioClient minioClient;

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

    @Transactional(readOnly = true)
    public EspecieResponseDTO buscarPorId(Long id) {
        Especie especie = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espécie não encontrada com o ID: " + id));
        return mapper.toResponseDTO(especie);
    }

    @Transactional
    public EspecieResponseDTO atualizarEspecie(Long id, EspecieRequestDTO dto) {
        Especie especieExistente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espécie não encontrada com o ID: " + id));

        // Atualiza os atributos (requer que você crie o método updateEntity no EspecieMapper)
        mapper.updateEntity(dto, especieExistente);

        // Se o seu DTO tiver um campo para a imagem da espécie (ex: dto.imagem()), atualizamos o documento:
        if (dto.foto() != null && !dto.foto().isEmpty()) {

            // Opcional: Aqui você pode inserir a lógica para apagar a imagem antiga do MinIO antes de pôr a nova

            especieExistente.getDocuments().clear();
            DocumentEntities novaImagemDoc = documentService.uploadAndCreateDocument(dto.foto());
            novaImagemDoc.setEspecie(especieExistente); // Vincula o documento à Espécie
            especieExistente.getDocuments().add(novaImagemDoc);
        }

        Especie especieAtualizada = repository.save(especieExistente);
        return mapper.toResponseDTO(especieAtualizada);
    }

    @Transactional
    public void deletarEspecie(Long id) {
        Especie especieExistente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espécie não encontrada com o ID: " + id));

        // Apaga o arquivo físico no MinIO antes de deletar a espécie
        if (especieExistente.getDocuments() != null && !especieExistente.getDocuments().isEmpty()) {
            for (DocumentEntities doc : especieExistente.getDocuments()) {
                try {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket("arborize-materials") // Confirme se a espécie salva no mesmo bucket
                                    .object(doc.getFilePath())
                                    .build()
                    );
                } catch (Exception e) {
                    System.err.println("Aviso: Falha ao deletar imagem da Espécie no MinIO: " + e.getMessage());
                }
            }
        }

        repository.delete(especieExistente);
    }

    @Transactional(readOnly = true)
    public byte[] obterBytesDaImagem(Long id) {
        Especie especie = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espécie não encontrada com o ID: " + id));

        if (especie.getDocuments() == null || especie.getDocuments().isEmpty()) {
            throw new RuntimeException("Imagem não encontrada nesta espécie");
        }

        DocumentEntities imagemDoc = especie.getDocuments().get(0);

        String chaveNoMinio = imagemDoc.getFilePath();

        // Retorna os bytes chamando o seu DocumentService
        return documentService.getFileBytes(chaveNoMinio);
    }
}