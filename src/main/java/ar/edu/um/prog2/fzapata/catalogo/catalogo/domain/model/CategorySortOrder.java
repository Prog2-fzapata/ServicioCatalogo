package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

// «Nombre ascendente»: un campo y una dirección. Los campos son los únicos por los que se puede ordenar
public record CategorySortOrder(Field field, Direction direction) {

    public enum Field {
        ID,
        NAME
    }

    public enum Direction {
        ASC,
        DESC
    }

    // Orden por defecto: por nombre
    public static List<CategorySortOrder> byName() {
        return List.of(new CategorySortOrder(Field.NAME, Direction.ASC));
    }
}
