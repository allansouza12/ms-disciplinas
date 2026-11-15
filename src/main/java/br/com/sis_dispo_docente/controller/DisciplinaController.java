package br.com.sis_dispo_docente.controller;

import br.com.sis_dispo_docente.dto.DisciplinaDto;
import br.com.sis_dispo_docente.dto.DisciplinaReq;
import br.com.sis_dispo_docente.service.DisciplinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplinas")
@RequiredArgsConstructor
public class DisciplinaController {

    private final DisciplinaService service;

    @GetMapping
    public List<DisciplinaDto> listar(@RequestParam(required = false) Long cursoId) {
        return service.listar(cursoId).stream().map(DisciplinaDto::de).toList();
    }

    /** Consultado pelo orquestrador: o curso X tem disciplinas? */
    @GetMapping("/existe")
    public boolean existe(@RequestParam Long cursoId) {
        return service.cursoPossuiDisciplinas(cursoId);
    }

    @GetMapping("/{id}")
    public DisciplinaDto buscar(@PathVariable Long id) {
        return DisciplinaDto.de(service.buscar(id));
    }

    @PostMapping
    public DisciplinaDto criar(@RequestBody @Valid DisciplinaReq req) {
        return DisciplinaDto.de(service.criar(req));
    }

    @PutMapping("/{id}")
    public DisciplinaDto atualizar(@PathVariable Long id, @RequestBody @Valid DisciplinaReq req) {
        return DisciplinaDto.de(service.atualizar(id, req));
    }

    @PatchMapping("/{id}/inativar")
    public DisciplinaDto inativar(@PathVariable Long id) {
        return DisciplinaDto.de(service.inativar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
