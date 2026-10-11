package br.com.example.autogestao_backend.model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class MarcacaoPontoTest {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant INSTANTE = Instant.parse("2026-10-11T01:30:00Z");
    private static final Clock RELOGIO = Clock.fixed(INSTANTE, ZoneOffset.UTC);
    private static final String HASH = new BCryptPasswordEncoder(4).encode("123456");

    private Funcionario funcionario() { return new Funcionario("João", "OF-001", HASH); }

    @Test
    void funcionarioInativoNaoPodeRegistrarPonto() {
        var funcionario = funcionario();
        funcionario.desativar();
        assertThrows(IllegalStateException.class, () -> MarcacaoPonto.registrar(funcionario,
            TipoMarcacao.ENTRADA, UUID.randomUUID(), RELOGIO, FUSO));
    }

    @ParameterizedTest
    @EnumSource(TipoMarcacao.class)
    void registraEtapaComFuncionarioEConfirmacaoInformados(TipoMarcacao tipo) {
        var funcionario = funcionario();
        var requisicao = UUID.randomUUID();
        var marcacao = MarcacaoPonto.registrar(funcionario, tipo, requisicao, RELOGIO, FUSO);

        assertNotNull(marcacao.getId());
        assertSame(funcionario, marcacao.getFuncionario());
        assertEquals(funcionario.getId(), marcacao.getFuncionarioId());
        assertEquals(tipo, marcacao.getTipo());
        assertEquals(requisicao, marcacao.getRequisicaoId());
        assertEquals(INSTANTE, marcacao.getInstante());
    }

    @Test
    void usaDiaLocalMesmoQuandoUtcJaEstaNoDiaSeguinte() {
        var marcacao = MarcacaoPonto.registrar(funcionario(), TipoMarcacao.ENTRADA,
            UUID.randomUUID(), RELOGIO, FUSO);

        assertEquals(LocalDate.of(2026, 10, 10), marcacao.getDataRegistro());
    }

    @Test
    void respeitaFusoConfiguradoNoServidorEmVezDoFusoDoRelogio() {
        var marcacao = MarcacaoPonto.registrar(funcionario(), TipoMarcacao.ENTRADA,
            UUID.randomUUID(), RELOGIO.withZone(ZoneId.of("Asia/Tokyo")), FUSO);

        assertEquals(INSTANTE, marcacao.getInstante());
        assertEquals(LocalDate.of(2026, 10, 10), marcacao.getDataRegistro());
    }

    @Test
    void rejeitaDadosObrigatoriosAusentes() {
        var funcionario = funcionario();
        var requisicao = UUID.randomUUID();
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> MarcacaoPonto.registrar(null, TipoMarcacao.ENTRADA, requisicao, RELOGIO, FUSO)),
            () -> assertThrows(NullPointerException.class, () -> MarcacaoPonto.registrar(funcionario, null, requisicao, RELOGIO, FUSO)),
            () -> assertThrows(NullPointerException.class, () -> MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, null, RELOGIO, FUSO)),
            () -> assertThrows(NullPointerException.class, () -> MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, requisicao, null, FUSO)),
            () -> assertThrows(NullPointerException.class, () -> MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, requisicao, RELOGIO, null))
        );
    }
}
