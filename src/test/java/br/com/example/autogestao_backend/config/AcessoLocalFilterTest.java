package br.com.example.autogestao_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcessoLocalFilterTest {
    @Test
    void aceitaSomenteLoopbackDireto() {
        var request = new MockHttpServletRequest();
        for (String ip : new String[] {"127.0.0.1", "::1", "0:0:0:0:0:0:0:1"}) {
            request.setRemoteAddr(ip);
            assertTrue(AcessoLocalFilter.local(request));
        }
        request.setRemoteAddr("192.168.1.10");
        assertFalse(AcessoLocalFilter.local(request));
    }

    @Test
    void recusaHostPublicoMesmoComConexaoLocal() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setServerName("oficina.example.com");
        assertFalse(AcessoLocalFilter.local(request));
    }

    @Test
    void recusaTunelMesmoQuandoProxyConectaPorLoopback() {
        for (String header : new String[] {"Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "CF-Connecting-IP"}) {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.1");
            request.addHeader(header, "127.0.0.1");
            assertFalse(AcessoLocalFilter.local(request));
        }
    }
}
