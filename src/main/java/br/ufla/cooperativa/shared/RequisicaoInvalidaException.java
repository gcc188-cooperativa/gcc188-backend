package br.ufla.cooperativa.shared;

/** Requisição malformada (ex.: ordenação não permitida): HTTP 400. */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) { super(mensagem); }
}
