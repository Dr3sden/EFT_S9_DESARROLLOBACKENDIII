# 🏦 Banco XYZ — Desarrollo Backend III

## 📋 Descripción del Proyecto

Migración completa del sistema legacy del **Banco XYZ** (COBOL + Shell sobre mainframe) a una arquitectura moderna de **microservicios en la nube**, integrando:

- **Procesos Batch** migrados a Spring Batch con particionamiento paralelo
- **Patrón Backend for Frontend (BFF)** para 3 canales: Web, Móvil y ATM
- **Microservicios de negocio** con Spring Cloud (Eureka + Config + Resilience4j)
- **Seguridad distribuida** con OAuth2 + JWT (RSA)
- **Mensajería asíncrona** con Apache Kafka desplegado en AWS EC2
- **Contenedores** con Docker + Docker Compose

## 🏗️ Arquitectura General
┌─────────────────────────────────────────────────────────────────────────┐
│ AWS EC2 (us-east-1) │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Apache Kafka + Zookeeper │ │
│ │ Topics: transacciones-topic (3p) · pagos-topic (3p) │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────▲───────────────────────────────────────────┘
│
│ Kafka protocol (9092)
│
┌─────────────────────────────┴───────────────────────────────────────────┐
│ DOCKER COMPOSE (local) │
│ │
│ ┌─ Infraestructura ────────────────────────────────────────────────┐ │
│ │ config-server (8888) · eureka-server (8761) · auth-server │ │
│ │ (OAuth2 + JWT RSA) (Service Discovery) (9000) │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ │
│ ┌─ Microservicios de Negocio ──────────────────────────────────────┐ │
│ │ microservicio-transacciones (8081) ← Kafka Producer │ │
│ │ microservicio-cuentas (8082) ← Kafka Consumer │ │
│ │ microservicio-pagos (8085) ← Kafka Producer │ │
│ │ microservicio-clientes (8086) │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ │
│ ┌─ Backend For Frontend (BFF) ─────────────────────────────────────┐ │
│ │ bff-web (8080) → 6 campos por transacción (completo) │ │
│ │ bff-movil (8083) → 4 campos por transacción (optimizado) │ │
│ │ bff-atm (8084) → 2 campos por saldo + retiro con CB │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ │
│ ┌─ Procesos Batch ─────────────────────────────────────────────────┐ │
│ │ batch-service (8087) → 3 jobs + PostgreSQL + paralelismo │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ │
│ ┌─ Persistencia ───────────────────────────────────────────────────┐ │
│ │ PostgreSQL 16 (5433 → 5432) · H2 in-memory (micros) │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘

## 📦 Componentes

### Infraestructura

**`config-server`** — Puerto **8888**
Servidor de configuración centralizada (Spring Cloud Config). Todos los servicios obtienen su configuración desde este servidor al arrancar.

**`eureka-server`** — Puerto **8761**
Registro y descubrimiento de servicios (Netflix Eureka). Permite que los servicios se encuentren entre sí por nombre lógico en lugar de IP.

**`auth-server`** — Puerto **9000**
Authorization Server OAuth2 con JWT firmados con RSA. Emite tokens JWT para autenticar los microservicios y BFFs.

### Microservicios de Negocio

**`microservicio-transacciones`** — Puerto **8081**
- **Kafka:** Producer
- **Responsabilidad:** Gestión de transacciones bancarias
- **Publica en:** `transacciones-topic`

**`microservicio-cuentas`** — Puerto **8082**
- **Kafka:** Consumer
- **Responsabilidad:** Consulta y retiro de cuentas
- **Consume de:** `transacciones-topic`

**`microservicio-pagos`** — Puerto **8085**
- **Kafka:** Producer
- **Responsabilidad:** Transferencias y depósitos
- **Publica en:** `pagos-topic`

**`microservicio-clientes`** — Puerto **8086**
- **Kafka:** No utiliza
- **Responsabilidad:** CRUD de clientes del banco

### Backend For Frontend (BFF)

**`bff-web`** — Puerto **8080**
- **Canal:** Navegador web
- **Campos de respuesta:** 6 campos completos por transacción
- **Optimización:** Datos completos con toda la información disponible

**`bff-movil`** — Puerto **8083**
- **Canal:** Aplicación móvil
- **Campos de respuesta:** 4 campos reducidos por transacción
- **Optimización:** Reduce ancho de banda eliminando campos no esenciales

**`bff-atm`** — Puerto **8084**
- **Canal:** Cajero automático
- **Campos de respuesta:** 2 campos por saldo
- **Optimización:** Respuestas mínimas + operación exclusiva de retiro con actualización de saldo

### Procesos Batch

**`reporteTransaccionesDiariasJob`**
- Reporte diario de transacciones con detección de anomalías
- Procesamiento **secuencial**
- Valida y filtra registros inválidos (fechas, tipos, montos)

**`calculoInteresesMensualesJob`**
- Cálculo de intereses sobre cuentas de ahorro y préstamos
- Procesamiento **secuencial**
- Aplica tasa según tipo de cuenta (AHORRO 3%, PRÉSTAMO 12%, HIPOTECA 6%)

**`generacionEstadosCuentaAnualesJob`**
- Estados de cuenta anuales con particionamiento paralelo
- Procesamiento **paralelo configurable** con 1, 2, 4 u 8 hilos
- Configuración óptima: 4 hilos (speedup 1.71x sobre 2151 ms base)

## 🛠️ Stack Tecnológico

**Lenguaje:** Java 17

**Framework principal:** Spring Boot 3.3.5

**Spring Cloud 2023.0.3:**
- Config Server (configuración centralizada)
- Netflix Eureka (service discovery)
- Circuit Breaker con Resilience4j (tolerancia a fallos)

**Spring Batch 5.1.2:**
- Procesamiento batch con Chunk-Oriented Processing
- Particionamiento paralelo con TaskExecutor
- Skip/Retry policies configurables

**Seguridad:**
- Spring Security 6.3
- OAuth2 Authorization Server
- JWT firmados con RSA de 2048 bits

**Mensajería:**
- Apache Kafka 3.7 (Confluent Platform 7.5.0)
- Topics particionados (3 particiones por topic)

**Persistencia:**
- PostgreSQL 16 (producción, batch-service)
- H2 in-memory (microservicios)
- Spring Data JPA + Hibernate 6.5

**Contenedores:**
- Docker 25.0
- Docker Compose v2.24

**Cloud Provider:**
- AWS EC2 instancia `t3.medium`
- Región `us-east-1`
- Amazon Linux 2023

**Build:**
- Maven 3.9

**Utilidades:**
- Lombok 1.18.34
- Jackson 2.17
- Logback 1.5

## 🚀 Requisitos Previos

- **JDK 17** o superior
- **Maven 3.9+** (o usar el `mvnw` incluido)
- **Docker 24+** y **Docker Compose v2**
- **8 GB de RAM** disponibles (mínimo)
- **Git**

## 📥 Instalación

# Clonar el repositorio

git clone https://github.com/Dr3sden/banco-xyz-cloud.git
cd banco-xyz-cloud

▶️ Ejecución Local

1. Levantar todo el ecosistema

docker compose up -d

Esto levanta 13 contenedores:

3 infraestructura (config, eureka, auth)

4 microservicios (transacciones, cuentas, pagos, clientes)

3 BFFs (web, móvil, atm)

1 batch-service

1 PostgreSQL

1 Kafka + Zookeeper (en AWS EC2 se despliega por separado)

2. Verificar estado

docker compose ps

Esperar a que todos estén en estado Up. Kafka tarda ~60s en arrancar.

3. Verificar en Eureka

Abrir en el navegador: http://localhost:8761

Deberías ver los siguientes servicios registrados:

BFF-ATM

BFF-MOVIL

BFF-WEB

MICROSERVICIO-CLIENTES

MICROSERVICIO-CUENTAS

MICROSERVICIO-PAGOS

MICROSERVICIO-TRANSACCIONES

AUTH-SERVER

# 🔐 Obtener Token OAuth2

Todos los endpoints (excepto /actuator/**) requieren autenticación JWT.

TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u "bff-client:secret" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read" \
  | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token: ${TOKEN:0:50}..."

# 🧪 Pruebas de los BFFs

BFF Web (6 campos por transacción)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/web/transacciones | jq '.[0]'

Respuesta esperada:

json
{
  "id": 1,
  "tipo": "DEPOSITO",
  "monto": 3000.0,
  "fecha": "2024-06-30",
  "anomalia": false,
  "fechaProcesamiento": "2026-10-11"
}

BFF Móvil (4 campos por transacción)

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8083/api/mobile/transacciones/recientes?limit=5" | jq '.[0]'

Respuesta esperada:

json
{
  "id": 1,
  "tipo": "DEPOSITO",
  "monto": 3000.0,
  "fecha": "2024-06-30"
}

BFF ATM (2 campos por saldo)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/atm/saldo/1000102 | jq
  
Respuesta esperada:

json
{
  "cuentaId": "1000102",
  "saldo": 1000000.0
}

BFF ATM — Retiro con actualización de saldo

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":"1000102","monto":500}' \
  http://localhost:8084/api/atm/retiro | jq

Respuesta esperada:

json
{
  "cuentaId": "1000102",
  "montoRetirado": 500.0,
  "saldoRestante": 999500.0,
  "exitoso": true,
  "mensaje": "Retiro exitoso"
}

# 📊 Ejecución de Jobs Batch

Los jobs se ejecutan bajo demanda con parámetro --job=<nombre>:

Job 1: Reporte de Transacciones Diarias

docker compose run --rm batch-service \
  --job=reporteTransaccionesDiariasJob

Job 2: Cálculo de Intereses Mensuales

docker compose run --rm batch-service \
  --job=calculoInteresesMensualesJob

Job 3: Estados de Cuenta Anuales (con paralelismo)

# Con 1 hilo (secuencial)
docker compose run --rm \
  -e BATCH_PARALLELISM=1 -e BATCH_ESTADOS_GRID=1 \
  batch-service --job=generacionEstadosCuentaAnualesJob

# Con 4 hilos (óptimo)
docker compose run --rm \
  -e BATCH_PARALLELISM=4 -e BATCH_ESTADOS_GRID=4 \
  batch-service --job=generacionEstadosCuentaAnualesJob

# Con 8 hilos
docker compose run --rm \
  -e BATCH_PARALLELISM=8 -e BATCH_ESTADOS_GRID=8 \
  batch-service --job=generacionEstadosCuentaAnualesJob

Verificar resultados en PostgreSQL

docker exec postgres psql -U banco -d banco_xyz -c "SELECT COUNT(*) FROM estados_cuenta;"

## ☁️ Despliegue en AWS

Ver el documento despliegue.md para instrucciones paso a paso del despliegue de Kafka en AWS EC2.

Resumen:

Instancia EC2: t3.medium en us-east-1

AMI: Amazon Linux 2023

Docker 25 + Compose v2

Kafka + Zookeeper desplegados como contenedores

Puertos abiertos: 22 (SSH), 9092 (Kafka), 2181 (Zookeeper)

SPRING_KAFKA_BOOTSTRAP_SERVERS apunta a la IP pública de la EC2

## 🛡️ Tolerancia a Fallos (Resilience4j)

Todos los BFFs y microservicios implementan Circuit Breaker con fallback:

resilience4j:
  circuitbreaker:
    instances:
      atmBffCB:
        sliding-window-size: 5
        minimum-number-of-calls: 3
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        automatic-transition-from-open-to-half-open-enabled: true

Verificar estado del Circuit Breaker

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/actuator/circuitbreakers | jq

Probar el fallback

# Detener el microservicio
docker compose stop microservicio-cuentas

# Hacer 5 llamadas → las respuestas deben ser 200 con datos vacíos
for i in 1 2 3 4 5; do
  curl -s -w "HTTP: %{http_code} | Tiempo: %{time_total}s\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8084/api/atm/saldo/1000102
done

# Ver logs del fallback
docker logs bff-atm 2>&1 | grep "FALLBACK"

# Reiniciar
docker compose start microservicio-cuentas

## 📁 Estructura del Repositorio

banco-xyz-cloud/
├── config-server/                  Puerto 8888
├── eureka-server/                  Puerto 8761
├── auth-server/                    Puerto 9000
├── microservicio-transacciones/    Puerto 8081
├── microservicio-cuentas/          Puerto 8082
├── microservicio-pagos/            Puerto 8085
├── microservicio-clientes/         Puerto 8086
├── bff-web/                        Puerto 8080
├── bff-movil/                      Puerto 8083
├── bff-atm/                        Puerto 8084
├── batch-service/                  Puerto 8087
├── config-repo/                    Configuraciones YAML
├── evidencias/                     Capturas para el informe
├── docker-compose.yml              13 contenedores
├── readme.md                       Este archivo
├── instrucciones.md                Cómo ejecutar cada componente
├── despliegue.md                   Despliegue en AWS
└── Informe_Tecnico.pdf             Informe completo (plantilla Duoc)

## 👨‍💻 Autor

José Ignacio Soto Southerland
Asignatura: Desarrollo Backend III (PBY2203)
Fecha: Octubre 2026