package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;

public interface CatalogRepository {

    // Deja la copia local igual al snapshot: crea y actualiza lo que llega y borra lo que ya no llega.
    // No abre transacción: participa de la del caso de uso
    void replaceAll(CatalogSnapshot snapshot);
}
