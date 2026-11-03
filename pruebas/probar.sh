#!/usr/bin/env bash
# Prueba de punta a punta contra el gateway. Uso: ./pruebas/probar.sh [URL_BASE]
# Requiere el proyecto levantado (docker compose up -d --build) y curl.
BASE="${1:-http://localhost:8080}"
OK=0; FALLOS=0

# Espera hasta 120 s a que los servicios terminen de arrancar
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"nombre":"admin","password":"admin12345"}')" = "200" ] \
    && [ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/productos")" = "200" ] \
    && [ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/docs/pedidos")" = "200" ] && break
  sleep 2
done

verificar() { # descripcion codigo_esperado curl_args...
  local desc="$1" esperado="$2"; shift 2
  local codigo; codigo=$(curl -s --path-as-is -o /dev/null -w '%{http_code}' "$@")
  if [ "$codigo" = "$esperado" ]; then echo "OK    [$codigo] $desc"; OK=$((OK+1));
  else echo "FALLA [$codigo, esperado $esperado] $desc"; FALLOS=$((FALLOS+1)); fi
}
login() { curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"nombre\":\"$1\",\"password\":\"$2\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/'; }
JSON='Content-Type: application/json'

ADMIN=$(login admin admin12345)
CLIENTE=$(login cliente cliente12345)

verificar "Catalogo publico sin token"              200 "$BASE/api/productos"
verificar "Login con clave incorrecta"              401 -X POST "$BASE/api/auth/login" -H "$JSON" -d '{"nombre":"admin","password":"incorrecta1"}'
verificar "Ver pedidos sin token"                   401 "$BASE/api/pedidos"
verificar "Ver pedidos con token falso"             401 "$BASE/api/pedidos" -H "Authorization: Bearer abc.def.ghi"
verificar "Cliente intenta crear producto"          403 -X POST "$BASE/api/productos" -H "Authorization: Bearer $CLIENTE" -H "$JSON" -d '{"nombre":"X","precio":1,"stock":1}'
verificar "Admin crea producto"                     201 -X POST "$BASE/api/productos" -H "Authorization: Bearer $ADMIN" -H "$JSON" -d '{"nombre":"Monitor","precio":150000,"stock":5}'
verificar "Admin envia producto invalido"           400 -X POST "$BASE/api/productos" -H "Authorization: Bearer $ADMIN" -H "$JSON" -d '{"nombre":"","precio":-1,"stock":1}'
verificar "Cliente crea pedido"                     201 -X POST "$BASE/api/pedidos" -H "Authorization: Bearer $CLIENTE" -H "$JSON" -d '{"productoId":1,"cantidad":2}'
verificar "Pedido sobre producto inexistente"       400 -X POST "$BASE/api/pedidos" -H "Authorization: Bearer $CLIENTE" -H "$JSON" -d '{"productoId":999,"cantidad":1}'
verificar "Pedido sin stock suficiente"             409 -X POST "$BASE/api/pedidos" -H "Authorization: Bearer $CLIENTE" -H "$JSON" -d '{"productoId":2,"cantidad":50}'
verificar "Ruta con .. para burlar reglas"          400 "$BASE/api/productos/../pedidos"
verificar "Ruta con .. hacia actuator interno"      400 "$BASE/api/productos/../../actuator"
verificar "Docs no da acceso a otros endpoints"     401 -X POST "$BASE/docs/productos/api/productos" -H "$JSON" -d '{}'
verificar "Preflight CORS desde React"              200 -X OPTIONS "$BASE/api/pedidos" -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST'
verificar "Preflight CORS desde origen no permitido" 403 -X OPTIONS "$BASE/api/pedidos" -H 'Origin: http://otro-sitio.example' -H 'Access-Control-Request-Method: POST'
verificar "Swagger unificado"                       200 -L "$BASE/swagger-ui.html"
verificar "Documentacion OpenAPI de pedidos"        200 "$BASE/docs/pedidos"

# Suplantacion: un cliente envia X-User-Name: admin. Debe ver solo sus pedidos.
USUARIOS=$(curl -s "$BASE/api/pedidos" -H "Authorization: Bearer $CLIENTE" -H 'X-User-Name: admin' -H 'X-User-Roles: ADMIN')
if echo "$USUARIOS" | grep -q '"usuario":"cliente"' && ! echo "$USUARIOS" | grep -q '"usuario":"admin"'; then
  echo "OK    Headers X-User falsificados se ignoran"; OK=$((OK+1))
else echo "FALLA Headers X-User falsificados: $USUARIOS"; FALLOS=$((FALLOS+1)); fi

echo; echo "Resultado: $OK correctas, $FALLOS fallidas"
[ "$FALLOS" -eq 0 ]
