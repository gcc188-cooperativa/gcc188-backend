package br.ufla.cooperativa.shared;

/** Regra de negócio violada: HTTP 422. */
public class RegraNegocioException extends RuntimeException {
    private final String codigoRegra;
    private final String campo;

    public RegraNegocioException(String codigoRegra, String campo, String mensagem) {
        super(mensagem);
        this.codigoRegra = codigoRegra;
        this.campo = campo;
    }

    public String getCodigoRegra() { return codigoRegra; }
    public String getCampo() { return campo; }
}
