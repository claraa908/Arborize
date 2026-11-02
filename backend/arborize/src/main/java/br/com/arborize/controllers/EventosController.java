package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.services.EventosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos") // O seu Vue.js deve fazer fetch para http://localhost:8080/eventos
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, allowCredentials = "true")
@Tag(name = "Eventos", description = "Endpoints de CRUD para Eventos")
public class EventosController {

    @Autowired
    private EventosService service;

    // Rota para CRIAR o evento (Recebe o formulário e a imagem)
    // Repare que mudei para ResponseEntity<?> para podermos devolver texto em caso de erro
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarEvento(@ModelAttribute EventosRequestDTO dto) {
        try {
            EventosResponseDTO novoEvento = service.criarEvento(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoEvento);
        } catch (Exception e) {
            // ISTO É A PEÇA CHAVE! Vai imprimir o erro real no terminal do IntelliJ
            e.printStackTrace();
            // E vai enviar a mensagem de erro para o painel do navegador!
            return ResponseEntity.badRequest().body("Falha ao salvar: " + e.getMessage());
        }
    }

    // Rota para LISTAR os eventos (Devolve os DTOs em vez da Entidade pura)
    @GetMapping
    public ResponseEntity<List<EventosResponseDTO>> listarEventos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Rota para BUSCAR UM EVENTO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<EventosResponseDTO> buscarEventoPorId(@PathVariable Long id) {
        try {
            EventosResponseDTO evento = service.buscarPorId(id);
            return ResponseEntity.ok(evento);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build(); // Retorna erro 404 se não existir
        }
    }

    @GetMapping("/{id}/banner")
    public ResponseEntity<byte[]> buscarBannerDoEvento(@PathVariable Long id) {
        try {
            // Busca o evento para pegar o tipo de conteúdo (ex: image/png, image/jpeg)
            EventosResponseDTO evento = service.buscarPorId(id);
            String contentType = evento.documentos().isEmpty() ? "image/jpeg" : evento.documentos().get(0).contentType();

            // Busca os bytes da imagem no MinIO
            byte[] imagemBytes = service.obterBytesDoBanner(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imagemBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Rota para ATUALIZAR o evento
    @PutMapping(value = "/editar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizarEvento(@PathVariable Long id, @ModelAttribute EventosRequestDTO dto){
        try {
            EventosResponseDTO eventoAtualizado = service.atualizarEvento(id, dto);
            return ResponseEntity.ok(eventoAtualizado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Falha ao atualizar: " + e.getMessage());
        }
    }

    // Rota para DELETAR o evento
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarEvento(@PathVariable Long id) {
        try {
            service.deletarEvento(id);
            return ResponseEntity.noContent().build(); // Retorna 204 (No Content) em caso de sucesso
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Falha ao deletar: " + e.getMessage());
        }
    }
}
