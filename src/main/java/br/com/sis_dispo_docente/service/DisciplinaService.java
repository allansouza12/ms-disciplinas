package br.com.sis_dispo_docente.service;

import br.com.sis_dispo_docente.domain.Disciplina;
import br.com.sis_dispo_docente.domain.Status;
import br.com.sis_dispo_docente.dto.DisciplinaReq;
import br.com.sis_dispo_docente.exception.NaoEncontradoException;
import br.com.sis_dispo_docente.exception.RegraNegocioException;
import br.com.sis_dispo_docente.repository.DisciplinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private final DisciplinaRepository repository;

    public List<Disciplina> listar(Long cursoId) {
        return cursoId == null ? repository.findAllByOrderBySiglaAsc()
                : repository.findByCursoIdOrderBySiglaAsc(cursoId);
    }

    public Disciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada."));
    }

    /** Consultado pelo ORQUESTRADOR antes de pedir a exclusão de um curso ao ms-curso. */
    public boolean cursoPossuiDisciplinas(Long cursoId) {
        return repository.existsByCursoId(cursoId);
    }

    @Transactional
    public Disciplina criar(DisciplinaReq req) {
        Disciplina d = new Disciplina();
        preencher(d, req);
        d.setDataCadastro(LocalDate.now());
        return repository.save(d);
    }

    @Transactional
    public Disciplina atualizar(Long id, DisciplinaReq req) {
        Disciplina d = buscar(id);
        preencher(d, req);
        return d;
    }

    @Transactional
    public Disciplina inativar(Long id) {
        Disciplina d = buscar(id);
        d.setStatus(Status.INATIVO);
        return d;
    }

    @Transactional
    public void excluir(Long id) {
        // No monólito, bloqueava se houvesse Interesse/MapaItem vinculados. Essas tabelas pertencem
        // a outro contexto (docentes/mapa) e não existem neste serviço.
        repository.delete(buscar(id));
    }

    private void preencher(Disciplina d, DisciplinaReq req) {
        String sigla = req.sigla().trim().toUpperCase();

        // Validar se o curso existe e está ATIVO é papel do ORQUESTRADOR (consulta o ms-curso antes de chamar aqui).

        boolean duplicada = d.getId() == null
                ? repository.existsBySiglaIgnoreCaseAndCursoId(sigla, req.cursoId())
                : repository.existsBySiglaIgnoreCaseAndCursoIdAndIdNot(sigla, req.cursoId(), d.getId());
        if (duplicada) {
            throw new RegraNegocioException("Já existe uma disciplina com essa sigla nesse curso.");
        }

        d.setSigla(sigla);
        d.setDescricao(req.descricao().trim());
        d.setCursoId(req.cursoId());
        d.setCargaHoraria(req.cargaHoraria());
    }
}
