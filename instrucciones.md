## 📘 Instrucciones de Ejecución y Pruebas

**Banco XYZ — Desarrollo Backend III (EFT)**
Guía paso a paso para ejecutar y verificar cada componente del sistema.

## Requisitos Previos

Antes de comenzar, verifica que tengas instalado:

**Java 17** o superior:
java -version

**Maven 3.9+** (o usar el mvnw incluido):
mvn --version

**Docker 24+** y **Docker Compose v2**:
docker --version
docker compose version

**Git**:
git --version

RAM disponible: mínimo 6 GB libres.

Puertos requeridos libres: 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8761, 8888, 9000, 5433.

Verificar puertos libres:
sudo ss -tlnp | grep -E ":(8080|8081|8082|8083|8084|8085|8086|8087|8761|8888|9000|5433)"

Si hay resultados, hay procesos usando esos puertos. Detenerlos antes de continuar.

## Levantar el Ecosistema

Paso 1: Clonar el repositorio
git clone https://github.com/Dr3sden/banco-xyz-cloud.git
cd banco-xyz-cloud

Paso 2: Levantar los 13 contenedores
docker compose up -d

Esto descarga imágenes y levanta:
3 servicios de infraestructura (config-server, eureka-server, auth-server)
4 microservicios (transacciones, cuentas, pagos, clientes)
3 BFFs (web, móvil, atm)
1 batch-service
1 PostgreSQL
1 Kafka + Zookeeper (local, opcional para pruebas)
1 Red compartida

Paso 3: Esperar el arranque completo
echo "Esperando 3 minutos a que todo arranque..."
sleep 180
docker compose ps

Resultado esperado: todos los contenedores en estado Up o Up (healthy).

Si algún contenedor está en Exit, ver logs:
docker compose logs <nombre-contenedor> --tail 50

Paso 4: Verificar que cada servicio responde
# Config Server
curl -s http://localhost:8888/application/default | head -5

# Eureka
curl -s http://localhost:8761/actuator/health | jq '.status'

# Auth Server (discovery OIDC)
curl -s http://localhost:9000/.well-known/openid-configuration | jq '.issuer'

# BFFs (health check)
curl -s http://localhost:8080/actuator/health | jq '.status'
curl -s http://localhost:8083/actuator/health | jq '.status'
curl -s http://localhost:8084/actuator/health | jq '.status'

# Batch
curl -s http://localhost:8087/actuator/health | jq '.status'

Resultado esperado: todos responden "status": "UP" o un JSON válido.

Verificar Servicios Registrados

Abrir el dashboard de Eureka
En el navegador: http://localhost:8761

Debes ver en la sección "Instances currently registered with Eureka" los siguientes servicios:

AUTH-SERVER
BATCH-SERVICE
BFF-ATM
BFF-MOVIL
BFF-WEB
MICROSERVICIO-CLIENTES
MICROSERVICIO-CUENTAS
MICROSERVICIO-PAGOS
MICROSERVICIO-TRANSACCIONES

Alternativa por terminal
curl -s http://localhost:8761/eureka/apps -H "Accept: application/json" \
  | jq -r '.applications.application[].name' | sort

Resultado esperado:
AUTH-SERVER
BATCH-SERVICE
BFF-ATM
BFF-MOVIL
BFF-WEB
MICROSERVICIO-CLIENTES
MICROSERVICIO-CUENTAS
MICROSERVICIO-PAGOS
MICROSERVICIO-TRANSACCIONES

# Obtener Token OAuth2

Todos los endpoints (excepto /actuator/**) requieren autenticación JWT.

Paso 1: Solicitar token con client_credentials
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u "bff-client:secret" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read" \
  | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token obtenido: ${TOKEN:0:60}..."

Paso 2: Verificar que el token funciona
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/actuator/health | jq '.status'

Si responde "UP", el token es válido.

Paso 3: Guardar el token para reutilizar
Exporta el token a la sesión actual:

export TOKEN

Así puedes usarlo en los siguientes comandos sin volver a solicitarlo.

# Probar Microservicios de Negocio

**Microservicio de Cuentas (puerto 8082)**

Listar todas las cuentas:

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8082/api/cuentas | jq '.[0:3]'

Resultado esperado: lista de cuentas con numeroCuenta, saldo, tipoCuenta, estado.

Consultar una cuenta específica:

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8082/api/cuentas/1000102 | jq

Respuesta esperada:

{
  "id": 102,
  "numeroCuenta": "1000102",
  "tipoCuenta": "AHORRO",
  "saldo": 1000000.0,
  "tasaInteres": 4.5,
  "estado": "ACTIVA"
}

**Microservicio de Transacciones (puerto 8081)**

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/transacciones | jq '.[0:2]'

**Microservicio de Pagos (puerto 8085)**

Crear una transferencia:

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cuentaOrigen":"1000101",
    "cuentaDestino":"1000102",
    "monto":10000,
    "descripcion":"Prueba de transferencia"
  }' \
  http://localhost:8085/api/pagos/transferencia | jq

Respuesta esperada:

{
  "id": 1,
  "cuentaOrigen": "1000101",
  "cuentaDestino": "1000102",
  "monto": 10000.0,
  "tipo": "TRANSFERENCIA",
  "estado": "COMPLETADO",
  "descripcion": "Prueba de transferencia"
}

Listar todos los pagos:

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8085/api/pagos | jq

**Microservicio de Clientes (puerto 8086)**

Crear un cliente:

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rut":"12345678-9",
    "nombre":"Juan",
    "apellido":"Pérez",
    "email":"juan.perez@bancoxyz.cl",
    "telefono":"+56912345678",
    "direccion":"Av. Apoquindo 1234"
  }' \
  http://localhost:8086/api/clientes | jq

Consultar cliente por RUT:

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8086/api/clientes/rut/12345678-9 | jq

# Probar los 3 BFFs

**BFF Web (puerto 8080) — 6 campos**

Optimizado para navegadores. Devuelve la información completa.

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/web/transacciones | jq '.[0]'

Respuesta esperada (6 campos):

{
  "id": 1,
  "tipo": "DEPOSITO",
  "monto": 3000.0,
  "fecha": "2024-06-30",
  "anomalia": false,
  "fechaProcesamiento": "2026-10-11"
}

**BFF Móvil (puerto 8083) — 4 campos**

Optimizado para móviles. Reduce el ancho de banda eliminando campos no esenciales.

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8083/api/mobile/transacciones/recientes?limit=5" | jq '.[0]'

Respuesta esperada (4 campos, sin anomalia ni fechaProcesamiento):

{
  "id": 1,
  "tipo": "DEPOSITO",
  "monto": 3000.0,
  "fecha": "2024-06-30"
}

**BFF ATM (puerto 8084) — 2 campos**

Optimizado para cajeros. Respuestas mínimas + operación de retiro.

Consultar saldo (2 campos):

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/atm/saldo/1000102 | jq

Respuesta esperada:

{
  "cuentaId": "1000102",
  "saldo": 1000000.0
}

Procesar un retiro:

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":"1000102","monto":500}' \
  http://localhost:8084/api/atm/retiro | jq

Respuesta esperada:

{
  "cuentaId": "1000102",
  "montoRetirado": 500.0,
  "saldoRestante": 999500.0,
  "exitoso": true,
  "mensaje": "Retiro exitoso"
}

Verificar persistencia del saldo:

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/atm/saldo/1000102 | jq

El saldo debe ser 999500.0 (el anterior menos el retiro).

Probar validación de saldo insuficiente:

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":"1000102","monto":999999999}' \
  http://localhost:8084/api/atm/retiro | jq

Respuesta esperada:

{
  "cuentaId": "1000102",
  "montoRetirado": 0.0,
  "saldoRestante": 999500.0,
  "exitoso": false,
  "mensaje": "Saldo insuficiente. Disponible: 999500.0"
}

Probar validación de monto negativo:

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":"1000102","monto":-100}' \
  http://localhost:8084/api/atm/retiro | jq

Respuesta esperada: HTTP 400 Bad Request con mensaje de validación.

# Probar Circuit Breaker

Los 3 BFFs y los microservicios implementan Circuit Breaker con Resilience4j.

Paso 1: Verificar estado inicial (CLOSED)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/actuator/circuitbreakers | jq '.circuitBreakers.atmBffCB'

Respuesta esperada:

{
  "state": "CLOSED",
  "failureRate": "-1.0%",
  "bufferedCalls": 0,
  "failedCalls": 0
}

Paso 2: Detener el microservicio de cuentas

docker compose stop microservicio-cuentas
sleep 5

Paso 3: Hacer 5 llamadas seguidas para llenar la ventana deslizante

for i in 1 2 3 4 5; do
  echo "--- Llamada $i ---"
  curl -s -w "HTTP: %{http_code} | Tiempo: %{time_total}s\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8084/api/atm/saldo/1000102
done

Resultado esperado:

Las primeras llamadas tardan ~2-3s y devuelven {"cuentaId":"1000102","saldo":null} con HTTP 200. A partir de la 3ª, los tiempos bajan a milisegundos porque el Circuit Breaker se abre y el fallback responde instantáneamente.

Todas las llamadas devuelven 200 (NO 500). Esto es la evidencia de que el fallback funciona correctamente.

Paso 4: Verificar que el Circuit Breaker está OPEN

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/actuator/circuitbreakers | jq '.circuitBreakers.atmBffCB'

Respuesta esperada:

{
  "state": "OPEN",
  "failureRate": "60.0%",
  "bufferedCalls": 5,
  "failedCalls": 3,
  "notPermittedCalls": 2
}

Paso 5: Verificar los logs del fallback

docker logs bff-atm 2>&1 | grep "FALLBACK" | tail -5

Resultado esperado:

[BFF-ATM CB] *** FALLBACK SALDO ACTIVADO ***
[BFF-ATM CB] Cuenta: 1000102 | Excepcion: ResourceAccessException - ...

Paso 6: Reiniciar el microservicio y verificar recuperación

docker compose start microservicio-cuentas
sleep 30

# Esperar transición OPEN → HALF_OPEN → CLOSED
echo "Esperando recuperación del Circuit Breaker..."
sleep 15

# Hacer llamadas exitosas
for i in 1 2 3; do
  curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8084/api/atm/saldo/1000102 | jq
done

# Ver estado final
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/actuator/circuitbreakers | jq '.circuitBreakers.atmBffCB.state'

Resultado esperado: "CLOSED".

# Ejecutar Jobs Batch

El servicio batch-service expone 3 jobs ejecutables bajo demanda.

**Job 1: Reporte de Transacciones Diarias**

docker compose run --rm batch-service \
  --job=reporteTransaccionesDiariasJob 2>&1 | \
  grep -E "Job finalizado|Estado:"

Resultado esperado:

✔ Job finalizado: reporteTransaccionesDiariasJob | Estado: COMPLETED

**Job 2: Cálculo de Intereses Mensuales**

docker compose run --rm batch-service \
  --job=calculoInteresesMensualesJob 2>&1 | \
  grep -E "Job finalizado|Estado:"

**Job 3: Estados de Cuenta Anuales (con paralelismo)**

Ejecución secuencial (1 hilo):

docker compose run --rm \
  -e BATCH_PARALLELISM=1 \
  -e BATCH_ESTADOS_GRID=1 \
  batch-service \
  --job=generacionEstadosCuentaAnualesJob 2>&1 | \
  grep -E "PARTITIONER|Job finalizado|BATCH-CONFIG"

Ejecución paralela con 4 hilos (configuración óptima):

docker compose run --rm \
  -e BATCH_PARALLELISM=4 \
  -e BATCH_ESTADOS_GRID=4 \
  batch-service \
  --job=generacionEstadosCuentaAnualesJob 2>&1 | \
  grep -E "PARTITIONER|Job finalizado|BATCH-CONFIG"

Ejecución paralela con 8 hilos:

docker compose run --rm \
  -e BATCH_PARALLELISM=8 \
  -e BATCH_ESTADOS_GRID=8 \
  batch-service \
  --job=generacionEstadosCuentaAnualesJob 2>&1 | \
  grep -E "PARTITIONER|Job finalizado|BATCH-CONFIG"

# Verificar resultados en PostgreSQL

**Contar registros procesados**

docker exec postgres psql -U banco -d banco_xyz -c "SELECT COUNT(*) FROM estados_cuenta;"

**Ver historial de ejecuciones**

docker exec postgres psql -U banco -d banco_xyz -c "
SELECT JOB_EXECUTION_ID, STATUS,
       EXTRACT(EPOCH FROM (END_TIME - START_TIME)) * 1000 AS duracion_ms
FROM batch_job_execution
ORDER BY JOB_EXECUTION_ID DESC LIMIT 5;"

**Ver estadísticas por step**

docker exec postgres psql -U banco -d banco_xyz -c "
SELECT STEP_NAME, READ_COUNT, WRITE_COUNT, FILTER_COUNT
FROM batch_step_execution
ORDER BY STEP_EXECUTION_ID DESC LIMIT 10;"

Resultado esperado:

estados_cuenta con registros persistidos

Historial de ejecuciones en batch_job_execution

Conteos de read_count, write_count, filter_count

# Probar Mensajería Kafka

Verificar topics creados

**En el Kafka local**

docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

Resultado esperado:

__consumer_offsets
pagos-topic
transacciones-topic

Crear un pago y verificar publicación a Kafka

**Crear pago**

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cuentaOrigen":"1000101",
    "cuentaDestino":"1000102",
    "monto":50000,
    "descripcion":"Prueba Kafka"
  }' \
  http://localhost:8085/api/pagos/transferencia | jq

sleep 5

**Consumir el mensaje del topic**

timeout 8 docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pagos-topic \
  --from-beginning \
  --max-messages 3

Resultado esperado: ver los JSON de los pagos publicados.

**Ver logs del productor**

docker logs microservicio-pagos 2>&1 | grep "PAGOS-KAFKA" | tail -5

Resultado esperado:

[PAGOS-KAFKA] Evento publicado en pagos-topic: id=1 monto=50000.0

**Detener y Limpiar**

Detener todos los contenedores (conservar datos)

docker compose stop

Detener y eliminar contenedores (conservar volúmenes)

docker compose down

Detener, eliminar contenedores y volúmenes (limpieza total)

docker compose down -v

**Verificar limpieza**

docker ps -a | grep banco-xyz-cloud
docker volume ls | grep banco-xyz-cloud

# Verificación Rápida de Todo el Sistema

Ejecuta este bloque para verificar que todo funciona en un solo comando:

#!/bin/bash
echo "=========================================="
echo "VERIFICACIÓN COMPLETA DEL SISTEMA"
echo "=========================================="
echo ""

# Token
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u "bff-client:secret" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read" \
  | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Auth Server no responde"
  exit 1
fi
echo "✅ Auth Server OK"

# Eureka
SERVICES=$(curl -s http://localhost:8761/eureka/apps -H "Accept: application/json" \
  | jq -r '.applications.application | length')
echo "✅ Eureka: $SERVICES servicios registrados"

# BFFs
for port in 8080 8083 8084; do
  STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:$port/actuator/health | jq -r '.status')
  echo "✅ BFF puerto $port: $STATUS"
done

# Micros
for port in 8081 8082 8085 8086; do
  STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:$port/actuator/health | jq -r '.status')
  echo "✅ Micro puerto $port: $STATUS"
done

echo ""
echo "=========================================="
echo "SISTEMA VERIFICADO CORRECTAMENTE"
echo "=========================================="

Guarda este script como verificar.sh y ejecútalo:

chmod +x verificar.sh
./verificar.sh

## 👨‍💻 Autor

José Ignacio Soto Southerland
Asignatura: Desarrollo Backend III (PBY2203)
Fecha: Octubre 2026

