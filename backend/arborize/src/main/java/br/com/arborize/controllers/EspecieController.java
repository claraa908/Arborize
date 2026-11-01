package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.services.EspecieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especies")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, allowCredentials = "true")
public class EspecieController {

    @Autowired
    private EspecieService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarEspecie(@ModelAttribute EspecieRequestDTO dto) {
        try {
            EspecieResponseDTO novaEspecie = service.criarEspecie(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaEspecie);
        } catch (Exception e) {
            e.printStackTrace(); // Mantemos isto para debug fácil no terminal!
            return ResponseEntity.badRequest().body("Falha ao salvar espécie: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<EspecieResponseDTO>> listarEspecies() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Rota para BUSCAR POR ID (Para carregar os dados no formulário de edição)
    @GetMapping("/{id}")
    public ResponseEntity<EspecieResponseDTO> buscarEspeciePorId(@PathVariable Long id) {
        try {
            EspecieResponseDTO especie = service.buscarPorId(id);
            return ResponseEntity.ok(especie);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Rota para EDITAR
    // Usamos @PostMapping com "/editar" para evitar o erro 405 do PUT com arquivos
    @PutMapping(value = "/editar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizarEspecie(@PathVariable Long id, @ModelAttribute EspecieRequestDTO dto) {
        try {
            EspecieResponseDTO especieAtualizada = service.atualizarEspecie(id, dto);
            return ResponseEntity.ok(especieAtualizada);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Falha ao atualizar a espécie: " + e.getMessage());
        }
    }

    // Rota para DELETAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarEspecie(@PathVariable Long id) {
        try {
            service.deletarEspecie(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Falha ao deletar a espécie: " + e.getMessage());
        }
    }

    // Rota para carregar a imagem da espécie no frontend
    @GetMapping("/{id}/imagem")
    public ResponseEntity<byte[]> buscarImagemDaEspecie(@PathVariable Long id) {
        try {
            // Busca a espécie para tentar pegar o Content-Type original
            EspecieResponseDTO especie = service.buscarPorId(id);
            String contentType = (especie.documentos() != null && !especie.documentos().isEmpty())
                    ? especie.documentos().get(0).contentType()
                    : "image/jpeg";

            // Busca os bytes no MinIO
            byte[] imagemBytes = service.obterBytesDaImagem(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imagemBytes);
        } catch (Exception e) {
            System.err.println("ERRO AO CARREGAR IMAGEM DA ESPÉCIE " + id + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}