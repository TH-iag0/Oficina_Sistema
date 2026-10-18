package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class ExclusaoFuncionarioIntegrationTest {
    private final MockMvc mvc;
    private final FuncionarioRepository funcionarios;

    @Autowired
    ExclusaoFuncionarioIntegrationTest(MockMvc mvc, FuncionarioRepository funcionarios) {
        this.mvc = mvc;
        this.funcionarios = funcionarios;
    }

    @Test
    void excluiFuncionarioDeTesteSemHistorico() throws Exception {
        var funcionario = cadastrar();
        mvc.perform(delete(rota(funcionario.getId())).with(csrf())).andExpect(status().isNoContent());
        assertFalse(funcionarios.existsById(funcionario.getId()));
    }

    @Test
    void exigeCsrfERecusaFuncionarioInexistente() throws Exception {
        var funcionario = cadastrar();
        mvc.perform(delete(rota(funcionario.getId()))).andExpect(status().isForbidden());
        mvc.perform(delete(rota(UUID.randomUUID())).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void preservaFuncionarioComJornada(@Autowired JornadaTrabalhoRepository jornadas) throws Exception {
        var funcionario = cadastrar();
        jornadas.saveAndFlush(new JornadaTrabalho(funcionario, DayOfWeek.MONDAY,
            LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5, LocalDate.of(2026, 10, 1)));
        mvc.perform(delete(rota(funcionario.getId())).with(csrf())).andExpect(status().isConflict());
    }

    private Funcionario cadastrar() {
        return funcionarios.saveAndFlush(new Funcionario("Funcionário teste", UUID.randomUUID().toString().replace("-", ""),
            new BCryptPasswordEncoder(4).encode("123456")));
    }

    private String rota(UUID id) {
        return "/api/funcionarios/" + id + "/exclusao";
    }
}
