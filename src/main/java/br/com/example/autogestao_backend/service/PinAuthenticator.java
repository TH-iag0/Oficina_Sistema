package br.com.example.autogestao_backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PinAuthenticator {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public boolean matches(String pin, String hash) {
        return encoder.matches(pin, hash);
    }

    public String codificar(String pin) {
        return encoder.encode(pin);
    }
}
