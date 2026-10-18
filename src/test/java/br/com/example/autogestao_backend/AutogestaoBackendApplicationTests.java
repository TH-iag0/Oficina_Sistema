package br.com.example.autogestao_backend;

import br.com.example.autogestao_backend.support.IntegrationApiTest;
import br.com.example.autogestao_backend.service.FinanceiroService;
import br.com.example.autogestao_backend.service.FuncionarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@IntegrationApiTest
class AutogestaoBackendApplicationTests {

	@Test
	void contextLoads(@Autowired ApplicationContext contexto) {
		assertNotNull(contexto.getBean(FinanceiroService.class));
		assertNotNull(contexto.getBean(FuncionarioService.class));
		assertNotNull(contexto.getBean(SecurityFilterChain.class));
	}

}
