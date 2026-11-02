package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// weeklySchedules trae todos los horarios, también los deshabilitados
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalDetailResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private List<WeeklyScheduleDetailResponse> weeklySchedules;
}
