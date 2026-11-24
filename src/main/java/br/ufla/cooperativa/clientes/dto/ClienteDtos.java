package br.ufla.cooperativa.clientes.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class ClienteDtos {
    private ClienteDtos() {}

    /** Para o motorista, apenas identificação: demais campos vêm nulos (RF-CLI-02). */
    public record ClienteResponse(Long id, Long usuarioId, String nome, String documento, String telefone,
            String emailContato, boolean ativo, Instant criadoEm, Instant atualizadoEm) {}

    /** Credenciais no formato do contrato OpenAPI (objeto `usuario`). */
    public record UsuarioCredenciais(@NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 72) String senha) {}

    /**
     * Aceita `usuario: {email, senha}` (contrato) e, por compatibilidade, `email`/`senha` na raiz.
     */
    public record CadastrarClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 (CPF) ou 14 (CNPJ) dígitos") String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 160) String emailContato,
            String email,
            String senha,
            @Valid UsuarioCredenciais usuario) {

        @JsonIgnore
        @AssertTrue(message = "informe email e senha (ou usuario.email e usuario.senha)")
        public boolean isCredenciaisValidas() {
            return (usuario != null && usuario.email() != null && !usuario.email().isBlank()
                    && usuario.senha() != null && !usuario.senha().isBlank())
                    || (email != null && !email.isBlank() && senha != null && !senha.isBlank());
        }

        public String emailEfetivo() {
            return usuario != null ? usuario.email() : email;
        }

        public String senhaEfetivo() {
            return usuario != null ? usuario.senha() : senha;
        }
    }

    public record AtualizarClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 (CPF) ou 14 (CNPJ) dígitos") String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 160) String emailContato) {}
}
