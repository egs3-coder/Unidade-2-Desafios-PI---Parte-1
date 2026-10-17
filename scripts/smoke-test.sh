#!/usr/bin/env bash
# Smoke test pós-deploy: o sistema subiu de verdade, e na versão certa?
#   uso: ./scripts/smoke-test.sh <url-base> [versao-esperada]
#   ex.: ./scripts/smoke-test.sh https://agendasaude.onrender.com 3f2a…
#        ./scripts/smoke-test.sh http://localhost:8080
#
# 1. Espera /api/versao responder a versão esperada (o Render troca a versão aos poucos e o plano
#    gratuito "acorda" em ~1 min, por isso as tentativas).
# 2. Confere /actuator/health = UP.
# 3. Faz uma leitura real na API (lista de médicos, que depende do banco).
set -euo pipefail

URL="${1:?uso: smoke-test.sh <url-base> [versao-esperada]}"
URL="${URL%/}"
ESPERADA="${2:-}"
TENTATIVAS="${TENTATIVAS:-40}"
INTERVALO="${INTERVALO:-15}"

for ((i = 1; i <= TENTATIVAS; i++)); do
  VERSAO=$(curl -fsS --max-time 10 "$URL/api/versao" 2>/dev/null | sed -n 's/.*"versao":"\([^"]*\)".*/\1/p' || true)
  if [[ -n "$VERSAO" && ( -z "$ESPERADA" || "$VERSAO" == "$ESPERADA" ) ]]; then
    echo "✔ versão no ar: $VERSAO"
    break
  fi
  if (( i == TENTATIVAS )); then
    echo "✘ a versão ${ESPERADA:-esperada} não ficou no ar a tempo (última vista: ${VERSAO:-sem resposta})."
    echo "  Veja os logs do serviço no painel do Render. A versão anterior continua no ar."
    exit 1
  fi
  echo "… tentativa $i/$TENTATIVAS: no ar='${VERSAO:-sem resposta}', esperando='${ESPERADA:-qualquer}'"
  sleep "$INTERVALO"
done

curl -fsS --max-time 10 "$URL/actuator/health" | grep -q '"status":"UP"' \
  && echo "✔ /actuator/health: UP"

MEDICOS=$(curl -fsS --max-time 10 "$URL/api/medicos")
[[ "$MEDICOS" == \[* ]] && echo "✔ /api/medicos respondeu (banco acessível)"

echo "Smoke test OK em $URL"
