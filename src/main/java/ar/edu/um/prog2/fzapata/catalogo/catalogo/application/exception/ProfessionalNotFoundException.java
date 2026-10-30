package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

public class ProfessionalNotFoundException extends RuntimeException {

    public ProfessionalNotFoundException(Long id) {
        super("No existe el profesional con id " + id);
    }
}
