# Roteiro da apresentação (≈ 12 minutos)

> Todos os integrantes falam. Ajustem os nomes (Pessoa A–D) à divisão do grupo. Ensaiem uma vez com
> cronômetro. Abram a URL de produção **5 minutos antes**: o plano gratuito do Render "dorme" após
> 15 minutos sem acesso e leva cerca de 1 minuto para acordar.

## Antes de começar (checklist)

- [ ] `docker compose up -d --build` já rodou uma vez na máquina da demo (as imagens ficam em cache).
- [ ] Abas abertas: repositório no GitHub · aba Actions · PR vermelho (fechado) · painel do Render · URL de produção.
- [ ] Terminal na pasta do projeto, fonte grande.
- [ ] **Plano B:** os prints de `docs/evidencias/` abertos, caso a internet ou o Wi-Fi falhe.

## 1 · O problema e o story map — Pessoa A (1,5 min)

- Clínicas pequenas agendam por telefone e planilha: duas pessoas no mesmo horário, consulta marcada no
  sábado, cancelamento em cima da hora.
- Mostrar o [story map](story-map.md): backbone (Cadastrar → Agendar → Gerenciar → Atender → Acompanhar)
  e a Release 1 com as 3 histórias (H1 agendar, H2 cancelar, H3 agenda do dia).
- **Frase-chave:** "Cada história virou tarefas, e cada tarefa de regra virou um teste antes de virar código."

## 2 · E1 · Container — Pessoa B (2 min)

Ao vivo:

```bash
cp .env.example .env
docker compose up -d --build     # mostrar o 1º estágio rodando os testes
docker compose ps                # api e banco: (healthy)
curl -s localhost:8080/api/medicos
```

Pontos a falar (todos estão na tabela de Decisões do README):

- **Multi-stage:** o 1º estágio tem Maven e JDK e roda os testes; o 2º só tem a JRE e o jar.
  Teste vermelho = não existe imagem.
- **Volume nomeado:** o banco sobrevive ao `docker compose down`.
- **Rede do Compose:** a API acha o banco pelo nome `banco`; o banco nem expõe porta para o PC.
- **Configuração fora do código:** `.env` no `.gitignore`; o compose recusa subir sem senha.

## 3 · E2 · TDD: a jornada — Pessoa C (3,5 min)

- Mostrar `git log --oneline --reverse`: os pares `test: … (RED)` → `feat: … (GREEN)`.
- **Contar a história do bug da meia-noite** (é a melhor parte):
  1. Escrevemos 10 testes do expediente → vermelho.
  2. Implementamos → verde.
  3. Revisando as bordas: "e 23:30?" → novo teste → **vermelho**: o código aceitava consulta à meia-noite.
  4. Corrigimos e simplificamos o código, com 11 testes nos protegendo.
- Pirâmide: 30 unitários (menos de 1 s) · 5 de integração com PostgreSQL real · 9 de aceitação por HTTP.
- Duas decisões de design que o TDD forçou: o **relógio injetável** e as **regras puras separadas** do
  Spring.
- Por que o índice único no banco: mock não pega condição de corrida; o teste de integração pega.

## 4 · E3 · Esteira e deploy — Pessoa D (3,5 min)

- Mostrar o `ci-cd.yml` (só os nomes dos jobs): **1 unitários → 2 integração/aceitação → 3 imagem → 4 deploy + smoke**.
  Cada job tem `needs` do anterior: esse é o gate.
- **Execução vermelha:** o PR `demo/esteira-vermelha` (regra afrouxada para 12 h) → job 1 vermelho,
  jobs 2–4 *skipped*, merge **bloqueado** pela proteção da `main`.
- **Execução verde:** um merge real → 4 jobs verdes → "✔ versão no ar: <sha>".
- Abrir a URL de produção → `/api/versao` mostra o mesmo SHA do commit. "O que passou nos testes é exatamente
  o que está no ar."
- **Rollback:** Actions → Run workflow → SHA anterior → job 5 republica a imagem antiga, sem recompilar.
- Segredos: Deploy Hook no GitHub Secrets; senha do banco no Render; nada no repositório.

## 5 · Decisões, IA e fechamento — Pessoa A (1,5 min)

- Duas ou três linhas da tabela de Decisões: GitHub Flow, Render por imagem, Spring Boot 4.
- **Uso de IA:** qual ferramenta, para quê, e o que o grupo **revisou e mudou** (seção "Uso de IA" do README).
- Fechar com a pergunta central do PDF: *"Um colega que clonar o repositório amanhã consegue rodar,
  testar e publicar sem pedir ajuda?"* → "Sim: README, `.env.example`, `docker compose up` e a esteira."

## Perguntas prováveis (e respostas curtas)

| Pergunta | Resposta |
|---|---|
| Por que não H2 nos testes? | O índice único **parcial** e o CHECK são do PostgreSQL. O teste precisa do mesmo banco da produção, e o Testcontainers sobe exatamente a imagem do compose. |
| Por que o `.env` não vai para o Git? | Tem senha. O repositório leva só o `.env.example`, com nomes e valores de exemplo. |
| E se dois pacientes clicarem no mesmo segundo? | As duas checagens podem passar, mas o índice único parcial do banco aceita só um. O outro recebe 409 `CONFLITO_DE_DADOS`. |
| Por que GitHub Flow e não Git Flow? | Grupo pequeno, uma versão no ar, deploy a cada merge. Git Flow serve para versões planejadas e várias versões em uso, que não é o nosso caso. |
| O que acontece se o deploy quebrar em produção? | O smoke test fica vermelho e avisa. A versão anterior continua no ar até a nova responder. Para voltar: job de rollback com o SHA anterior, ou Rollback no painel do Render. |
| O que ficou sem teste? | Os cadastros simples (CRUD sem regra, cobertos só indiretamente pela aceitação), os scripts de deploy (testados manualmente) e carga e concorrência real (só o índice do banco é testado). Está no README. |
| A IA fez tudo? | A IA rascunhou. Nós rodamos `mvn verify`, revisamos cada teste ("ele falharia se a regra quebrasse?"), ajustamos e assinamos. Os prompts estão no README. |
| Por que Spring Boot 4 e não 3? | A linha 3.5 saiu do suporte open source em 30/06/2026; começar um projeto novo numa versão sem correções de segurança não faz sentido. |
