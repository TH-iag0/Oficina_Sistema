package br.com.example.autogestao_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AcessoLocalFilter extends OncePerRequestFilter {
    static boolean local(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(peer) || "::1".equals(peer)
            || "0:0:0:0:0:0:0:1".equals(peer);
        boolean encaminhada = request.getHeaderNames() != null
            && java.util.Collections.list(request.getHeaderNames()).stream()
                .map(nome -> nome.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(nome -> nome.equals("forwarded") || nome.startsWith("x-forwarded-")
                    || nome.startsWith("cf-"));
        String host = request.getServerName();
        boolean hostLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
            || "[::1]".equals(host) || "::1".equals(host);
        return loopback && hostLocal && !encaminhada;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        if (local(request) && SecurityContextHolder.getContext().getAuthentication() == null) {
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(new UsernamePasswordAuthenticationToken("proprietario-local", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            SecurityContextHolder.setContext(contexto);
        }
        chain.doFilter(request, response);
    }
}
