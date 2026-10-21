package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// Quien llama decide qué registrar: si newVersion es menor que previousVersion la cátedra retrocedió
public record SyncResult(long previousVersion, long newVersion) {
}
