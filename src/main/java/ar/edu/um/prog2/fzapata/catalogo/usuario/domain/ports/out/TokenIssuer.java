package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

public interface TokenIssuer {
    String issueToken(User user, boolean rememberMe);
}
