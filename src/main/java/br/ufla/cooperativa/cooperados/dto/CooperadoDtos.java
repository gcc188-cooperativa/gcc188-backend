package br.ufla.cooperativa.cooperados.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class CooperadoDtos {
    private CooperadoDtos() {}

    public record CooperadoResponse(Long id, Long usuarioId, String matricula, String nome, String cpf,
            String telefone, LocalDate dataIngresso, boolean ativo, Instant criadoEm, Instant atualizadoEm) {}

    /** Credenciais no formato do contrato OpenAPI (objeto `usuario`). */
    public record UsuarioCredenciais(@NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 72) String senha) {}

    /**
     * Aceita `usuario: {email, senha}` (contrato) e, por compatibilidade, `email`/`senha` na raiz.
     */
    public record CadastrarCooperadoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso,
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

    public record AtualizarCooperadoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso) {}
}
