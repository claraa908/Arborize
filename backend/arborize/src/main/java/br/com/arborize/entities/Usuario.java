package br.com.arborize.entities;

import br.com.arborize.enums.UsuarioRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.Instant;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuarios_cpf", columnNames = "cpf"),
                @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email")
        }
)
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String whatsapp;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32) default 'CIDADAO'")
    private UsuarioRole role = UsuarioRole.CIDADAO;

    @Column(nullable = false, updatable = false, columnDefinition = "timestamp with time zone default CURRENT_TIMESTAMP")
    private Instant dataCadastro;

    @PrePersist
    private void definirRolePadrao() {
        if (role == null) role = UsuarioRole.CIDADAO;
        if (dataCadastro == null) dataCadastro = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    @Override
    public String getPassword() { return senha; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public UsuarioRole getRole() { return role; }
    public void setRole(UsuarioRole role) { this.role = role; }
    public Instant getDataCadastro() { return dataCadastro; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
