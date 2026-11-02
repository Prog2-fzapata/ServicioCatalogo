package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;

public interface CatedraApiClient {

    // Falla con CatedraException (application.exception) si la cátedra no responde o responde algo inválido
    CatalogSnapshot fetchSnapshot();
}
