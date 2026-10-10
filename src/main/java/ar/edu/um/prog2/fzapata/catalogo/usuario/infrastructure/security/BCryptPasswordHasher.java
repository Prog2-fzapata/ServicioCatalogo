package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(preHash(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(preHash(rawPassword), hash);
    }

    // BCrypt rechaza más de 72 bytes. El SHA-256 en Base64 siempre mide 44 caracteres,
    // así que cualquier contraseña (hasta 100 caracteres, con tildes o ñ) entra completa.
    private String preHash(String rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
