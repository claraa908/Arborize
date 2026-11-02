package br.com.arborize.mappers;

import br.com.arborize.dtos.request.UsuarioCadastroRequestDTO;
import br.com.arborize.dtos.request.AdminUsuarioRequestDTO;
import br.com.arborize.dtos.request.IfceBolsistaRequestDTO;
import br.com.arborize.dtos.request.UsuarioPerfilRequestDTO;
import br.com.arborize.dtos.response.AdminUsuarioResponseDTO;
import br.com.arborize.dtos.response.IfceBolsistaResponseDTO;
import br.com.arborize.dtos.response.UsuarioResponseDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toIfceBolsistaEntity(
            IfceBolsistaRequestDTO request,
            String cpf,
            String email,
            String encodedPassword
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setWhatsapp(request.whatsapp().trim());
        usuario.setEndereco("");
        usuario.setSenha(encodedPassword);
        usuario.setRole(UsuarioRole.BOLSISTA_IFCE);
        return usuario;
    }

    public IfceBolsistaResponseDTO toIfceBolsistaResponseDTO(Usuario usuario) {
        return new IfceBolsistaResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getWhatsapp()
        );
    }

    public Usuario toInstitutionalEntity(
            AdminUsuarioRequestDTO request,
            String cpf,
            String email,
            String encodedPassword,
            UsuarioRole role
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setWhatsapp(request.whatsapp().trim());
        usuario.setEndereco("");
        usuario.setSenha(encodedPassword);
        usuario.setRole(role);
        return usuario;
    }

    public AdminUsuarioResponseDTO toAdminResponseDTO(Usuario usuario) {
        return new AdminUsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getDataCadastro(),
                usuario.isEnabled() ? "ATIVO" : "INATIVO"
        );
    }

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
