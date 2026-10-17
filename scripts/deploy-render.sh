#!/usr/bin/env bash
# Dispara o deploy de uma imagem específica no Render pelo Deploy Hook.
#   uso: RENDER_DEPLOY_HOOK_URL=... ./scripts/deploy-render.sh ghcr.io/dono/repo:<sha>
#
# O Deploy Hook é uma URL secreta (quem tem a URL publica). Por isso ela fica no GitHub Secrets e
# nunca no código. Para serviços que usam imagem pronta, o parâmetro imgURL escolhe a tag; tudo
# antes da tag precisa ser igual à imagem configurada no serviço do Render.
# Documentação: https://render.com/docs/deploy-hooks
set -euo pipefail

IMAGEM="${1:?uso: deploy-render.sh <imagem:tag>}"

if [[ -z "${RENDER_DEPLOY_HOOK_URL:-}" ]]; then
  echo "::error::Secret RENDER_DEPLOY_HOOK_URL não configurado (Settings → Secrets and variables → Actions)."
  exit 1
fi

IMAGEM_CODIFICADA=$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$IMAGEM")

echo "Publicando $IMAGEM no Render…"
# --fail: hook inválido ou imagem recusada (HTTP 4xx/5xx) derruba o job.
# A URL não é impressa: o GitHub mascara o secret, mas não custa evitar.
curl --fail --silent --show-error --max-time 30 \
  "${RENDER_DEPLOY_HOOK_URL}&imgURL=${IMAGEM_CODIFICADA}" > /dev/null
echo "Deploy solicitado. O smoke test confirma quando a nova versão estiver no ar."
