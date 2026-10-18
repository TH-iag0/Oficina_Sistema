package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.UsuarioAdmin;
import br.com.example.autogestao_backend.repository.UsuarioAdminRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService implements UserDetailsService {
    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);
    private final UsuarioAdminRepository usuarios;
    private final PasswordEncoder encoder;
    public AdminService(UsuarioAdminRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional
    public void inicializar(String login, String senhaInicial) {
        if (usuarios.count() > 0) {
            return;
        }
        if (senhaInicial == null || senhaInicial.isBlank()) {
            LOG.warn("Defina AUTOGESTAO_ADMIN_PASSWORD para configurar o acesso do proprietário.");
            return;
        }
        validarSenha(senhaInicial);
        usuarios.saveAndFlush(new UsuarioAdmin(login, encoder.encode(senhaInicial)));
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        var usuario = usuarios.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return User.withUsername(usuario.getLogin()).password(usuario.hashParaAutenticacao()).roles("ADMIN").build();
    }

    @Transactional
    public void alterarSenha(String login, OficinaDtos.SenhaEntrada dados) {
        var usuario = usuarios.bloquear(login).orElseThrow(() -> new ErroApi(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        if (!encoder.matches(dados.senhaAtual(), usuario.hashParaAutenticacao())) {
            throw new ErroApi(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        validarSenha(dados.novaSenha());
        usuario.alterarHash(encoder.encode(dados.novaSenha()));
    }

    private void validarSenha(String senha) {
        int bytes = senha.getBytes(StandardCharsets.UTF_8).length;
        if (senha.length() < 12 || bytes > 72) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Senha deve ter pelo menos 12 caracteres e até 72 bytes.");
        }
    }
}
