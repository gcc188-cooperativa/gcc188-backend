package br.ufla.cooperativa.usuarios.service;

import br.ufla.cooperativa.shared.AppProperties;
import br.ufla.cooperativa.shared.Papel;
import br.ufla.cooperativa.usuarios.persistence.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria o administrador inicial no primeiro boot, a partir de variáveis de ambiente (D17). Idempotente. */
@Component
public class AdministradorInicial implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);
    private final UsuarioRepository repo;
    private final UsuarioService usuarios;
    private final AppProperties props;

    public AdministradorInicial(UsuarioRepository repo, UsuarioService usuarios, AppProperties props) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = props.bootstrap().admin();
        if (admin == null || blank(admin.email()) || blank(admin.password())) return;
        if (repo.existsByPapel(Papel.ADMINISTRADOR)) return;
        usuarios.criarInterno("Administrador", admin.email(), admin.password(), Papel.ADMINISTRADOR);
        log.info("Administrador inicial criado");
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
