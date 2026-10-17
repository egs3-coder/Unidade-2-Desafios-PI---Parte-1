# A jornada TDD do AgendaSaúde

> O professor pediu: *"Se usarem TDD, expliquem como foi a jornada de desenvolvimento e as lições
> aprendidas."* Este é o roteiro, ciclo a ciclo. Cada passo tem um commit e, para as regras de domínio,
> a saída real dos testes em [`tdd-log/`](tdd-log/).

## O método que seguimos

1. **Story map → histórias → tarefas** ([story-map.md](story-map.md)). Cada tarefa de regra ganhou uma
   **lista de testes** (BECK, 2003), do caso mais simples às bordas.
2. **Red:** o teste chama uma classe ou método que ainda não existe. A IDE gera o esqueleto
   (*Create class / Create method*) e o corpo lança `UnsupportedOperationException`. Assim o teste
   compila e falha **pelo motivo certo**: a funcionalidade não existe, e não um erro de digitação.
3. **Green:** o mínimo de código para passar.
4. **Refactor:** com tudo verde, melhoramos nomes e estrutura, sob a proteção dos testes.
5. Commits separados: `test: … (RED)` e depois `feat: … (GREEN)`. O histórico prova a ordem.

## Ciclo a ciclo

| # | Commit | O que aconteceu | Resultado |
|---|---|---|---|
| 1 | `test: T1 expediente … (RED)` | 10 testes: 08:00 e 17:30 aceitos; 07:30, 18:00, almoço, sábado, domingo, 09:15 e 09:00:30 recusados. | 10 de 10 falhando ([log](tdd-log/01-T1-expediente-RED.txt)) |
| 2 | `feat: T1 implementa Expediente (GREEN)` | Implementação "natural": calcula o fim da consulta (`inicio + 30 min`) e compara com 18:00. | 10 de 10 passando ([log](tdd-log/02-T1-expediente-GREEN.txt)) |
| 3 | `test: T1 consulta às 23:30 … (RED)` | **Revisando as bordas, surgiu a pergunta: e 23:30?** 23:30 + 30 min = 00:00, e 00:00 *não* é depois de 18:00. O código aceitava uma consulta à meia-noite. | 1 falha: o bug estava lá ([log](tdd-log/03-T1-meia-noite-RED.txt)) |
| 4 | `fix: T1 … (GREEN + REFACTOR)` | Passamos a comparar só o **início** com limites fixos (08:00–17:30) e extraímos `estaNaGrade` e `estaNoHorarioDeAtendimento`. | 11 de 11 passando ([log](tdd-log/04-T1-meia-noite-GREEN-refactor.txt)) |
| 5–6 | `T2 janela de agendamento` | Bordas decididas pelo teste: "exatamente agora" é passado; "exatamente 60 dias" ainda vale. | 5 falhas → 0 |
| 7–8 | `T6 política de cancelamento` | Borda inclusiva: com **exatamente** 24 h ainda pode cancelar; com 23h59, não. | 4 falhas → 0 |
| 9–10 | `T3/T7/T11 AgendamentoService` | Primeiro teste com dublês (Mockito) no lugar do banco. Um teste exigiu `verifyNoInteractions` no sábado, o que nos levou a validar as regras baratas **antes** de consultar o banco. | 10 falhas → 0 ([RED](tdd-log/09-T3-T7-T11-servico-RED.txt), [GREEN](tdd-log/10-T3-T7-T11-servico-GREEN.txt)) |
| 11–12 | `T4/T8/T10 integração` | O teste com PostgreSQL real (Testcontainers) não subia: faltava o schema. Escrevemos as migrações Flyway com **índice único parcial**. | Contexto não sobe → verde |
| 13–14 | `T5/T9/T12 aceitação` | Os cenários Gherkin viraram testes HTTP (Dado/Quando/Então). Sem controllers, tudo dava 404; depois, 201/409/422/400. | 404 → verde |

**Placar final:** 30 testes unitários (rodam em menos de 1 s, sem Docker) + 5 de integração +
9 de aceitação = **44 testes**. Proporção próxima da pirâmide de Cohn: base larga de unitários e
poucos testes caros no topo.

## Lições aprendidas

1. **O teste de borda achou um bug real no nosso próprio GREEN.** A consulta às 23:30 passava porque
   `LocalTime` "dá a volta" na meia-noite. Sem a lista de testes pensando nas bordas, isso iria para
   produção. A correção ainda deixou o código mais simples.
2. **O "agora" precisa ser injetável.** Regras como "24 h de antecedência" são impossíveis de testar se o
   código chama `LocalDateTime.now()` direto: o teste passaria de manhã e falharia à noite. Recebemos um
   `Clock` no construtor; nos testes de aceitação ele fica congelado em segunda, 19/10/2026, 09:00.
   Foi o TDD que forçou essa decisão de design.
3. **Separar regra pura de orquestração deixa os testes rápidos.** `Expediente`, `JanelaDeAgendamento`
   e `PoliticaDeCancelamento` não conhecem Spring nem banco: são testadas em milissegundos. O serviço só
   orquestra, e o controller só traduz HTTP.
4. **Mock não pega condição de corrida; o banco pega.** Duas requisições simultâneas podem passar pela
   checagem "horário livre?" ao mesmo tempo. O teste unitário não enxerga isso. O teste de integração
   prova que o índice único parcial do PostgreSQL barra o segundo agendamento, e o *parcial*
   (`WHERE status = 'AGENDADA'`) prova que cancelar libera o horário. Um H2 "parecido" não garantiria o
   mesmo comportamento, e por isso usamos Testcontainers com a mesma imagem do compose.
5. **"Vermelho pelo motivo certo" importa.** Um teste que falha por erro de compilação não prova nada sobre
   a regra. O esqueleto com `UnsupportedOperationException` garante que o vermelho é "ainda não implementei".
6. **Testar o teste.** Para gerar a evidência vermelha, afrouxamos a regra de 24 h para 12 h sem mexer
   nos testes ([`scripts/demo-esteira-vermelha.sh`](../scripts/demo-esteira-vermelha.sh)). Dois testes
   quebraram na hora: sinal de que eles realmente protegem a regra. Um teste que continuasse verde seria
   um teste fraco, o risco que o professor apontou para código gerado por IA.
7. **Ferramenta também se pesquisa.** Planejávamos Cucumber para os cenários BDD, mas a versão estável não
   suporta oficialmente o JUnit 6 usado pelo Spring Boot 4. Mantivemos o Gherkin como especificação e
   escrevemos a aceitação em JUnit no formato Dado/Quando/Então. O PDF permite: o Gherkin "pode ser",
   não "deve ser".
8. **TDD custa no começo e paga depois.** Escrever o teste antes leva mais tempo por tarefa (a literatura
   fala em 15–35% a mais; NAGAPPAN et al., 2008), mas a refatoração do ciclo 4 levou minutos e sem medo,
   porque 11 testes vigiavam o comportamento.

## O que faríamos diferente

- Começar pela regra de **maior risco** (conflito de horário) em vez do expediente. Ordenamos pela
  sequência do story map, não pelo risco.
- Medir cobertura (JaCoCo) desde o primeiro dia e publicá-la na esteira.
- Rodar os testes de aceitação também contra a URL de produção (hoje o smoke test só faz leituras).
