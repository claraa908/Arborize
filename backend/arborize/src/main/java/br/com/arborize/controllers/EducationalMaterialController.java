package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.services.EducationalMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class EducationalMaterialController {

    @Autowired
    private EducationalMaterialService service;

    // 1. Endpoint POST para cadastrar o material e enviar os arquivos (Capa e PDF) para o MinIO
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EducationalMaterialResponseDTO> createMaterial(
            @ModelAttribute EducationalMaterialRequestDTO requestDTO) {
        EducationalMaterialResponseDTO response = service.createMaterial(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Endpoint GET para listar os materiais PUBLICADOS na tela pública (com suporte a busca e categoria)
    @GetMapping
    public ResponseEntity<List<EducationalMaterialResponseDTO>> getPublicMaterials(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaterialCategory category) {
        List<EducationalMaterialResponseDTO> materials = service.getPublicMaterials(search, category);
        return ResponseEntity.ok(materials);
    }

    // 3. Endpoint GET para o Administrador listar TODOS os materiais (Incluindo Rascunhos e Publicados)
    @GetMapping("/admin")
    public ResponseEntity<List<EducationalMaterialResponseDTO>> getAllMaterialsForAdmin() {
        List<EducationalMaterialResponseDTO> materials = service.getAllMaterialsForAdmin();
        return ResponseEntity.ok(materials);
    }

    // 4. Endpoint GET para buscar um material específico por ID (Para o Admin ver detalhes/editar)
    @GetMapping("/{id}")
    public ResponseEntity<EducationalMaterialResponseDTO> getMaterialById(@PathVariable Long id) {
        EducationalMaterialResponseDTO material = service.getMaterialById(id);
        return ResponseEntity.ok(material);
    }

    // 5. Endpoint GET para o download do PDF armazenado no MinIO
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] pdfBytes = service.getFileStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "cartilha_" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getCoverPhoto(@PathVariable Long id) {
        byte[] coverBytes = service.getCoverStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // ou detecte o tipo original

        return new ResponseEntity<>(coverBytes, headers, HttpStatus.OK);
    }
}