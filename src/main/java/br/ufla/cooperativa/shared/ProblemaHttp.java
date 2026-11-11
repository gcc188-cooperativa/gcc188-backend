package br.ufla.cooperativa.shared;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;

/** Escreve um problem+json (RFC 9457) fora do MVC, para filtros e handlers de segurança. */
public final class ProblemaHttp {
    private ProblemaHttp() {}

    public static void escrever(HttpServletResponse resp, int status, String titulo, String detalhe, String codigoRegra)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/problem+json");
        resp.setCharacterEncoding("UTF-8");
        String corr = MDC.get("correlation_id");
        StringBuilder sb = new StringBuilder("{\"type\":\"about:blank\",\"title\":").append(q(titulo))
                .append(",\"status\":").append(status)
                .append(",\"detail\":").append(q(detalhe));
        if (codigoRegra != null) sb.append(",\"codigo_regra\":").append(q(codigoRegra));
        if (corr != null) sb.append(",\"correlation_id\":").append(q(corr));
        sb.append('}');
        resp.getWriter().write(sb.toString());
    }

    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(ch < 0x20 ? " " : String.valueOf(ch));
            }
        }
        return sb.append('"').toString();
    }
}
