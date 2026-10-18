package br.com.example.autogestao_backend.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    public TokenResposta token(CsrfToken token) {
        return new TokenResposta(token.getHeaderName(), token.getToken());
    }

    public record TokenResposta(String headerName, String token) {}
}
