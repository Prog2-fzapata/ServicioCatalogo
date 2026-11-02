package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// Todos los criterios son opcionales; sin ninguno se devuelven todos los profesionales. Un nombre en blanco no filtra
public record ProfessionalFilter(Long categoryId, String name, Boolean enabled, Boolean available) {

    public ProfessionalFilter {
        name = name == null || name.isBlank() ? null : name.trim();
    }
}
