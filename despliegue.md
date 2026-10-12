# ☁️ Guía de Despliegue en la Nube — AWS EC2

**Banco XYZ — Desarrollo Backend III (EFT)**
Guía paso a paso para desplegar Apache Kafka en AWS EC2 y conectar los microservicios locales.

## Arquitectura del Despliegue
┌─────────────────────────────────────────────────────────────┐
│ AWS EC2 (us-east-1) │
│ │
│ Instancia t3.medium │
│ Amazon Linux 2023 │
│ │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Docker Engine 25.0 │ │
│ │ │ │
│ │ ┌────────────────┐ ┌───────────────────────┐ │ │
│ │ │ Zookeeper │◄───│ Kafka │ │ │
│ │ │ puerto 2181 │ │ puerto 9092 (público)│ │ │
│ │ └────────────────┘ │ puerto 29092 (interno)│ │ │
│ │ └───────────────────────┘ │ │
│ └──────────────────────────────────────────────────────┘ │
│ │
│ Security Group: │
│ • 22 (SSH) → solo mi IP │
│ • 9092 (Kafka)→ 0.0.0.0/0 │
│ • 2181 (ZK) → solo mi IP │
│ • 29092 → solo dentro de la VPC │
└──────────────────────────────────────────────────────────────┘
▲
│
│ Conexión Kafka (TCP 9092)
│
▼
┌──────────────────────────────────────────────────────────────┐
│ TU PC LOCAL (Docker Compose) │
│ │
│ • microservicio-transacciones (producer) │
│ • microservicio-pagos (producer) │
│ • microservicio-cuentas (consumer) │
│ • BFFs, Eureka, Config Server, Auth Server │
│ │
│ SPRING_KAFKA_BOOTSTRAP_SERVERS=<IP_PUBLICA_EC2>:9092 │
└──────────────────────────────────────────────────────────────┘

Solo Kafka vive en AWS. Los microservicios siguen corriendo en local (Docker Desktop) pero **publican y consumen eventos desde el broker remoto en la nube**.

## Requisitos Previos

**Cuenta AWS** con permisos para:
- Crear instancias EC2
- Crear Security Groups
- Crear Key Pairs

**AWS CLI v2** instalado:
aws --version

# aws-cli/2.x.x Python/3.x.x Linux/x.x.x

Par de claves SSH (vockey en AWS Academy o propio).

Archivo PEM descargado y accesible.

Docker Desktop corriendo en tu PC local.

IP pública actual (para configurar el Security Group):
curl -s https://checkip.amazonaws.com

## Configuración de AWS CLI

Paso 1: Crear carpeta de credenciales

mkdir -p ~/.aws

Paso 2: Configurar credenciales

Obtén las credenciales desde tu panel de AWS Academy o consola IAM:

cat > ~/.aws/credentials << 'EOF'
[default]
aws_access_key_id=TU_ACCESS_KEY_ID
aws_secret_access_key=TU_SECRET_ACCESS_KEY
aws_session_token=TU_SESSION_TOKEN
EOF

Paso 3: Configurar región por defecto

cat > ~/.aws/config << 'EOF'
[default]
region = us-east-1
output = json
EOF

Paso 4: Verificar configuración

aws sts get-caller-identity

Resultado esperado:

{
    "UserId": "AROA...:user",
    "Account": "505691241538",
    "Arn": "arn:aws:sts::505691241538:assumed-role/voclabs/..."
}

Paso 5: Mover el archivo PEM a ubicación segura

mkdir -p ~/.aws-keys
mv ~/Descargas/banco-xyz-cloud/labsuser.pem ~/.aws-keys/cloud-lab.pem
chmod 400 ~/.aws-keys/cloud-lab.pem
ls -la ~/.aws-keys/cloud-lab.pem

Resultado esperado:

-r-------- 1 jose jose 1678 ... /home/jose/.aws-keys/cloud-lab.pem

**Crear Security Group**

El Security Group define qué puertos estarán accesibles desde internet.

Paso 1: Variables y creación

MI_IP=$(curl -s https://checkip.amazonaws.com)
echo "Mi IP pública: $MI_IP"

# Verificar VPC por defecto

VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" \
  --query 'Vpcs[0].VpcId' --output text)
echo "VPC: $VPC_ID"

# Crear Security Group

SG_ID=$(aws ec2 create-security-group \
  --group-name kafka-banco-xyz-sg \
  --description "SG para Kafka del Banco XYZ" \
  --vpc-id $VPC_ID \
  --query 'GroupId' --output text)

echo "Security Group: $SG_ID"
echo $SG_ID > /tmp/kafka_sg_id.txt

Paso 2: Abrir puertos

# SSH desde mi IP

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp --port 22 --cidr ${MI_IP}/32

# Kafka desde cualquier IP

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp --port 9092 --cidr 0.0.0.0/0

# Zookeeper desde mi IP

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp --port 2181 --cidr ${MI_IP}/32

# Puerto interno Kafka

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp --port 29092 --cidr 172.31.0.0/16

Paso 3: Verificar reglas

aws ec2 describe-security-groups --group-ids $SG_ID \
  --query 'SecurityGroups[0].IpPermissions[*].[IpProtocol,FromPort,ToPort,IpRanges[0].CidrIp]' \
  --output table

Resultado esperado:

-------------------------------------------------
|  tcp|  22    |  22    |  191.125.35.250/32    |
|  tcp|  29092 |  29092 |  172.31.0.0/16        |
|  tcp|  2181  |  2181  |  191.125.35.250/32    |
|  tcp|  9092  |  9092  |  0.0.0.0/0            |
+-----+--------+--------+-----------------------+
⚠️ Advertencia: Si tu IP cambia (ISP con IP dinámica), debes actualizar la regla de SSH:

NUEVA_IP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID --protocol tcp --port 22 --cidr ${NUEVA_IP}/32

**Lanzar Instancia EC2**

Paso 1: Obtener el AMI de Amazon Linux 2023

AMI_ID=$(aws ssm get-parameter \
  --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --query 'Parameter.Value' --output text)

echo "AMI: $AMI_ID"

Paso 2: Lanzar la instancia

INSTANCE_ID=$(aws ec2 run-instances \
  --image-id $AMI_ID \
  --instance-type t3.medium \
  --key-name vockey \
  --security-group-ids $SG_ID \
  --associate-public-ip-address \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=kafka-banco-xyz}]' \
  --query 'Instances[0].InstanceId' --output text)

echo "Instancia lanzada: $INSTANCE_ID"
echo $INSTANCE_ID > /tmp/kafka_instance_id.txt

Especificaciones:
Tipo: t3.medium (2 vCPU, 4 GB RAM) — suficiente para Kafka + Zookeeper
AMI: Amazon Linux 2023 (x86_64)
Almacenamiento: 8 GB gp3 por defecto

Paso 3: Esperar a que esté running

aws ec2 wait instance-running --instance-ids $INSTANCE_ID
echo "✅ Instancia corriendo"

Paso 4: Obtener la IP pública

PUBLIC_IP=$(aws ec2 describe-instances \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)

echo "IP pública: $PUBLIC_IP"
echo $PUBLIC_IP > /tmp/kafka_public_ip.txt

Paso 5: Esperar a que SSH esté disponible

echo "Esperando 45s a que SSH esté disponible..."
sleep 45

Paso 6: Verificar conexión SSH

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP \
  "echo 'SSH OK'; uname -a; cat /etc/os-release | head -2"

Resultado esperado:

SSH OK
Linux ip-172-31-XX-XX ... x86_64
NAME="Amazon Linux"
VERSION="2023"
Nota: El usuario de Amazon Linux 2023 es ec2-user (no ubuntu).

**Instalar Docker en la EC2**

Paso 1: Instalar Docker y Docker Compose

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP << 'ENDSSH'
set -e

# Actualizar sistema

sudo dnf update -y

# Instalar Docker

sudo dnf install -y docker

# Iniciar y habilitar Docker

sudo systemctl start docker
sudo systemctl enable docker

# Agregar ec2-user al grupo docker

sudo usermod -aG docker ec2-user

# Instalar Docker Compose v2

DOCKER_COMPOSE_VERSION="v2.24.6"
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Verificar

echo ""
echo "=== Verificación ==="
sudo docker --version
sudo docker compose version
ENDSSH

Resultado esperado:

Docker version 25.0.x, build ...
Docker Compose version v2.24.6

Paso 2: Verificar Docker sin sudo

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP \
  "docker ps && docker --version"

Resultado esperado:

CONTAINER ID   IMAGE   COMMAND   CREATED   STATUS   PORTS   NAMES
Docker version 25.0.x
Si docker ps falla con "permission denied", cerrar y reconectar SSH para que tome el grupo docker.

**Desplegar Kafka y Zookeeper**

Paso 1: Crear el docker-compose.yml en la EC2

El truco está en configurar los listeners correctamente para que clientes externos puedan conectarse.

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP << ENDSSH
set -e
mkdir -p ~/kafka && cd ~/kafka

EC2_PUBLIC_IP="${PUBLIC_IP}"

cat > docker-compose.yml << EOF
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - kafka-net
    restart: unless-stopped

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENERS: INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:29092,EXTERNAL://\${EC2_PUBLIC_IP}:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_HEAP_OPTS: "-Xmx512M -Xms256M"
    networks:
      - kafka-net
    restart: unless-stopped

networks:
  kafka-net:
    driver: bridge
EOF

echo "✅ docker-compose.yml creado"
ENDSSH

**Explicación de los listeners:**

KAFKA_LISTENERS: define dos listeners: INTERNAL en 29092 (para comunicación interna) y EXTERNAL en 9092 (para clientes remotos)

KAFKA_ADVERTISED_LISTENERS: le dice a Kafka qué dirección anunciar:

A contenedores internos: kafka:29092

A clientes externos: <IP_PUBLICA>:9092

KAFKA_INTER_BROKER_LISTENER_NAME: nombre del listener usado entre brokers (INTERNAL)

Este patrón es obligatorio para exponer Kafka correctamente detrás de NAT/EC2.

Paso 2: Levantar los contenedores

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP << 'ENDSSH'
cd ~/kafka
docker compose up -d
echo "Esperando 60s a que arranque Kafka..."
sleep 60
docker compose ps
ENDSSH

Resultado esperado:

NAME        IMAGE                          STATUS    PORTS
kafka       confluentinc/cp-kafka:7.5.0    Up        0.0.0.0:9092->9092/tcp
zookeeper   confluentinc/cp-zookeeper:7.5.0 Up        0.0.0.0:2181->2181/tcp

Paso 3: Verificar logs de Kafka

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP \
  "docker logs kafka --tail 20"

Busca la línea: [KafkaServer id=1] started.

**Crear Topics**

Los microservicios esperan 2 topics específicos:

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP << 'ENDSSH'
echo "=== Crear topic transacciones-topic ==="
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic transacciones-topic \
  --partitions 3 --replication-factor 1 2>/dev/null || echo "(ya existe)"

echo ""
echo "=== Crear topic pagos-topic ==="
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic pagos-topic \
  --partitions 3 --replication-factor 1 2>/dev/null || echo "(ya existe)"

echo ""
echo "=== Topics disponibles ==="
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

echo ""
echo "=== Detalle de transacciones-topic ==="
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic transacciones-topic
ENDSSH

Resultado esperado:

Topic: transacciones-topic  TopicId: ...  PartitionCount: 3  ReplicationFactor: 1
Topic: pagos-topic          TopicId: ...  PartitionCount: 3  ReplicationFactor: 1

**Conectar Microservicios Locales**

Paso 1: Actualizar docker-compose.yml local

Los microservicios locales deben apuntar al Kafka de AWS.

cd ~/Descargas/banco-xyz-cloud

# Hacer backup

cp docker-compose.yml docker-compose.yml.bak-aws

# Obtener IP de la EC2

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)
echo "Actualizando con IP: $PUBLIC_IP"

# Reemplazar bootstrap servers

python3 << PYEOF
import re

with open('docker-compose.yml', 'r') as f:
    contenido = f.read()

# Reemplazar cualquier IP previa

contenido = re.sub(
    r'SPRING_KAFKA_BOOTSTRAP_SERVERS: [\d\.]+:9092',
    'SPRING_KAFKA_BOOTSTRAP_SERVERS: ${PUBLIC_IP}:9092',
    contenido
)

# Si no existe la línea, agregarla a los micros que usan Kafka

if 'SPRING_KAFKA_BOOTSTRAP_SERVERS' not in contenido:
    contenido = contenido.replace(
        '      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/',
        '      SPRING_KAFKA_BOOTSTRAP_SERVERS: ${PUBLIC_IP}:9092\n      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/'
    )

with open('docker-compose.yml', 'w') as f:
    f.write(contenido)

print("✅ docker-compose.yml actualizado")
PYEOF

# Verificar

grep "SPRING_KAFKA_BOOTSTRAP_SERVERS" docker-compose.yml

Paso 2: Reiniciar microservicios con Kafka

docker compose up -d --force-recreate \
  microservicio-transacciones \
  microservicio-pagos \
  microservicio-cuentas

echo "Esperando 60s..."
sleep 60

docker compose ps microservicio-transacciones microservicio-pagos microservicio-cuentas

Paso 3: Verificar conexión a AWS

# Ver bootstrap servers configurados
docker logs microservicio-pagos 2>&1 | grep "bootstrap.servers"
docker logs microservicio-transacciones 2>&1 | grep "bootstrap.servers"

Resultado esperado:

bootstrap.servers = [35.153.202.176:9092]

Paso 4: Verificar accesibilidad del puerto

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

echo "=== Test: puerto 9092 accesible desde tu PC ==="
timeout 5 bash -c "</dev/tcp/$PUBLIC_IP/9092" && echo "✅ Puerto 9092 accesible" || echo "❌ No accesible"

**Verificar el Flujo End-to-End**

Paso 1: Obtener token OAuth2

TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u "bff-client:secret" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read" \
  | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token: ${TOKEN:0:50}..."

Paso 2: Publicar un pago desde local

curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cuentaOrigen":"1000101",
    "cuentaDestino":"1000102",
    "monto":99999,
    "descripcion":"Prueba AWS Kafka"
  }' \
  http://localhost:8085/api/pagos/transferencia | jq

echo ""
echo "Esperando 10s a que el mensaje llegue a AWS..."
sleep 10

Paso 3: Consumir el mensaje desde la EC2

PUBLIC_IP=$(cat /tmp/kafka_public_ip.txt)

ssh -i ~/.aws-keys/cloud-lab.pem -o StrictHostKeyChecking=no ec2-user@$PUBLIC_IP << 'ENDSSH'
echo "=== Mensajes en pagos-topic (EC2) ==="
timeout 10 docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pagos-topic \
  --from-beginning \
  --max-messages 3 2>/dev/null

echo ""
echo "=== Offsets del topic ==="
docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 \
  --topic pagos-topic 2>/dev/null
ENDSSH

Resultado esperado:

{"id":1,"cuentaOrigen":"1000101","cuentaDestino":"1000102","monto":99999.0,
 "tipo":"TRANSFERENCIA","estado":"COMPLETADO","descripcion":"Prueba AWS Kafka"}

pagos-topic:0:1
pagos-topic:1:0
pagos-topic:2:0

🎉 Si ves el JSON con el contenido del pago, el flujo end-to-end funciona correctamente.

**Detener y Limpiar Recursos**

Detener la instancia (conserva datos)

INSTANCE_ID=$(cat /tmp/kafka_instance_id.txt)

aws ec2 stop-instances --instance-ids $INSTANCE_ID
echo "Instancia detenida"

La instancia detenida conserva:
El disco EBS (los contenedores siguen instalados)
El Security Group
El Key Pair

Puedes reiniciarla después con:

aws ec2 start-instances --instance-ids $INSTANCE_ID

⚠️ Nota: Al detener y reiniciar, la IP pública cambia (a menos que asignes una Elastic IP). Debes actualizar SPRING_KAFKA_BOOTSTRAP_SERVERS con la nueva IP.

Eliminar todo (limpieza total)

INSTANCE_ID=$(cat /tmp/kafka_instance_id.txt)
SG_ID=$(cat /tmp/kafka_sg_id.txt)

# Terminar instancia
aws ec2 terminate-instances --instance-ids $INSTANCE_ID

# Esperar a que termine
aws ec2 wait instance-terminated --instance-ids $INSTANCE_ID

# Eliminar Security Group
aws ec2 delete-security-group --group-id $SG_ID

echo "✅ Recursos eliminados"
⚠️ Advertencia: Terminar la instancia es irreversible. Se pierde todo lo instalado.

Restaurar Kafka Local

Si quieres volver a usar Kafka local (el que se levanta con docker compose up -d):

cd ~/Descargas/banco-xyz-cloud

# Restaurar desde backup
cp docker-compose.yml.bak-aws docker-compose.yml

# Reiniciar micros
docker compose up -d --force-recreate \
  microservicio-transacciones microservicio-pagos

echo "✅ Kafka local restaurado"

O editar manualmente:

# Cambiar la IP por "kafka"
sed -i 's|SPRING_KAFKA_BOOTSTRAP_SERVERS: [^:]*:9092|SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092|g' docker-compose.yml

## 👨‍💻 Autor

José Ignacio Soto Southerland
Asignatura: Desarrollo Backend III (PBY2203)
Fecha: Octubre 2026
