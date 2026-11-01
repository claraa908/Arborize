package br.com.arborize.mappers;

import br.com.arborize.dtos.request.UsuarioCadastroRequestDTO;
import br.com.arborize.dtos.request.UsuarioPerfilRequestDTO;
import br.com.arborize.dtos.response.UsuarioResponseDTO;
import br.com.arborize.entities.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioCadastroRequestDTO request, String cpf, String email, String encodedPassword) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setWhatsapp(request.whatsapp().trim());
        usuario.setEndereco(request.endereco().trim());
        usuario.setSenha(encodedPassword);
        return usuario;
    }

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getWhatsapp(),
                usuario.getEndereco()
        );
    }

    public void updateProfile(UsuarioPerfilRequestDTO request, Usuario usuario) {
        usuario.setNome(request.nome().trim());
        usuario.setWhatsapp(request.whatsapp().trim());
        usuario.setEndereco(request.endereco().trim());
    }
}
