package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

// «Apellido ascendente»: un campo y una dirección. Los campos son los únicos por los que se puede ordenar
public record ProfessionalSortOrder(Field field, Direction direction) {

    public enum Field {
        ID,
        FIRST_NAME,
        LAST_NAME
    }

    public enum Direction {
        ASC,
        DESC
    }

    // Orden por defecto: apellido y nombre
    public static List<ProfessionalSortOrder> byName() {
        return List.of(new ProfessionalSortOrder(Field.LAST_NAME, Direction.ASC),
                new ProfessionalSortOrder(Field.FIRST_NAME, Direction.ASC));
    }
}
