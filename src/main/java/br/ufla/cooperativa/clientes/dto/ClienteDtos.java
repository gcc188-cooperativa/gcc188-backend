package br.ufla.cooperativa.clientes.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class ClienteDtos {
    private ClienteDtos() {}

    /** Para o motorista, apenas identificação: demais campos vêm nulos (RF-CLI-02). */
    public record ClienteResponse(Long id, Long usuarioId, String nome, String documento, String telefone,
            String emailContato, boolean ativo, Instant criadoEm, Instant atualizadoEm) {}

    public record CadastrarClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 (CPF) ou 14 (CNPJ) dígitos") String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 160) String emailContato,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank String senha) {}

    public record AtualizarClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve conter 11 (CPF) ou 14 (CNPJ) dígitos") String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 160) String emailContato) {}
}
