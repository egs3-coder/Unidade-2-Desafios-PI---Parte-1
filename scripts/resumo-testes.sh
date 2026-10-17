#!/usr/bin/env bash
# Gera uma tabela Markdown com o resultado dos relatórios JUnit (surefire/failsafe) para o
# "Summary" da execução no GitHub Actions — evidência legível sem baixar artefatos.
#   uso: ./scripts/resumo-testes.sh <pasta-de-relatorios> <titulo>
set -uo pipefail
PASTA="${1:?pasta}"; TITULO="${2:-Testes}"

echo "### $TITULO"
if ! ls "$PASTA"/TEST-*.xml >/dev/null 2>&1; then
  echo "_Nenhum relatório encontrado (o build pode ter falhado antes dos testes)._"
  exit 0
fi

python3 - "$PASTA" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
linhas, tot = [], [0, 0, 0, 0]
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*.xml")):
    s = ET.parse(f).getroot()
    n, fa, er, sk = (int(s.get(k, 0)) for k in ("tests", "failures", "errors", "skipped"))
    tot = [a + b for a, b in zip(tot, (n, fa, er, sk))]
    icone = "✅" if fa + er == 0 else "❌"
    linhas.append(f"| {icone} | `{s.get('name').split('.')[-1]}` | {n} | {fa + er} | {float(s.get('time', 0)):.2f}s |")
print("| | Classe | Testes | Falhas | Tempo |\n|---|---|---|---|---|")
print("\n".join(linhas))
print(f"\n**Total: {tot[0]} testes, {tot[1] + tot[2]} falhas, {tot[3]} ignorados.**")
PY
