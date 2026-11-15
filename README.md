# ms-disciplina

Microsserviço de **Disciplinas ligadas a um curso** do sistema de disponibilidade docente (TED 2ª unidade: do monolítico ao microsserviço).

| | |
|---|---|
| **Nome no Eureka** (`spring.application.name`) | `ms-disciplina` (o repositório se chama `ms-disciplinas`) |
| **Porta** | `8082` |
| **Banco** | MySQL, `ms_disciplina` (criado automaticamente) |
| **Tabela** | `disciplina` (única por `sigla` + `curso_id`) |
| **Stack** | Java 17, Spring Boot 3.3.4, Spring Cloud 2023.0.3, Spring Data JPA, Eureka Client |

> Este serviço **não chama outros microsserviços**. Ele só responde às requisições do **orquestrador** (API Gateway).
> A disciplina guarda apenas o `cursoId` (referência lógica, **sem chave estrangeira**): o curso vive no `ms-curso`, em outro banco.
> Quem garante que o curso existe e está ATIVO antes de criar/editar uma disciplina é o **orquestrador**.

---

## Como rodar

**Pré-requisitos:** Java 17, Maven, MySQL ligado em `localhost:3306`.

1. Clone o repositório e abra no VS Code / IntelliJ.
2. Em `src/main/resources`, copie `secrets.yml.example` para **`secrets.yml`** (este arquivo é ignorado pelo Git) e coloque a senha do seu MySQL:
   ```yaml
   spring:
     datasource:
       password: SUA_SENHA_DO_MYSQL
   ```
3. Aponte para o Eureka do grupo. Por padrão é `http://localhost:8761/eureka`. Para outro endereço, adicione no `secrets.yml`:
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://IP-DO-DISCOVERY:8761/eureka
   ```
   Para testar **sem** Eureka, use:
   ```yaml
   eureka:
     client:
       enabled: false
   ```
4. Rode a classe `SisDispoDocenteApplication` (ou `mvn spring-boot:run`).
5. Confira: `http://localhost:8082/api/disciplinas` responde e, no painel do Eureka (`http://localhost:8761`), aparece **MS-DISCIPLINA** como UP.

Se rodar em máquina diferente da do Eureka/Gateway, libere a porta 8082 no firewall (o serviço registra o IP, `prefer-ip-address: true`).

---

## Endpoints

Base: `http://localhost:8082` (ou, via Eureka, `lb://ms-disciplina`)

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| GET | `/api/disciplinas` | Lista (ordenada por sigla). Filtro opcional `?cursoId=1` | 200 |
| GET | `/api/disciplinas/existe?cursoId=1` | O curso tem disciplinas? Devolve `true` ou `false` | 200 |
| GET | `/api/disciplinas/{id}` | Busca uma disciplina | 200 / 404 |
| POST | `/api/disciplinas` | Cria | 200 / 409 |
| PUT | `/api/disciplinas/{id}` | Edita | 200 / 404 / 409 |
| PATCH | `/api/disciplinas/{id}/inativar` | Muda o status para `INATIVO` | 200 / 404 |
| DELETE | `/api/disciplinas/{id}` | Exclui | 204 / 404 |

### Corpo do POST / PUT

```json
{
  "sigla": "POO",
  "descricao": "Programacao Orientada a Objetos",
  "cursoId": 1,
  "cargaHoraria": 60
}
```

| Campo | Regra |
|---|---|
| `sigla` | obrigatória, até 20 caracteres (salva em maiúsculas); **única dentro do mesmo curso** |
| `descricao` | obrigatória |
| `cursoId` | obrigatório (id do curso, que vive no `ms-curso`) |
| `cargaHoraria` | obrigatória, mínimo 1 |

### Resposta

```json
{
  "id": 1,
  "sigla": "POO",
  "descricao": "Programacao Orientada a Objetos",
  "cursoId": 1,
  "cargaHoraria": 60,
  "dataCadastro": "2026-11-15",
  "status": "ATIVO"
}
```

### Erros

Sempre no formato `{ "mensagem": "..." }`:

| Status | Quando |
|---|---|
| 400 | corpo inválido (campo faltando, `cargaHoraria` < 1...) |
| 404 | disciplina não encontrada |
| 409 | sigla já existe nesse curso / registro duplicado |

---

## O que o orquestrador precisa fazer

As regras que envolvem **mais de um serviço** ficam no orquestrador (API Gateway), não aqui:

1. **Criar/editar disciplina** (`POST/PUT /api/disciplinas` no gateway)
   1. `GET lb://ms-curso/api/cursos/{cursoId}`
   2. se `404` → responder **404** "Curso não encontrado."
   3. se `status != ATIVO` → responder **409** "Não é possível vincular uma disciplina a um curso inativo."
   4. senão → `POST/PUT lb://ms-disciplina/api/disciplinas` com o mesmo corpo
2. **Excluir curso** (`DELETE /api/cursos/{id}` no gateway): antes de apagar no `ms-curso`, chamar `GET lb://ms-disciplina/api/disciplinas/existe?cursoId={id}`; se `true` → **409** "O curso possui disciplinas vinculadas. Ele não pode ser excluído, apenas inativado."
3. **Listar disciplinas com os dados do curso** (ex.: `cursoSigla`):
   1. `GET lb://ms-disciplina/api/disciplinas`
   2. para cada `cursoId` distinto: `GET lb://ms-curso/api/cursos/{id}`
   3. montar a resposta junta no gateway

Rota simples no gateway (o `lb://` resolve o nome pelo Eureka):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: disciplinas
          uri: lb://ms-disciplina
          predicates: [ Path=/api/disciplinas/** ]
```

> O Spring Cloud Gateway é reativo (WebFlux): para chamar os serviços por código, use `WebClient` com `@LoadBalanced`.

---

## Observações

- No monólito, excluir disciplina era bloqueado se houvesse `Interesse`/`MapaItem` vinculados. Essas tabelas pertencem a outro contexto (docentes/mapa) e não existem aqui; essa verificação, se necessária, é do orquestrador.
- Autenticação/token **não** faz parte deste serviço (fica no gateway ou no microsserviço de segurança do grupo).
- Versões que **todos** os projetos do grupo devem usar: Java 17, Spring Boot 3.3.4, Spring Cloud 2023.0.3.