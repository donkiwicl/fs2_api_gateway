# Ejemplo de API Gateway con microservicios (DSY1104)

Tienda de ejemplo con Spring Boot 4.1.2 y Spring Cloud Gateway (Server MVC).
Refuerzo de DSY1103 para DSY1104. Todo el tráfico entra por `http://localhost:8080`.

La explicación completa, paso a paso, está en [GUIA.md](GUIA.md).

| Servicio | Puerto | Publicado con Docker | Responsabilidad |
| --- | --- | --- | --- |
| gateway | 8080 | Sí | Rutas, JWT, roles, CORS, fallback, Swagger único |
| auth-service | 8081 | No | Login, registro y firma de tokens |
| productos-service | 8082 | No | CRUD de productos |
| pedidos-service | 8083 | No | Pedidos; consulta precio y stock a productos |

## Inicio rápido

Requiere Docker con Docker Compose y curl.

```bash
cp .env.example .env
docker compose up -d --build
./pruebas/probar.sh
```

El script espera a que los servicios arranquen y termina con `Resultado: 18 correctas, 0 fallidas`.

- Swagger: http://localhost:8080/swagger-ui.html
- Postman: importa `pruebas/tienda.postman_collection.json`
- Detener: `docker compose down`

## Sin Docker

Requiere Java 17 o superior. Abre una terminal por servicio:

```bash
cd auth-service && ./mvnw spring-boot:run
cd productos-service && ./mvnw spring-boot:run
cd pedidos-service && ./mvnw spring-boot:run
cd gateway && ./mvnw spring-boot:run
```

En este modo los puertos 8081 a 8083 quedan accesibles sin pasar por el gateway. Con Docker no.

## Pruebas automáticas

```bash
cd gateway && ./mvnw test
```

## Usuarios de demostración

| Usuario | Clave | Rol |
| --- | --- | --- |
| admin | admin12345 | ADMIN |
| cliente | cliente12345 | CLIENTE |

Son solo para laboratorio. Antes de desplegar, cambia `JWT_SECRET` en `.env` y elimina estas cuentas.
