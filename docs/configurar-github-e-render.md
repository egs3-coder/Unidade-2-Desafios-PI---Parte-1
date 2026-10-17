# Passo a passo: GitHub + Render (uma vez por grupo)

Tempo estimado: 30–40 minutos. Faça na ordem: cada passo depende do anterior.

## 1. Publicar o repositório no GitHub

1. No GitHub: **New repository** → nome `agendasaude` (ou o do PI) → **Public** → *sem* README (já existe).
2. No terminal, dentro da pasta do projeto:

   ```bash
   git remote add origin https://github.com/SEU-USUARIO/agendasaude.git
   git push -u origin main
   ```

3. Aba **Actions**: a esteira roda. Os jobs 1, 2 e 3 devem ficar verdes. O job 4 (deploy) aparece
   como *skipped*, porque o Render ainda não foi configurado (a variável `URL_PRODUCAO` não existe).
   Nesse primeiro push na `main`, o job 3 **já publica a imagem** no GitHub Container Registry.

> Se o job 1 ou 2 ficar vermelho logo no primeiro push, abram o log, corrijam e registrem o que
> aconteceu em `docs/jornada-tdd.md`. Isso também é evidência.

## 2. Deixar a imagem acessível ao Render

1. No perfil (ou organização) do GitHub → **Packages** → `agendasaude`.
2. **Package settings** → **Change visibility** → **Public**.
   (Alternativa: manter privado e cadastrar no Render uma *Registry Credential* com um token
   `read:packages`. Público é mais simples para o PI.)

## 3. Criar o banco no Render

1. [dashboard.render.com](https://dashboard.render.com) → **New** → **Postgres**.
2. Name `agendasaude-db` · Database `agendasaude` · User `agendasaude` · Region **Virginia** ·
   PostgreSQL **17** · Plan **Free** → **Create Database**.
3. Na página do banco, em **Connections**, anote o **Hostname** interno (algo como `dpg-xxxx-a`),
   o **Database**, o **Username** e o **Password**.

> ⚠ O Postgres gratuito do Render **expira 30 dias após a criação** (com 14 dias de carência).
> Criem o banco perto da data de apresentação ou, se precisarem de mais tempo, usem outro provedor
> gratuito de PostgreSQL (ex.: Neon) e troquem só as variáveis `DB_*`.

## 4. Criar o serviço web no Render

1. **New** → **Web Service** → **Existing Image**.
2. Image URL: `ghcr.io/SEU-USUARIO/agendasaude:latest` (tudo minúsculo).
3. Name `agendasaude` · Region **Virginia** (a mesma do banco) · Instance type **Free**.
4. **Environment Variables**:

   | Variável | Valor |
   |---|---|
   | `DB_HOST` | Hostname interno do passo 3 |
   | `DB_PORT` | `5432` |
   | `DB_NAME` | `agendasaude` |
   | `DB_USER` | `agendasaude` |
   | `DB_PASSWORD` | senha do passo 3 (fica só no Render: é o "cofre" do provedor) |

5. **Advanced** → **Health Check Path**: `/actuator/health`.
6. **Deploy Web Service**. Na primeira subida o Flyway cria as tabelas. Abra
   `https://agendasaude-xxxx.onrender.com/api/medicos`: deve listar 3 médicos.
7. Ainda no serviço: **Settings** → **Deploy Hook** → copie a URL. **Ela é um segredo**: quem tem a
   URL consegue publicar.

> Alternativa: em vez dos passos 3 e 4, **New → Blueprint** usando o `render.yaml` da raiz
> (troque o nome da imagem antes).

## 5. Ligar a esteira ao Render (segredos no cofre do GitHub)

No repositório: **Settings** → **Secrets and variables** → **Actions**.

| Onde | Nome | Valor | Por quê |
|---|---|---|---|
| aba **Secrets** | `RENDER_DEPLOY_HOOK_URL` | URL do Deploy Hook | É segredo: o GitHub criptografa e mascara nos logs. |
| aba **Variables** | `URL_PRODUCAO` | `https://agendasaude-xxxx.onrender.com` | Não é segredo; aparece no ambiente e no smoke test. |

Depois disso o job 4 deixa de ser pulado.

## 6. Proteger a `main` (GitHub Flow de verdade)

**Settings** → **Rules** → **Rulesets** → **New ruleset** → **New branch ruleset**:

- Ruleset name: `proteger-main` · Enforcement status: **Active**
- Target branches: **Add target** → **Include default branch**
- Marque:
  - ✅ **Restrict deletions**
  - ✅ **Require a pull request before merging** → *Required approvals*: **1**
  - ✅ **Require status checks to pass** → *Add checks*:
    `1 · Build e testes unitários`, `2 · Testes de integração e aceitação (PostgreSQL real)`, `3 · Imagem Docker`
  - ✅ **Block force pushes**
- **Create**.

> Os checks só aparecem na busca depois de terem rodado ao menos uma vez (passo 1).
> Em repositório privado de conta gratuita, *rulesets* podem não estar disponíveis: por isso o
> passo 1 cria o repositório como **público**.

## 7. Testar o fluxo completo

```bash
git switch -c melhoria/teste-do-fluxo
# altere algo pequeno (ex.: o texto da página inicial em InfoController)
git commit -am "chore: testa o fluxo de deploy"
git push -u origin melhoria/teste-do-fluxo
```

Abra o PR → esteira verde → um colega aprova → **Merge** → na `main` a esteira roda os 4 jobs →
o job 4 termina com "✔ versão no ar: <sha>". Pronto: **cada merge na main chega sozinho à produção**.

## 8. Rollback (voltar à versão anterior)

Três caminhos, do mais simples ao mais "certo":

1. **Pela esteira (recomendado):** **Actions** → **CI/CD** → **Run workflow** → cole o SHA completo do
   último commit bom (`git log --format=%H -n 5 main`) → **Run**. O job 5 republica a imagem
   `ghcr.io/…:<sha>`, que já existe no registry (não recompila), e roda o smoke test.
2. **Pelo painel do Render:** serviço → **Events/Deploys** → deploy anterior → **Rollback**.
   Atenção: o Render desliga o autodeploy do serviço após um rollback pelo painel.
3. **Pelo Git:** `git revert <sha-ruim>` numa branch → PR → merge. A correção vira histórico e
   passa pelos mesmos gates.

O rollback 1 ou 2 apaga o incêndio em minutos; o 3 é a correção definitiva. O próximo merge na
`main` publica de novo a versão mais recente.
