package br.ufla.cooperativa.usuarios.dto;

import br.ufla.cooperativa.shared.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UsuarioDtos {
    private UsuarioDtos() {}

    public record UsuarioResponse(Long id, String nome, String email, Papel papel, boolean ativo,
            boolean deveTrocarSenha, Long versao, Instant criadoEm, Instant atualizadoEm) {}

    public record CriarUsuarioRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank String senha,
            @NotNull Papel papel) {}

    public record AtualizarUsuarioRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotNull Papel papel,
            @NotNull Long versao) {}

    public record RedefinirSenhaRequest(@NotBlank String senha) {}
}
