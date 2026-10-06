# Banco XYZ – Microservicios con Spring Cloud

Sistema de microservicios resiliente, seguro y cloud-ready para el Banco XYZ, desarrollado con Spring Cloud, OAuth2, Kafka, Resilience4j y Docker Compose.


## 📌 Objetivo del proyecto

Modernizar la arquitectura legacy del Banco XYZ mediante la implementación de un ecosistema de microservicios distribuidos que garantice:

- **Configuración centralizada** mediante un Config Server
- **Service Discovery** con Eureka Server
- **Autenticación y autorización** con OAuth2 + JWT
- **Tolerancia a fallos** con Resilience4j (Circuit Breaker + Fallback)
- **Mensajería asíncrona** con Apache Kafka
- **Backend for Frontend (BFF)** para canales Web y Móvil
- **Despliegue en contenedores** con Docker y Docker Compose

## 🏗️ Arquitectura

El proyecto implementa una arquitectura de microservicios distribuida con siete aplicaciones Spring Boot independientes que se comunican entre sí mediante HTTP y mensajería asíncrona. A continuación se describe cómo interactúa cada componente:

- **Servicios de infraestructura**

El Config Server (puerto 8888) actúa como el repositorio centralizado de configuración. Todos los microservicios y BFFs obtienen sus archivos .yml desde este servidor al iniciar, eliminando la necesidad de mantener configuraciones duplicadas en cada proyecto. El Eureka Server (puerto 8761) funciona como el registro de servicios: cada microservicio se registra al arrancar y consulta periódicamente la lista de servicios disponibles, permitiendo que se comuniquen por nombre lógico (microservicio-transacciones, bff-web, etc.) en lugar de direcciones IP fijas. El Auth Server (puerto 9000) implementa el protocolo OAuth2 con Spring Authorization Server, emitiendo tokens JWT firmados con RSA que son validados por los servicios protegidos.

- **Microservicios de negocio**

El microservicio-transacciones (puerto 8081) expone la API REST con los datos de transacciones cargados desde el archivo CSV legacy. Además de procesar las peticiones HTTP, actúa como Kafka Producer: cada vez que se crea una nueva transacción mediante POST /api/transacciones, publica un evento JSON en el topic transacciones-topic de Kafka. Este microservicio aplica Resilience4j (Circuit Breaker) para proteger las operaciones de lectura y escritura ante fallos de la base de datos. El microservicio-cuentas (puerto 8082) expone los datos de cuentas bancarias y actúa como Kafka Consumer: se suscribe al topic transacciones-topic y recibe los eventos publicados por el microservicio de transacciones, procesándolos de forma asíncrona sin bloquear su propia API REST.

- **Backends for Frontend (BFFs)**

El BFF Web (puerto 8080) está diseñado para navegadores de escritorio. Recibe peticiones autenticadas con JWT, las valida contra el Auth Server, y luego consume vía HTTP al microservicio-transacciones y microservicio-cuentas usando RestClient. Antes de reenviar la petición, el BFF propaga el token JWT mediante un interceptor personalizado, garantizando que la seguridad se mantenga de extremo a extremo. Devuelve las respuestas completas (6 campos por transacción) para interfaces ricas. El BFF Móvil (puerto 8083) sigue el mismo patrón, pero aplica transformaciones para reducir el tamaño de las respuestas: elimina campos como anomalia y fechaProcesamiento, y limita la cantidad de registros devueltos para optimizar el consumo de ancho de banda en dispositivos móviles. Ambos BFFs aplican Circuit Breaker con fallback: si el microservicio subyacente no responde, devuelven una lista vacía en lugar de propagar el error al cliente.

- **Flujo de una petición típica**

Cuando un cliente hace una petición autenticada al BFF Web, el flujo es el siguiente:

-El cliente obtiene un token JWT del Auth Server mediante POST /oauth2/token con credenciales bff-client:secret.
-El cliente envía la petición al BFF Web con el header Authorization: Bearer <token>.
-El BFF valida el token contra la configuración OIDC del Auth Server.
-El BFF propaga el mismo token al llamar al microservicio-transacciones vía RestClient.
-El microservicio valida el token, consulta su base de datos H2 y responde con el JSON.
-El BFF recibe la respuesta y aplica sus transformaciones si es necesario.
-El cliente recibe la respuesta final.

- **Mensajería asíncrona**

Cuando el microservicio-transacciones recibe una petición POST /api/transacciones, además de guardar la transacción en su base de datos, publica un evento JSON en el topic transacciones-topic de Kafka. El microservicio-cuentas está suscrito a ese topic (con su propio consumer group banco-xyz-group-cuentas-v2) y recibe el evento de forma asíncrona e independiente, registrándolo en sus logs para auditoría o para futuras actualizaciones de saldo. Esto desacopla ambos microservicios: el productor no necesita esperar confirmación del consumidor, y si uno falla, el otro sigue funcionando.

- **Diagrama de despliegue**

┌──────────────────────────────────────────────────────────────────────┐
│                    ECOSISTEMA BANCO XYZ                              │
│                                                                       │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐ │
│  │  Config Server │  │ Eureka Server  │  │   Auth Server          │ │
│  │    :8888       │  │    :8761       │  │  (OAuth2 + JWT) :9000  │ │
│  └────────────────┘  └────────────────┘  └────────────────────────┘ │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │  ┌──────────────┐   ┌──────────────┐                           │ │
│  │  │   BFF Web    │   │  BFF Móvil   │                           │ │
│  │  │    :8080     │   │    :8083     │                           │ │
│  │  └──────┬───────┘   └──────┬───────┘                           │ │
│  │         │                   │                                   │ │
│  │         └───────┬───────────┘                                   │ │
│  │                 │                                               │ │
│  │      ┌──────────▼──────────┐    ┌──────────────────────┐      │ │
│  │      │ Microservicio        │   │ Microservicio        │      │ │
│  │      │ Transacciones :8081  │──▶│ Cuentas :8082        │      │ │
│  │      │ (Kafka Producer)     │   │ (Kafka Consumer)     │      │ │
│  │      │ + Resilience4j       │   │ + Resilience4j       │      │ │
│  │      └──────────────────────┘   └──────────────────────┘      │ │
│  │                                                                 │ │
│  │  ┌───────────────┐      ┌────────────────┐                     │ │
│  │  │ Kafka :9092   │      │ Zookeeper:2181 │                     │ │
│  │  └───────────────┘      └────────────────┘                     │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘

## 🧰 Tecnologías utilizadas

El proyecto está construido sobre el siguiente stack tecnológico, agrupado por área funcional:

- **Lenguaje y framework base**

Se utiliza Java 17 como lenguaje principal, aprovechando las características modernas como records, switch expressions y mejoras en la API de Streams. Sobre este lenguaje, el proyecto emplea Spring Boot 3.3.5 como framework principal, que proporciona auto-configuración, servidor Tomcat embebido y gestión simplificada de dependencias. Para la gestión de dependencias y compilación se usa Apache Maven 3.9, con Lombok 1.18 como librería auxiliar que reduce el código boilerplate mediante anotaciones como @Data, @Slf4j y @RequiredArgsConstructor.

- **Ecosistema Spring Cloud**

La capa de integración distribuida se apoya en Spring Cloud 2023.0.3, que es la versión compatible con Spring Boot 3.3.5. Dentro de este ecosistema se utilizan tres componentes clave: Spring Cloud Config Server, que habilita la configuración centralizada para todos los microservicios; Spring Cloud Netflix Eureka, que implementa el registro y descubrimiento de servicios; y Spring Cloud Circuit Breaker con Resilience4j, que añade tolerancia a fallos con patrones de Circuit Breaker, retry y fallback.

- **Seguridad**

La seguridad del sistema se implementa con Spring Security 6.x como framework de autenticación y autorización, complementado con Spring Authorization Server, que implementa el protocolo OAuth2. Los tokens generados son JWT (JSON Web Tokens) firmados con algoritmos RSA de 2048 bits, lo que permite validar la autenticidad de cada petición sin necesidad de consultar al servidor de autorización en cada llamada.

- **Mensajería asíncrona**

Para la comunicación asíncrona entre microservicios se usa Spring Kafka 3.x como librería cliente, que se conecta al broker Apache Kafka 7.5.0 (distribución de Confluent). Kafka está coordinado por Zookeeper 7.5.0, también de Confluent. Los mensajes se serializan como JSON con el JsonSerializer de Spring Kafka, y se deserializan de forma segura como String para evitar problemas de tipo entre microservicios independientes.

- **Persistencia de datos**

Cada microservicio mantiene su propia base de datos H2 2.x en memoria, lo cual facilita las pruebas locales y garantiza el aislamiento entre servicios. Spring Data JPA se encarga de la capa de acceso a datos, con Hibernate 6.5 como implementación subyacente del estándar JPA. Los datos iniciales se cargan automáticamente desde los archivos CSV del repositorio legacy del Banco XYZ mediante un componente CsvDataLoader que se ejecuta al iniciar cada microservicio.

- **Contenedores y orquestación**

Todo el ecosistema se despliega con Docker 27.x y se orquesta con Docker Compose 2.x. Cada microservicio cuenta con su propio Dockerfile multi-etapa: la primera etapa usa la imagen maven:3.9-eclipse-temurin-17 para compilar el JAR con todas las dependencias, y la segunda etapa usa eclipse-temurin:17-jre-alpine (una imagen mínima de solo 47 MB) para ejecutar el JAR final. Esto reduce significativamente el tamaño de las imágenes y mejora la seguridad al no incluir el JDK completo en producción.

## 📁 Estructura del proyecto

banco-xyz-cloud/
├── config-server/ # Config Server (puerto 8888)
│ ├── src/main/java/com/bancoxyz/configserver/
│ │ └── ConfigServerApplication.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── eureka-server/ # Eureka Server (puerto 8761)
│ ├── src/main/java/com/bancoxyz/eurekaserver/
│ │ └── EurekaServerApplication.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── auth-server/ # Auth Server OAuth2 (puerto 9000)
│ ├── src/main/java/com/bancoxyz/authserver/
│ │ ├── AuthServerApplication.java
│ │ └── config/SecurityConfig.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── microservicio-transacciones/ # Microservicio 1 (puerto 8081)
│ ├── src/main/java/com/bancoxyz/transacciones/
│ │ ├── TransaccionesApplication.java
│ │ ├── config/
│ │ │ ├── SecurityConfig.java
│ │ │ ├── KafkaConfig.java
│ │ │ └── CsvDataLoader.java
│ │ ├── controller/TransaccionController.java
│ │ ├── model/Transaccion.java
│ │ ├── repository/TransaccionRepository.java
│ │ └── service/
│ │ ├── TransaccionService.java
│ │ └── KafkaProducerService.java
│ ├── src/main/resources/
│ │ ├── application.yml
│ │ └── transacciones.csv
│ ├── Dockerfile
│ └── pom.xml
│
├── microservicio-cuentas/ # Microservicio 2 (puerto 8082)
│ ├── src/main/java/com/bancoxyz/cuentas/
│ │ ├── CuentasApplication.java
│ │ ├── config/
│ │ │ ├── SecurityConfig.java
│ │ │ └── CsvDataLoader.java
│ │ ├── controller/CuentaController.java
│ │ ├── model/Cuenta.java
│ │ ├── repository/CuentaRepository.java
│ │ ├── service/CuentaService.java
│ │ └── listener/TransaccionConsumer.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── bff-web/ # BFF Web (puerto 8080)
│ ├── src/main/java/com/bancoxyz/bffweb/
│ │ ├── BffWebApplication.java
│ │ ├── config/
│ │ │ ├── SecurityConfig.java
│ │ │ └── RestClientConfig.java
│ │ ├── controller/WebBffController.java
│ │ ├── dto/TransaccionWebDTO.java
│ │ └── service/BffWebService.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── bff-movil/ # BFF Móvil (puerto 8083)
│ ├── src/main/java/com/bancoxyz/bffmovil/
│ │ ├── BffMovilApplication.java
│ │ ├── config/
│ │ │ ├── SecurityConfig.java
│ │ │ └── RestClientConfig.java
│ │ ├── controller/MobileBffController.java
│ │ ├── dto/TransaccionMobileDTO.java
│ │ └── service/BffMovilService.java
│ ├── src/main/resources/application.yml
│ ├── Dockerfile
│ └── pom.xml
│
├── config-repo/ # Configuraciones centralizadas
│ ├── auth-server.yml
│ ├── microservicio-transacciones.yml
│ ├── microservicio-cuentas.yml
│ ├── bff-web.yml
│ └── bff-movil.yml
│
├── docker-compose.yml # Orquestación de los 9 contenedores
├── .gitignore
├── .gitattributes
└── README.md

## 🚀 Instrucciones de ejecución

### Requisitos previos

- **Java 17** instalado
- **Docker Desktop** (con al menos 4 GB de RAM asignados)
- **Git** instalado

## Paso 1: Clonar el repositorio

git clone https://github.com/Dr3sden/Exp3_S8_DesarrolloBackendIII.git
cd banco-xyz-cloud

## Paso 2: Construir las imágenes Docker

docker compose build

## Paso 3: Levantar el ecosistema

docker compose up -d

## Paso 4: Esperar 3 minutos

Kafka y Zookeeper tardan ~60 segundos en estar completamente disponibles.

sleep 180

## Paso 5: Verificar que todos los contenedores estén corriendo

docker compose ps

Debes ver 9 contenedores con estado Up:

NAME                          STATUS
auth-server                   Up X minutes
bff-movil                     Up X minutes
bff-web                       Up X minutes
config-server                 Up X minutes
eureka-server                 Up X minutes
kafka                         Up X minutes
microservicio-cuentas         Up X minutes
microservicio-transacciones   Up X minutes
zookeeper                     Up X minutes

## Paso 6: Crear el topic de Kafka

docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic transacciones-topic --partitions 3 --replication-factor 1

Verificar:

docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

## 📡 Endpoints y pruebas

1. Eureka Server (Dashboard)

Abre en el navegador:

http://localhost:8761

Deberías ver 5 servicios registrados: AUTH-SERVER, BFF-MOVIL, BFF-WEB, MICROSERVICIO-CUENTAS, MICROSERVICIO-TRANSACCIONES.

2. Obtener token OAuth2

TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "bff-client:secret" \
  -d "grant_type=client_credentials&scope=read" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token: ${TOKEN:0:40}..."

3. Microservicio Transacciones (directo)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/transacciones

4. Microservicio Cuentas (directo)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/cuentas

5. BFF Web (datos completos)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/web/transacciones

Respuesta esperada: JSON con 6 campos por transacción.

6. BFF Móvil (datos reducidos)

curl -H "Authorization: Bearer $TOKEN" "http://localhost:8083/api/mobile/transacciones/recientes?limit=5"

Respuesta esperada: JSON con solo 4 campos y máximo 5 registros.

7. Prueba de seguridad (sin token)

curl -i http://localhost:8080/api/web/transacciones

Resultado esperado:
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer

8. OIDC Discovery

curl http://localhost:9000/.well-known/openid-configuration

9. Config Server

curl http://localhost:8888/auth-server/default

10. Prueba de Kafka (Producer + Consumer)

Crear una transacción (dispara el Producer):

NUEVO_ID=$((RANDOM + 100000))

curl -s -X POST http://localhost:8081/api/transacciones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"id\":$NUEVO_ID,\"tipo\":\"DEPOSITO\",\"monto\":3000.0,\"fecha\":\"2026-10-05\"}"

sleep 5

Ver logs del Producer:

docker compose logs microservicio-transacciones | grep "KAFKA-PRODUCER" | tail -3

Ver logs del Consumer:

docker compose logs microservicio-cuentas | grep -A 8 "KAFKA-CONSUMER" | tail -10

Resultado esperado:
Producer: [KAFKA-PRODUCER] Enviando transacción ID: XXXXXX
Consumer: ✅ [KAFKA-CONSUMER] Evento recibido: {id: XXXXXX, tipo: DEPOSITO, ...}

11. Prueba de Circuit Breaker

# Detener el microservicio de transacciones
docker compose stop microservicio-transacciones
sleep 5

# Llamar al BFF Web (debe activar el fallback)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/web/transacciones

# Ver logs del Circuit Breaker
docker compose logs bff-web | grep -i "fallback\|CB" | tail -3

# Reactivar el microservicio
docker compose start microservicio-transacciones

Resultado esperado: El BFF Web devuelve [] (lista vacía) por el fallback de Resilience4j.

## 🛡️ Seguridad (OAuth2 + JWT)

- **Flujo de autenticación**

Cliente → Auth Server (:9000) → Obtiene JWT
    ↓
Cliente → BFF (:8080) con JWT
    ↓
BFF propaga JWT al Microservicio (:8081)
    ↓
Microservicio valida JWT y responde

- **Credenciales**

El sistema OAuth2 está configurado con un cliente de prueba llamado bff-client, cuyo secreto es secret. Este cliente utiliza el flujo de autenticación client_credentials, lo cual significa que puede obtener un token JWT directamente sin necesidad de que un usuario interactúe con un formulario de login. El único scope habilitado es read, suficiente para consultar todos los endpoints protegidos del ecosistema.

Adicionalmente, el Auth Server tiene configurado un usuario en memoria con credenciales usuario: user y contraseña: password. Este usuario se utiliza únicamente para el flujo de form login cuando se accede a la consola del servidor de autorización (por ejemplo, al solicitar un token con flujo authorization_code desde el navegador). Para el uso normal de las APIs, basta con el cliente bff-client y el grant type client_credentials.

- **Roles por canal**

El sistema implementa tres roles distintos, uno por cada tipo de canal de acceso al Banco XYZ. Cada rol tiene un tiempo de expiración del token adaptado a la sensibilidad de las operaciones de su canal:

Canal Web — Rol asignado: ROLE_WEB_USER. El token tiene una duración de 1 hora. Se otorga a los usuarios que acceden desde navegadores de escritorio, donde la sesión puede ser más extensa porque las interacciones son consultas detalladas y reportes.

Canal Móvil — Rol asignado: ROLE_MOBILE_USER. El token tiene una duración de 30 minutos. Se otorga a dispositivos móviles, donde se busca un balance entre seguridad (tokens más cortos) y experiencia de usuario (evitar reautenticaciones frecuentes).

Canal Cajeros Automáticos (ATM) — Rol asignado: ROLE_ATM_USER. El token tiene una duración de 5 minutos. Se otorga a los cajeros automáticos, donde las operaciones son críticas (retiros, transferencias) y se requiere que cada operación cuente con un token reciente para minimizar el riesgo de robo o uso indebido.

Cada rol determina qué endpoints puede consumir el portador del token. Por ejemplo, un token emitido para el canal Móvil no puede acceder a los endpoints del BFF Web, y viceversa. Esto se controla mediante las reglas de authorizeHttpRequests configuradas en SecurityConfig.java de cada BFF, que exigen que la petición tenga el rol específico del canal correspondiente.

## ⚡ Tolerancia a fallos (Resilience4j)

El proyecto implementa Circuit Breaker con fallback en:

-BFF Web (webBffCB)
-BFF Móvil (mobileBffCB)
-Microservicio Transacciones (transaccionesCB)
-Microservicio Cuentas (cuentasCB)

- **Configuración**

resilience4j:
  circuitbreaker:
    instances:
      webBffCB:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5

## 📨 Mensajería asíncrona (Kafka)

El sistema implementa un canal de comunicación asíncrona basado en Apache Kafka para transferir eventos entre microservicios sin acoplarlos directamente. Esta arquitectura orientada a eventos permite que cada microservicio procese la información a su propio ritmo, mejorando la resiliencia y escalabilidad del ecosistema.

- **Topics utilizados**

- transacciones-topic: Topic principal del sistema. Cuenta con 3 particiones, lo que permite que múltiples consumidores procesen los mensajes en paralelo. Se utiliza para transferir los eventos de transacciones desde el microservicio productor hacia el microservicio consumidor. Adicionalmente, Kafka crea automáticamente el topic interno __consumer_offsets para gestionar los offsets de cada consumer group.

- **Flujo de eventos**

El flujo completo de la mensajería asíncrona es el siguiente:

1. El microservicio-transacciones actúa como Kafka Producer. Cuando recibe una petición POST /api/transacciones, guarda la transacción en su base de datos H2 y luego publica un mensaje JSON en el topic transacciones-topic usando KafkaTemplate.

2. El mensaje se serializa como JSON y se almacena en una de las tres particiones del topic, distribuido según la clave del mensaje (el ID de la transacción).

3. El microservicio-cuentas actúa como Kafka Consumer. Mantiene una conexión permanente con Kafka y está suscrito al topic transacciones-topic mediante el consumer group banco-xyz-group-cuentas-v2.

4. Kafka entrega el mensaje al consumer de forma automática. El consumer lo recibe como String (JSON crudo), lo parsea con ObjectMapper y lo registra en los logs para auditoría o para futuras actualizaciones del saldo de la cuenta relacionada.

5. El consumer confirma el procesamiento (auto-commit) y Kafka actualiza el offset del grupo, asegurando que el mensaje no se vuelva a entregar.

## 🐳 Docker

- **Dockerfiles multi-etapa**

Cada microservicio tiene un Dockerfile que usa dos etapas:

1. Build: maven:3.9-eclipse-temurin-17 → compila el JAR

2. Runtime: eclipse-temurin:17-jre-alpine → ejecuta el JAR

Esto reduce el tamaño de las imágenes finales y mejora la seguridad.

- **Comandos útiles**

# Construir todas las imágenes
docker compose build

# Construir un servicio específico
docker compose build bff-web

# Levantar todos los servicios
docker compose up -d

# Ver estado
docker compose ps

# Ver logs de un servicio
docker compose logs -f microservicio-transacciones

# Detener todo
docker compose down

# Eliminar todo (contenedores + volúmenes)
docker compose down -v --remove-orphans

## 📸 Evidencias de ejecución

Las capturas de pantalla y resultados de las pruebas se encuentran en el documento Evidencias_Exp3_S8.pdf

## 👤 Autor
Nombre: José Ignacio Soto Southerland
Curso: Desarrollo Backend III (PBY2203)
Semana: 8
Fecha de entrega: Octubre 2026

