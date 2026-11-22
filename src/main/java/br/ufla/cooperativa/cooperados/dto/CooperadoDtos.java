package br.ufla.cooperativa.cooperados.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class CooperadoDtos {
    private CooperadoDtos() {}

    public record CooperadoResponse(Long id, Long usuarioId, String matricula, String nome, String cpf,
            String telefone, LocalDate dataIngresso, boolean ativo, Instant criadoEm, Instant atualizadoEm) {}

    public record CadastrarCooperadoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank String senha) {}

    public record AtualizarCooperadoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso) {}
}
