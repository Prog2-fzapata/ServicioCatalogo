package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.TreeSet;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtClaims;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtProperties;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.TokenIssuer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Override
    public String issueToken(User user, boolean rememberMe) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(rememberMe ? properties.rememberMeValidity() : properties.tokenValidity());

        // Sin nombres en el token: un claim queda fijo al emitirlo (D34); el nombre se consulta en /api/account
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getLogin())
                .claim(JwtClaims.USER_ID, user.getId())
                .claim(JwtClaims.AUTHORITIES, String.join(" ", new TreeSet<>(user.getAuthorities())))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(JwtConfiguration.ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
