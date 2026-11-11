package br.ufla.cooperativa.cooperandos.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class CooperandoDtos {
    private CooperandoDtos() {}

    public record CooperandoResponse(Long id, Long usuarioId, String matricula, String nome, String cpf,
            String telefone, LocalDate dataIngresso, boolean ativo, Instant criadoEm, Instant atualizadoEm) {}

    public record CadastrarCooperandoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank String senha) {}

    public record AtualizarCooperandoRequest(
            @NotBlank @Size(max = 20) String matricula,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
            @Size(max = 20) String telefone,
            @NotNull LocalDate dataIngresso) {}
}
