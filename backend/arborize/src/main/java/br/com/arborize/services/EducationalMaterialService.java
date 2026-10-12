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

    // 2. Lista os materiais publicados (com suporte a busca ou categoria)
    @Transactional(readOnly = true)
    public List<EducationalMaterialResponseDTO> getPublicMaterials(String search, MaterialCategory category) {
        List<EducationalMaterialEntities> materials;

        if (search != null && !search.isEmpty()) {
            materials = repository.findByStatusAndTitleContainingIgnoreCase("PUBLICADO", search);
        } else if (category != null) {
            materials = repository.findByStatusAndCategory("PUBLICADO", category);
        } else {
            materials = repository.findByStatus("PUBLICADO");
        }

        return materials.stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 3. Busca os bytes do PDF no MinIO para download
    public byte[] getFileStream(Long materialId) {
        EducationalMaterialEntities material = repository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));

        if (material.getDocuments() == null || material.getDocuments().isEmpty()) {
            throw new RuntimeException("Nenhum documento associado a este material");
        }

        // Pega o primeiro documento (geralmente o PDF) para download
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
}