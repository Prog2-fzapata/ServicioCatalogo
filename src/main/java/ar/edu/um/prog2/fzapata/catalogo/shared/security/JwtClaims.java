package ar.edu.um.prog2.fzapata.catalogo.shared.security;

// Nombres de los claims propios del JWT; los comparten quien lo emite y quien lo lee
public final class JwtClaims {

    public static final String USER_ID = "uid";
    // Autoridades separadas por espacio, como el JWT de JHipster
    public static final String AUTHORITIES = "auth";

    private JwtClaims() {
    }
}
