package br.com.arborize.services;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.EducationalMaterialEntities;
import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.mappers.EducationalMaterialMapper;
import br.com.arborize.repositories.EducationalMaterialRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EducationalMaterialService {

    @Autowired
    private EducationalMaterialRepository repository;

    @Autowired
    private EducationalMaterialMapper mapper;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    // 1. Cadastra o material e os arquivos (Capa e PDF)
    public EducationalMaterialResponseDTO createMaterial(EducationalMaterialRequestDTO dto) {
        try {
            EducationalMaterialEntities material = mapper.toEntity(dto);
            List<DocumentEntities> documents = new ArrayList<>();

            if (dto.file() != null && !dto.file().isEmpty()) {
                DocumentEntities pdfDoc = documentService.uploadAndCreateDocument(dto.file());
                pdfDoc.setEducationalMaterial(material);
                documents.add(pdfDoc);
            }

            if (dto.coverPhoto() != null && !dto.coverPhoto().isEmpty()) {
                DocumentEntities coverDoc = documentService.uploadAndCreateDocument(dto.coverPhoto());
                coverDoc.setEducationalMaterial(material);
                documents.add(coverDoc);
            }

            material.setDocuments(documents);
            EducationalMaterialEntities saved = repository.save(material);

            return mapper.toResponseDTO(saved);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cadastrar material educativo: " + e.getMessage(), e);
        }
    }

    // 2. Listagem Pública com suporte a busca por título E filtro por categoria
    @Transactional(readOnly = true)
    public List<EducationalMaterialResponseDTO> getPublicMaterials(String search, MaterialCategory category) {
        List<EducationalMaterialEntities> materials;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasCategory = category != null;

        if (hasSearch && hasCategory) {
            materials = repository.findByStatusAndCategoryAndTitleContainingIgnoreCase("PUBLICADO", category, search);
        } else if (hasSearch) {
            materials = repository.findByStatusAndTitleContainingIgnoreCase("PUBLICADO", search);
        } else if (hasCategory) {
            materials = repository.findByStatusAndCategory("PUBLICADO", category);
        } else {
            materials = repository.findByStatus("PUBLICADO");
        }

        return materials.stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 3. Lista TODOS os materiais para o Administrador (Incluindo RASCUNHO e PUBLICADO)
    @Transactional(readOnly = true)
    public List<EducationalMaterialResponseDTO> getAllMaterialsForAdmin() {
        List<EducationalMaterialEntities> materials = repository.findAllByOrderByIdDesc();
        return materials.stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 4. Busca um material específico por ID (Para o Admin ver os detalhes e editar)
    @Transactional(readOnly = true)
    public EducationalMaterialResponseDTO getMaterialById(Long id) {
        EducationalMaterialEntities material = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material não encontrado com o ID: " + id));
        return mapper.toResponseDTO(material);
    }

    // 5. UPDATE (Modificar dados do material)
    @Transactional
    public EducationalMaterialResponseDTO updateMaterial(Long id, EducationalMaterialRequestDTO dto) {
        EducationalMaterialEntities material = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material não encontrado para atualização com ID: " + id));

        material.setTitle(dto.title());
        material.setDescription(dto.description());
        material.setType(dto.type());
        material.setCategory(dto.category());
        material.setStatus(dto.status());

        EducationalMaterialEntities updated = repository.save(material);
        return mapper.toResponseDTO(updated);
    }

    // 6. UPDATE exclusivo de status
    @Transactional
    public EducationalMaterialResponseDTO updateStatus(Long id, String newStatus) {
        EducationalMaterialEntities material = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material não encontrado com ID: " + id));

        if (!newStatus.equals("PUBLICADO") && !newStatus.equals("RASCUNHO")) {
            throw new IllegalArgumentException("Status inválido. Use 'PUBLICADO' ou 'RASCUNHO'.");
        }

        material.setStatus(newStatus);
        EducationalMaterialEntities updated = repository.save(material);
        return mapper.toResponseDTO(updated);
    }

    // 7. DELETE (Excluir material e remover arquivos do MinIO)
    @Transactional
    public void deleteMaterial(Long id) {
        EducationalMaterialEntities material = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material não encontrado para exclusão com ID: " + id));

        // Remove os arquivos associados do MinIO
        if (material.getDocuments() != null) {
            for (DocumentEntities doc : material.getDocuments()) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(doc.getFilePath())
                                    .build()
                    );
                } catch (Exception e) {
                    System.err.println("Aviso: Não foi possível remover o arquivo do MinIO: " + doc.getFilePath() + " - " + e.getMessage());
                }
            }
        }

        repository.delete(material);
    }

    // 8. Busca os bytes do PDF no MinIO para download
    public byte[] getFileStream(Long materialId) {
        EducationalMaterialEntities material = repository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));

        if (material.getDocuments() == null || material.getDocuments().isEmpty()) {
            throw new RuntimeException("Nenhum documento associado a este material");
        }

        DocumentEntities doc = material.getDocuments().get(0);

        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(doc.getFilePath())
                            .build()
            );
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao recuperar arquivo do MinIO: " + e.getMessage(), e);
        }
    }

    public byte[] getCoverStream(Long materialId) {
        EducationalMaterialEntities material = repository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));

        if (material.getDocuments() == null || material.getDocuments().isEmpty()) {
            throw new RuntimeException("Nenhum documento associado");
        }

        // Procura o documento que seja imagem (por exemplo, contendo image/ no content type ou extensão)
        // Ou se preferir, o segundo documento enviado. Vamos pegar o que tiver extensão de imagem ou o segundo elemento:
        DocumentEntities coverDoc = material.getDocuments().stream()
                .filter(d -> d.getContentType() != null && d.getContentType().startsWith("image"))
                .findFirst()
                .orElse(material.getDocuments().size() > 1 ? material.getDocuments().get(1) : null);

        if (coverDoc == null) {
            throw new RuntimeException("Nenhuma foto de capa associada a este material");
        }

        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(coverDoc.getFilePath())
                            .build()
            );
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao recuperar capa do MinIO: " + e.getMessage(), e);
        }
    }
}