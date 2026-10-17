#!/usr/bin/env bash
# Gera a EVIDÊNCIA da execução vermelha exigida no E3, de forma controlada:
# cria uma branch que "afrouxa" a regra de cancelamento de 24h para 12h, sem mexer no teste.
# O teste "recusa com 23h59 de antecedência" falha → job 1 vermelho → jobs 2, 3 e 4 pulados
# → o PR fica bloqueado pela proteção da main → nada chega à produção.
#
#   ./scripts/demo-esteira-vermelha.sh
#   (abra o PR que o GitHub sugerir, tire os prints e FECHE o PR sem fazer merge)
set -euo pipefail

BRANCH="demo/esteira-vermelha"
ARQUIVO="src/main/java/br/com/agendasaude/dominio/PoliticaDeCancelamento.java"

git switch main
git pull --ff-only
git switch -c "$BRANCH"
sed -i.bak 's/Duration.ofHours(24)/Duration.ofHours(12)/' "$ARQUIVO" && rm -f "$ARQUIVO.bak"
git commit -am "demo: afrouxa cancelamento para 12h sem atualizar o teste (deve ficar VERMELHO)"
git push -u origin "$BRANCH"

echo
echo "Agora: abra o pull request de $BRANCH para main no GitHub e espere a esteira."
echo "Prints: (1) a execução vermelha com os jobs seguintes 'skipped'; (2) o PR com o merge bloqueado."
echo "Depois: feche o PR sem merge e apague a branch:  git switch main && git branch -D $BRANCH && git push origin --delete $BRANCH"
