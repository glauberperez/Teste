package com.quipux.cadastro.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centraliza a traducao de excecoes em respostas HTTP, para que o cliente sempre
 * receba um corpo JSON no mesmo formato.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} para que as excecoes padrao do
 * Spring MVC (metodo nao suportado, content-type invalido, rota inexistente,
 * parametro com tipo errado) mantenham o status HTTP correto em vez de cairem no
 * tratamento generico de erro 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------- validacao ---------- */

    /** Falhas de @Valid no corpo da requisicao (POST /registrarName, POST /auth/login). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                "Um ou mais campos estao invalidos",
                caminho(request),
                campos);
        return ResponseEntity.badRequest().headers(headers).body(corpo);
    }

    /** Falhas de validacao em parametros de rota e de query (@Validated no controller). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath().toString();
            String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
            campos.putIfAbsent(campo, violacao.getMessage());
        }
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parametro invalido",
                "Um ou mais parametros estao invalidos",
                request.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    /** Parametro recebido com tipo incompativel, por exemplo ?limite=abc. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        if (ex instanceof MethodArgumentTypeMismatchException erro) {
            campos.put(erro.getName(), "valor invalido para este parametro: " + erro.getValue());
        }
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parametro invalido",
                "Um ou mais parametros estao com o tipo errado",
                caminho(request),
                campos.isEmpty() ? null : campos);
        return ResponseEntity.badRequest().headers(headers).body(corpo);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        return ResponseEntity.badRequest().headers(headers).body(ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                "Corpo da requisicao ausente ou em JSON malformado",
                caminho(request)));
    }

    /* ---------- erros de negocio ---------- */

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResponse.de(
                HttpStatus.NOT_FOUND.value(), "Nao encontrado", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.de(
                HttpStatus.CONFLICT.value(), "Conflito", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ServicoExternoException.class)
    public ResponseEntity<ErroResponse> tratarServicoExterno(
            ServicoExternoException ex, HttpServletRequest request) {

        log.warn("Falha ao consumir servico externo: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErroResponse.de(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Servico externo indisponivel",
                ex.getMessage(),
                request.getRequestURI()));
    }

    /* ---------- fallbacks ---------- */

    /**
     * Demais excecoes padrao do Spring MVC: metodo nao suportado (405),
     * content-type nao suportado (415), rota inexistente (404) etc.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object corpoOriginal, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String mensagem = switch (ex) {
            case HttpRequestMethodNotSupportedException erro ->
                    "O metodo " + erro.getMethod() + " nao e suportado neste recurso";
            case HttpMediaTypeNotSupportedException erro ->
                    "Content-Type nao suportado. Utilize application/json";
            case NoResourceFoundException erro ->
                    "Recurso nao encontrado";
            case MissingServletRequestParameterException erro ->
                    "Parametro obrigatorio ausente: " + erro.getParameterName();
            default -> descricaoDoStatus(status);
        };

        ErroResponse corpo = ErroResponse.de(
                status.value(), descricaoDoStatus(status), mensagem, caminho(request));
        return ResponseEntity.status(status).headers(headers).body(corpo);
    }

    /** Rede de seguranca: nada deve vazar stack trace para o cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErroResponse.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao",
                request.getRequestURI()));
    }

    /* ---------- apoio ---------- */

    private static String caminho(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    private static String descricaoDoStatus(HttpStatusCode status) {
        HttpStatus resolvido = HttpStatus.resolve(status.value());
        return resolvido == null ? "Erro" : resolvido.getReasonPhrase();
    }
}
