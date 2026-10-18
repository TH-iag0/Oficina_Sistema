package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_admin")
public class UsuarioAdmin extends Entidade {
    @Column(nullable = false, unique = true, length = 120)
    private String login;
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;
    protected UsuarioAdmin() {}
    public UsuarioAdmin(String login, String senhaHash) {
        this.login = RegrasModel.texto(login, 120, "Login");
        this.senhaHash = RegrasModel.texto(senhaHash, 100, "Hash");
    }

    public String getLogin() {
        return login;
    }

    public String hashParaAutenticacao() {
        return senhaHash;
    }

    public void alterarHash(String hash) {
        senhaHash = RegrasModel.texto(hash, 100, "Hash");
    }
}
