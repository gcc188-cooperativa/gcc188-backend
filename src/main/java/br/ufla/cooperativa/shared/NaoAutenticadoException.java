package br.ufla.cooperativa.shared;

/** Credenciais ou token inválidos: HTTP 401. */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String mensagem) { super(mensagem); }
}
