# Story map do AgendaSaúde

> Técnica de Jeff Patton (*User Story Mapping*, O'Reilly, 2014): da esquerda para a direita, a ordem em que o
> usuário usa o sistema (o **backbone**); de cima para baixo, da fatia essencial à desejável (as **releases**).

**Problema:** clínicas pequenas agendam por telefone e planilha. Resultado: dois pacientes marcados no mesmo
horário, consultas marcadas no almoço ou no sábado e cancelamentos em cima da hora que deixam o médico ocioso.

**Usuários:** paciente, recepcionista e médico.

## 1. O mapa

```
BACKBONE   │ Cadastrar          │ Agendar consulta        │ Gerenciar agenda        │ Atender               │ Acompanhar
(atividade)│                    │                         │                         │                       │
───────────┼────────────────────┼─────────────────────────┼─────────────────────────┼───────────────────────┼──────────────────────
RELEASE 1  │ cadastrar médico   │ ★ H1 agendar em horário │ ★ H2 cancelar consulta  │                       │
walking    │ cadastrar paciente │   livre do médico       │   (antecedência 24h)    │                       │
skeleton   │ listar médicos     │                         │ ★ H3 ver agenda do dia  │                       │
(MVP)      │                    │                         │   do médico             │                       │
───────────┼────────────────────┼─────────────────────────┼─────────────────────────┼───────────────────────┼──────────────────────
RELEASE 2  │ login por perfil   │ sugerir próximo horário │ remarcar consulta       │ check-in na recepção  │ lembrete por e-mail
           │ (paciente/recep.)  │ livre                   │ bloquear agenda (férias)│ registrar falta       │ taxa de faltas
───────────┼────────────────────┼─────────────────────────┼─────────────────────────┼───────────────────────┼──────────────────────
RELEASE 3  │ convênios          │ fila de espera          │ limite de faltas →      │ prontuário resumido   │ painel da clínica
           │                    │                         │ bloqueio temporário     │                       │
```

★ = histórias da Release 1 quebradas em tarefas abaixo (o PDF pede pelo menos 3).

## 2. Histórias da Release 1 → tarefas → testes

Convenção: **R** = tarefa de regra de negócio (ganha teste unitário escrito antes do código);
**I** = tarefa de infraestrutura/integração (ganha teste de integração com PostgreSQL real).

### H1 · Agendar consulta em horário livre

> **Como** paciente, **quero** agendar uma consulta num horário livre do médico,
> **para** ser atendido sem conflito e sem precisar ligar para a clínica.

**Critérios de aceite (Gherkin)**

```gherkin
Funcionalidade: Agendar consulta

  Cenário: horário livre dentro do expediente
    Dado um médico e um paciente cadastrados
    Quando o paciente agenda terça-feira às 09:00
    Então a consulta é criada com status AGENDADA (HTTP 201)

  Cenário: horário já ocupado pelo médico
    Dado que o médico já tem consulta terça-feira às 10:00
    Quando outro paciente tenta agendar o mesmo horário
    Então o agendamento é recusado com MEDICO_OCUPADO (HTTP 409)

  Cenário: fora do expediente
    Quando o paciente tenta agendar num sábado
    Então o agendamento é recusado com FORA_DO_EXPEDIENTE (HTTP 422)
```

| Tarefa | Tipo | O que faz | Teste (escrito antes) |
|---|---|---|---|
| T1 | R | `Expediente`: seg–sex, 08:00–18:00, consultas de 30 min começando em :00/:30, sem almoço (12:00–13:00) | `ExpedienteTest` (unitário) |
| T2 | R | `JanelaDeAgendamento`: não agenda no passado nem além de 60 dias | `JanelaDeAgendamentoTest` (unitário) |
| T3 | R | `AgendamentoService.agendar`: médico e paciente sem outra consulta no mesmo horário | `AgendamentoServiceTest` (unitário, Mockito) |
| T4 | I | Tabelas + índice único parcial `(medico_id, inicio) WHERE status='AGENDADA'` | `ConsultaRepositoryIT` (integração) |
| T5 | I | `POST /api/consultas` + tradução das violações para HTTP (409/422/404) | `AgendarConsultaAceitacaoIT` (aceitação) |

### H2 · Cancelar consulta

> **Como** paciente, **quero** cancelar minha consulta com antecedência,
> **para** liberar o horário para outra pessoa.

```gherkin
Funcionalidade: Cancelar consulta

  Cenário: cancelamento com antecedência suficiente
    Dado uma consulta agendada para daqui a 3 dias
    Quando o paciente cancela
    Então a consulta fica CANCELADA (HTTP 200)
    E o horário volta a ficar disponível para outro paciente

  Cenário: cancelamento em cima da hora
    Dado uma consulta agendada para daqui a menos de 24 horas
    Quando o paciente tenta cancelar
    Então o cancelamento é recusado com CANCELAMENTO_TARDIO (HTTP 422)
```

| Tarefa | Tipo | O que faz | Teste (escrito antes) |
|---|---|---|---|
| T6 | R | `PoliticaDeCancelamento`: antecedência mínima de 24 h | `PoliticaDeCancelamentoTest` (unitário) |
| T7 | R | `AgendamentoService.cancelar`: muda status; não cancela duas vezes | `AgendamentoServiceTest` (unitário, Mockito) |
| T8 | I | Consulta cancelada libera o horário no banco (índice parcial) | `ConsultaRepositoryIT` (integração) |
| T9 | I | `PATCH /api/consultas/{id}/cancelamento` | `CancelarConsultaAceitacaoIT` (aceitação) |

### H3 · Ver a agenda do dia do médico

> **Como** recepcionista, **quero** ver a agenda do dia de um médico em ordem de horário,
> **para** organizar a recepção dos pacientes.

```gherkin
Funcionalidade: Agenda do dia

  Cenário: agenda ordenada e sem cancelamentos
    Dado que o médico tem consultas às 10:00 e às 08:30 e uma consulta cancelada às 09:00
    Quando a recepcionista consulta a agenda daquele dia
    Então vê 08:30 antes de 10:00
    E não vê a consulta cancelada
```

| Tarefa | Tipo | O que faz | Teste (escrito antes) |
|---|---|---|---|
| T10 | I | Consulta `agendaDoMedico` ordenada, só AGENDADAS, só daquele dia | `ConsultaRepositoryIT` (integração) |
| T11 | R | `AgendamentoService.agendaDoDia` delega com o intervalo [00:00, 00:00 do dia seguinte) | `AgendamentoServiceTest` (unitário) |
| T12 | I | `GET /api/medicos/{id}/agenda?data=AAAA-MM-DD` | `AgendaDoDiaAceitacaoIT` (aceitação) |

## 3. Por que essa ordem (risco)

| Prioridade | Regra | Risco se falhar |
|---|---|---|
| 1 | Conflito de horário (T3, T4) | Dois pacientes no mesmo horário: um espera horas ou volta para casa. É o principal problema que o sistema resolve. |
| 2 | Expediente (T1) | Consulta marcada no sábado ou no almoço: paciente chega e a clínica está fechada. |
| 3 | Cancelamento com 24 h (T6) | Horário ocioso sem tempo de recolocar outro paciente: perda de receita. |
| 4 | Janela de 60 dias (T2) | Agenda aberta indefinidamente, sem saber as férias do médico. Risco menor. |
| 5 | Agenda do dia (T10–T12) | Leitura apenas: o erro é visível e não corrompe dados. |
