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
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
public class EducationalMaterialController {

    @Autowired
    private EducationalMaterialService service;

    // 1. Cadastrar Material (POST)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EducationalMaterialResponseDTO> createMaterial(
            @ModelAttribute EducationalMaterialRequestDTO requestDTO) {
        EducationalMaterialResponseDTO response = service.createMaterial(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Listagem Pública (Cidadão) com suporte a busca por texto E filtro por categoria
    @GetMapping
    public ResponseEntity<List<EducationalMaterialResponseDTO>> getPublicMaterials(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaterialCategory category) {
        List<EducationalMaterialResponseDTO> materials = service.getPublicMaterials(search, category);
        return ResponseEntity.ok(materials);
    }

    // 3. Listagem Admin (Todos os materiais)
    @GetMapping("/admin")
    public ResponseEntity<List<EducationalMaterialResponseDTO>> getAllMaterialsForAdmin() {
        List<EducationalMaterialResponseDTO> materials = service.getAllMaterialsForAdmin();
        return ResponseEntity.ok(materials);
    }

    // 4. Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<EducationalMaterialResponseDTO> getMaterialById(@PathVariable Long id) {
        EducationalMaterialResponseDTO material = service.getMaterialById(id);
        return ResponseEntity.ok(material);
    }

    // 5. Atualizar Material Completo (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<EducationalMaterialResponseDTO> updateMaterial(
            @PathVariable Long id,
            @ModelAttribute EducationalMaterialRequestDTO requestDTO) {
        EducationalMaterialResponseDTO updated = service.updateMaterial(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    // 6. Modificar Apenas o Status (PATCH) -> ex: {"status": "PUBLICADO"} ou {"status": "RASCUNHO"}
    @PatchMapping("/{id}/status")
    public ResponseEntity<EducationalMaterialResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        EducationalMaterialResponseDTO updated = service.updateStatus(id, newStatus);
        return ResponseEntity.ok(updated);
    }

    // 7. Excluir Documento/Material (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        service.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    // 8. Download do PDF (GET)
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