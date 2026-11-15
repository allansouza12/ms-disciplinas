package br.com.sis_dispo_docente.dto;

import br.com.sis_dispo_docente.domain.Disciplina;
import br.com.sis_dispo_docente.domain.Status;

import java.time.LocalDate;

public record DisciplinaDto(Long id, String sigla, String descricao, Long cursoId,
                            int cargaHoraria, LocalDate dataCadastro, Status status) {

    public static DisciplinaDto de(Disciplina d) {
        return new DisciplinaDto(d.getId(), d.getSigla(), d.getDescricao(), d.getCursoId(),
                d.getCargaHoraria(), d.getDataCadastro(), d.getStatus());
    }
}
