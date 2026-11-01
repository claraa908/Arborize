package br.com.arborize.services;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.entities.DocumentEntities; // Verifique se a sua classe filha se chama Documento ou DocumentEntities
import br.com.arborize.entities.Eventos;
import br.com.arborize.mappers.EventosMapper;
import br.com.arborize.repositories.EventosRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventosService {

    @Autowired
    private EventosRepository repository;

    @Autowired
    private EventosMapper mapper;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private io.minio.MinioClient minioClient;

    @Transactional
    public EventosResponseDTO criarEvento(EventosRequestDTO dto) {
        // 1. Converte o DTO para Entidade
        Eventos evento = mapper.toEntity(dto);

        // 2. Lida com o upload do banner (Imagem)
        if (dto.banner() != null && !dto.banner().isEmpty()) {
            // Usa o mesmo serviço de documentos que usou nos Materiais Educativos
            DocumentEntities bannerDoc = documentService.uploadAndCreateDocument(dto.banner());
            bannerDoc.setEventos(evento);
            evento.getDocuments().add(bannerDoc);
        }

        // 3. Salva no banco (O CascadeType.ALL vai salvar o Documento junto automaticamente)
        Eventos eventoSalvo = repository.save(evento);

        // 4. Converte o resultado de volta para DTO e retorna
        return mapper.toResponseDTO(eventoSalvo);
    }

    @Transactional(readOnly = true)
    public List<EventosResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventosResponseDTO buscarPorId(Long id) {
        Eventos evento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com o ID: " + id));

        return mapper.toResponseDTO(evento);
    }

    @Transactional(readOnly = true)
    public byte[] obterBytesDoBanner(Long id) {
        Eventos evento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com o ID: " + id));

        if (evento.getDocuments() == null || evento.getDocuments().isEmpty()) {
            throw new RuntimeException("Banner não encontrado neste evento");
        }

        DocumentEntities bannerDoc = evento.getDocuments().get(0);

        // Em vez de usar getOriginalFilename(), usamos o getFilePath() que já existe graças ao Lombok!
        String chaveNoMinio = bannerDoc.getFilePath();

        return documentService.getFileBytes(chaveNoMinio);
    }

    @Transactional
    public EventosResponseDTO atualizarEvento(Long id, EventosRequestDTO dto) {
        Eventos eventoExistente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com o ID: " + id));

        mapper.updateEntity(dto, eventoExistente);

        if (dto.banner() != null && !dto.banner().isEmpty()) {

            // Opcional: Se o evento só pode ter 1 banner, limpe a lista atual antes de adicionar o novo.
            // Para isso funcionar e apagar do banco, a relação @OneToMany em 'Eventos' precisa ter orphanRemoval = true
            eventoExistente.getDocuments().clear();

            DocumentEntities novoBannerDoc = documentService.uploadAndCreateDocument(dto.banner());
            novoBannerDoc.setEventos(eventoExistente);
            eventoExistente.getDocuments().add(novoBannerDoc);
        }

        Eventos eventoAtualizado = repository.save(eventoExistente);

        return mapper.toResponseDTO(eventoAtualizado);
    }

    @Transactional
    public void deletarEvento(Long id) {
        Eventos eventoExistente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com o ID: " + id));

        if (eventoExistente.getDocuments() != null && !eventoExistente.getDocuments().isEmpty()) {
            for (DocumentEntities doc : eventoExistente.getDocuments()) {
                try {
                    // Usa o MinioClient para apagar o arquivo
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket("arborize-materials")
                                    .object(doc.getFilePath())
                                    .build()
                    );

                } catch (Exception e) {
                    // Se o arquivo não existir no MinIO, não trava a exclusão do banco
                    System.err.println("Aviso: Falha ao deletar banner do MinIO (Evento " + id + "): " + e.getMessage());
                }
            }
        }

        repository.delete(eventoExistente);
    }
}