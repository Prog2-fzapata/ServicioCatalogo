package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

// Mensaje que no se puede interpretar: reintentarlo no lo arregla, así que se descarta sin reintentos
public class InvalidCatalogEventException extends RuntimeException {

    public InvalidCatalogEventException(String message) {
        super(message);
    }

    public InvalidCatalogEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
