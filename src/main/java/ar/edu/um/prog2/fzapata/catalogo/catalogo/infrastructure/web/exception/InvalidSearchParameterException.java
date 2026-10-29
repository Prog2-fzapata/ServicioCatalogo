package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception;

// Un parámetro de búsqueda que no se puede usar: se responde 400 con el campo y el motivo
public class InvalidSearchParameterException extends RuntimeException {

    private final String field;

    public InvalidSearchParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
