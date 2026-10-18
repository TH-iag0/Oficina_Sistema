package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

final class RegrasModel {
    private RegrasModel() {}
    static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo)
            throw new IllegalArgumentException(campo + " obrigatório, com até " + maximo + " caracteres.");
        return valor.strip();
    }
    static String opcional(String valor, int maximo, String campo) {
        return valor == null || valor.isBlank() ? null : texto(valor, maximo, campo);
    }
    static BigDecimal dinheiro(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("Valor não pode ser nulo ou negativo.");
        }
        try {
            BigDecimal resultado = valor.setScale(2, RoundingMode.UNNECESSARY);
            if (resultado.precision() > 15) {
                throw new IllegalArgumentException("Valor excede o limite monetário.");
            }
            return resultado;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Valores monetários devem ter até duas casas decimais.", e);
        }
    }
    static BigDecimal positivo(BigDecimal valor) {
        BigDecimal resultado = dinheiro(valor);
        if (resultado.signum() == 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero.");
        }
        return resultado;
    }
    static void cronologia(Instant anterior, Instant proximo) {
        if (proximo == null || proximo.isBefore(anterior)) {
            throw new IllegalArgumentException("Horário anterior ao último registro.");
        }
    }
}
