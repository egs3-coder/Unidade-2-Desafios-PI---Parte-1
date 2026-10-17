# Evidências

> O PDF avisa: **item sem evidência não pontua**. Esta pasta reúne os prints, salvos com os nomes
> abaixo e referenciados no README principal. Marquem cada item quando o print estiver aqui.

Os registros de texto do ciclo TDD (saídas reais dos testes em cada passo red → green) estão em
[`../tdd-log/`](../tdd-log/).

## E1 · Container automatizado

| ✓ | Arquivo | Como gerar |
|---|---|---|
| ☐ | `e1-01-compose-up.png` | `cp .env.example .env && docker compose up -d --build` (o terminal mostra a imagem sendo construída, com os testes unitários rodando no 1º estágio) |
| ☐ | `e1-02-compose-ps.png` | `docker compose ps`: `api` e `banco` com STATUS **(healthy)** |
| ☐ | `e1-03-api-respondendo.png` | `curl -s localhost:8080/api/medicos` (ou o navegador em `http://localhost:8080/`) |
| ☐ | `e1-04-volume-persiste.png` | Agendar uma consulta → `docker compose down` → `docker compose up -d` → `curl localhost:8080/api/consultas/1`: a consulta continua lá |
| ☐ | `e1-05-tamanho-imagem.png` | `docker images agendasaude-api`: imagem final pequena (só JRE + jar) |

## E2 · TDD

| ✓ | Arquivo | Como gerar |
|---|---|---|
| ☐ | `e2-01-historico-red-green.png` | `git log --oneline --reverse` mostrando os pares `test: … (RED)` → `feat: … (GREEN)` |
| ☐ | `e2-02-testes-locais.png` | `mvn verify` terminando com `BUILD SUCCESS` e a contagem de testes |
| ☐ | `e2-03-resumo-na-esteira.png` | Aba **Actions** → uma execução → **Summary**: tabelas "Testes unitários" e "Integração e aceitação" |
| ☐ | `e2-04-story-map.png` | Print do [story map](../story-map.md) ou do board (GitHub Projects/Trello), se usarem |

## E3 · Deploy automatizado

| ✓ | Arquivo | Como gerar |
|---|---|---|
| ☐ | `e3-01-execucao-vermelha.png` | `./scripts/demo-esteira-vermelha.sh` → abrir o PR → print da execução com o job 1 **vermelho** e os jobs 2, 3 e 4 **skipped** |
| ☐ | `e3-02-pr-bloqueado.png` | O PR acima com a mensagem de merge bloqueado (checks obrigatórios falhando). Depois, fechar sem merge. |
| ☐ | `e3-03-execucao-verde.png` | Um merge real na `main`: os 4 jobs verdes, com o ambiente `producao` e a URL |
| ☐ | `e3-04-smoke-test.png` | Log do job 4 com "✔ versão no ar: <sha>", "✔ /actuator/health: UP" |
| ☐ | `e3-05-render-deploys.png` | Painel do Render → serviço → **Events**: deploys disparados pela esteira |
| ☐ | `e3-06-url-no-ar.png` | Navegador em `https://<serviço>.onrender.com/` e `/api/versao` |
| ☐ | `e3-07-rollback.png` | **Run workflow** com um SHA anterior → job 5 verde → `/api/versao` mostrando o SHA antigo |
| ☐ | `e3-08-protecao-main.png` | Settings → Rules → ruleset `proteger-main` com os checks obrigatórios |
| ☐ | `e3-09-segredos.png` | Settings → Secrets and variables → Actions, mostrando **o nome** `RENDER_DEPLOY_HOOK_URL` (o valor nunca aparece) |

## Links das execuções (preencher)

- Execução vermelha que barrou o deploy: `https://github.com/SEU-USUARIO/agendasaude/actions/runs/…`
- Execução verde que publicou: `https://github.com/SEU-USUARIO/agendasaude/actions/runs/…`
- Execução de rollback: `https://github.com/SEU-USUARIO/agendasaude/actions/runs/…`
