package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenResponse {

    // Nombre del campo igual al de JHipster
    @JsonProperty("id_token")
    private String idToken;
}
