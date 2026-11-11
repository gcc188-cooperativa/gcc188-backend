package br.ufla.cooperativa.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduz exceções em application/problem+json (RFC 9457) com correlation_id (Design, tradução de exceções). */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<ProblemDetail> resposta(
            HttpStatus status, String titulo, String detalhe, String codigoRegra, List<Map<String, String>> campos) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(titulo);
        if (codigoRegra != null) pd.setProperty("codigo_regra", codigoRegra);
        if (campos != null && !campos.isEmpty()) pd.setProperty("campos_invalidos", campos);
        String corr = MDC.get("correlation_id");
        if (corr != null) pd.setProperty("correlation_id", corr);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private static List<Map<String, String>> campo(String campo, String mensagem) {
        return campo == null ? null : List.of(Map.of("campo", campo, "mensagem", mensagem));
    }

    static String snake(String s) {
        return s.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex) {
        List<Map<String, String>> campos = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> campos.add(Map.of(
                "campo", snake(f.getField()),
                "mensagem", f.getDefaultMessage() == null ? "inválido" : f.getDefaultMessage())));
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", "Há campos inválidos", null, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> corpo(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corpo da requisição ausente ou malformado", null, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> tipo(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para o parâmetro " + ex.getName(),
                null, campo(ex.getName(), "valor inválido"));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
        NoResourceFoundException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> spring(Exception ex) {
        ErrorResponse er = (ErrorResponse) ex;
        HttpStatus st = HttpStatus.valueOf(er.getStatusCode().value());
        return resposta(st, st.getReasonPhrase(), er.getBody().getDetail() == null ? st.getReasonPhrase() : er.getBody().getDetail(), null, null);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    ResponseEntity<ProblemDetail> invalida(RequisicaoInvalidaException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), null, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ProblemDetail> regra(RegraNegocioException ex) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(),
                ex.getCodigoRegra(), campo(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ProblemDetail> conflito(ConflitoException ex) {
        return resposta(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), null, campo(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> versao(OptimisticLockingFailureException ex) {
        return resposta(HttpStatus.CONFLICT, "Conflito", "O registro foi alterado por outra operação; recarregue e tente de novo", null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade", ex);
        return resposta(HttpStatus.CONFLICT, "Conflito", "A operação viola uma restrição de integridade dos dados", null, null);
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ProblemDetail> naoEncontrado(NaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), null, null);
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    ResponseEntity<ProblemDetail> naoAutenticado(NaoAutenticadoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage(), null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> negado(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta operação", null, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        log.error("Falha inesperada", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Falha inesperada; informe o correlation_id ao suporte", null, null);
    }
}
