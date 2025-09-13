#!/bin/sh
set -eu

echo "==> Configure mc alias"
mc alias set "${MINIO_ALIAS}" "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"

echo "==> Create bucket if missing"
mc mb --ignore-existing "${MINIO_ALIAS}/${BUCKET}"

echo "==> Set anonymous read ONLY on prefix 'cover/'"
# Donne un accès public en lecture aux objets sous voyages/cover/*
mc anonymous set download "${MINIO_ALIAS}/${BUCKET}/cover" || true

echo "==> Apply CORS for browser GET/PUT/HEAD"
cat >/tmp/cors.json <<'JSON'
[
  {
    "AllowedOrigins": ["https://voyages.siovision.fr", "https://api.voyages.siovision.fr", "http://localhost:5173"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag","x-amz-request-id"],
    "MaxAgeSeconds": 3000
  }
]
JSON
mc cors set "${MINIO_ALIAS}/${BUCKET}" /tmp/cors.json

echo "==> (Optional) Lifecycle for tmp/ prefix"
cat >/tmp/lifecycle.json <<'JSON'
{
  "Rules": [
    {
      "ID": "tmp-expire",
      "Status": "Enabled",
      "Filter": { "Prefix": "tmp/" },
      "Expiration": { "Days": 7 }
    }
  ]
}
JSON
mc ilm import "${MINIO_ALIAS}/${BUCKET}" /tmp/lifecycle.json || true

echo "==> Done."
