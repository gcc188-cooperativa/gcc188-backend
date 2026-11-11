package br.ufla.cooperativa.shared;

/** Transição inválida, versão divergente ou unicidade violada: HTTP 409. */
public class ConflitoException extends RuntimeException {
    private final String campo;

    public ConflitoException(String mensagem, String campo) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() { return campo; }
}
