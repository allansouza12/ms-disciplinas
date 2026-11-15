package br.com.sis_dispo_docente.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplinaReq(
        @NotBlank @Size(max = 20) String sigla,
        @NotBlank String descricao,
        @NotNull Long cursoId,
        @NotNull @Min(1) Integer cargaHoraria) {
}
