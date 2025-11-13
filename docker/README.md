# Docker 인프라 환경

이 디렉토리는 개발 환경에서 필요한 인프라(Redis, Kafka)를 Docker Compose로 관리합니다.

## 포함된 서비스

### 필수 서비스
- **Redis** (포트: 6379) - 캐시 및 세션 스토리지
- **Zookeeper** (포트: 2181) - Kafka 코디네이터
- **Kafka** (포트: 9092) - 메시지 브로커

### 관리 도구 (선택사항)
- **Kafka UI** (포트: 8090) - Kafka 토픽 및 메시지 모니터링
- **Redis Commander** (포트: 8081) - Redis 데이터 브라우저

## 사용 방법

### 1. 전체 서비스 시작
```bash
cd docker
docker-compose up -d
```

### 2. 특정 서비스만 시작
```bash
# Redis만 시작
docker-compose up -d redis

# Kafka만 시작 (Zookeeper 자동 시작됨)
docker-compose up -d kafka
```

### 3. 서비스 상태 확인
```bash
docker-compose ps
```

### 4. 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f redis
docker-compose logs -f kafka
```

### 5. 서비스 중지
```bash
# 전체 중지
docker-compose down

# 데이터 볼륨까지 삭제
docker-compose down -v
```

### 6. 서비스 재시작
```bash
docker-compose restart
```

## 접속 정보

### Redis
- **Host**: localhost
- **Port**: 6379
- **관리 UI**: http://localhost:8081

### Kafka
- **Bootstrap Server**: localhost:9092
- **Zookeeper**: localhost:2181
- **관리 UI**: http://localhost:8090

## 애플리케이션 설정

### application.properties 설정 예시
```properties
# Redis
redis.host=localhost
redis.port=6379

# Kafka
kafka.bootstrap-servers=localhost:9092
```

## Health Check

### Redis 연결 테스트
```bash
docker exec -it backend-redis redis-cli ping
# 응답: PONG
```

### Kafka 연결 테스트
```bash
# 토픽 목록 조회
docker exec -it backend-kafka kafka-topics --list --bootstrap-server localhost:9092

# 테스트 토픽 생성
docker exec -it backend-kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

## 데이터 지속성

모든 데이터는 Docker 볼륨에 저장되어 컨테이너를 재시작해도 유지됩니다:
- `redis-data`: Redis 데이터
- `kafka-data`: Kafka 메시지 데이터
- `zookeeper-data`: Zookeeper 데이터
- `zookeeper-logs`: Zookeeper 로그

## 문제 해결

### 포트 충돌
이미 포트가 사용 중인 경우 `docker-compose.yml`에서 포트 매핑을 변경하세요:
```yaml
ports:
  - "6380:6379"  # Redis 포트 변경 예시
```

### 컨테이너가 시작되지 않는 경우
```bash
# 로그 확인
docker-compose logs

# 컨테이너 강제 재생성
docker-compose up -d --force-recreate
```

### 데이터 초기화
```bash
# 모든 데이터 삭제 후 재시작
docker-compose down -v
docker-compose up -d
```

## 개발 팁

1. **Kafka UI 사용**: http://localhost:8090 에서 토픽 생성, 메시지 확인, 컨슈머 그룹 모니터링 가능
2. **Redis Commander 사용**: http://localhost:8081 에서 Redis 키 조회 및 수정 가능
3. **성능 모니터링**: 각 서비스의 Health Check로 상태 확인 가능

