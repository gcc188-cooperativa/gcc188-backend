package br.ufla.cooperativa.auth.dto;

import br.ufla.cooperativa.shared.Papel;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {}

    public record AlterarSenhaRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {}

    public record MeResponse(Long id, String nome, String email, Papel papel, Long vinculo, boolean deveTrocarSenha) {}
}
