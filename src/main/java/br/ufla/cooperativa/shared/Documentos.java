package br.ufla.cooperativa.shared;

/** Validação dos dígitos verificadores de CPF e CNPJ (somente dígitos). */
public final class Documentos {
    private Documentos() {}

    public static boolean cpfValido(String c) {
        if (c == null || !c.matches("\\d{11}") || c.chars().distinct().count() == 1) return false;
        return dvCpf(c, 9, 10) == c.charAt(9) - '0' && dvCpf(c, 10, 11) == c.charAt(10) - '0';
    }

    private static int dvCpf(String s, int n, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < n; i++) soma += (s.charAt(i) - '0') * (pesoInicial - i);
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    public static boolean cnpjValido(String c) {
        if (c == null || !c.matches("\\d{14}") || c.chars().distinct().count() == 1) return false;
        return dvCnpj(c, 12) == c.charAt(12) - '0' && dvCnpj(c, 13) == c.charAt(13) - '0';
    }

    private static int dvCnpj(String s, int n) {
        int[] w = n == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < n; i++) soma += (s.charAt(i) - '0') * w[i];
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    public static boolean cpfOuCnpjValido(String d) {
        return d != null && (d.length() == 11 ? cpfValido(d) : cnpjValido(d));
    }
}
