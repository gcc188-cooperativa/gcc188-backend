package br.ufla.cooperativa.shared;

/** Recurso inexistente ou fora do escopo do usuário: HTTP 404. */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) { super(mensagem); }
}
