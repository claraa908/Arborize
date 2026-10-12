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
}