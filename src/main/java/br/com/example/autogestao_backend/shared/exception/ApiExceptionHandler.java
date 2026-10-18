package br.com.example.autogestao_backend.shared.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    @ExceptionHandler(ErroApi.class)
    public ProblemDetail tratarErroApi(ErroApi erro) {
        return ProblemDetail.forStatusAndDetail(erro.getStatus(), erro.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
        List<CampoInvalido> campos = erro.getBindingResult().getFieldErrors().stream()
            .map(campo -> new CampoInvalido(campo.getField(), campo.getDefaultMessage())).toList();
        problema.setProperty("erros", campos);
        return problema;
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class,
        HandlerMethodValidationException.class})
    public ProblemDetail tratarEntrada(Exception erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetros ou dados inválidos.");
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class,
        OptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class})
    public ProblemDetail tratarConflito(Exception erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A operação conflita com os registros existentes.");
    }

    private record CampoInvalido(String campo, String mensagem) {}
}
