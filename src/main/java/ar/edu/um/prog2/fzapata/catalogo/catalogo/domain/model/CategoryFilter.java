package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// El criterio es opcional; sin él se devuelven todas las categorías, también las deshabilitadas
public record CategoryFilter(Boolean enabled) {
}
