# Backend Multi-Module Maven Project

Spring Framework 4.3.30 + Spring Boot 2.7.18 기반의 하이브리드 멀티모듈 Maven 프로젝트입니다.

## 📋 프로젝트 개요

**공공기관 신기술 도입 프로젝트**를 위한 현대적인 백엔드 시스템입니다.

### ✨ 주요 특징

- ✅ **전자정부프레임워크 미사용** - 순수 Spring Framework/Boot 기반
- ✅ **하이브리드 아키텍처** - Spring Framework (API/Web) + Spring Boot (Batch/Worker)
- ✅ **완전 독립적인 모듈 구조** - app 모듈 간, common 모듈 간 의존성 없음
- ✅ **마이크로서비스 지향** - Kafka 메시징, Redis 캐싱, Docker 컨테이너화
- ✅ **최신 기술 스택** - Kafka 2.8.2, Redis, Docker Compose
- ✅ **사용자별 순차 처리** - Kafka 파티셔닝 기반 순서 보장

## 🏗️ 프로젝트 구조

```
backend/
├── app/                    # 애플리케이션 모듈 (서로 독립)
│   ├── api/               # REST API 서버 (Spring MVC, WAR)
│   ├── web-static/        # SSR 템플릿 서버 (Thymeleaf, WAR)
│   ├── batch/             # 스케줄러 (Spring Boot, JAR)
│   └── worker/            # 비동기 처리 (Spring Boot, JAR)
├── common/                 # 공통 라이브러리 (서로 독립)
│   ├── core/              # 공통 유틸리티
│   ├── infra/             # 인프라 (Kafka, Redis, GameObject, HeavyTask)
│   └── web/               # 웹 공통 (필터, 예외처리, JWT)
├── docker/                 # Docker 개발 환경
│   ├── docker-compose.infra.yml          # Redis, Kafka, Zookeeper
│   ├── docker-compose.three-workers.yml  # 3 Worker 테스트 환경
│   └── Dockerfile.worker                  # Worker 컨테이너 이미지
└── pom.xml                # 루트 POM

📚 문서:
├── README.md                           # 프로젝트 개요 (이 파일)
├── MODULE-DEPENDENCY-ARCHITECTURE.md   # 모듈 의존성 구조
├── KAFKA-SEQUENCING-ARCHITECTURE.md    # Kafka 순차 처리 설계
├── CACHE-SERVICE-GUIDE.md              # CacheService 사용 가이드
├── EGOV-FRAMEWORK-ANALYSIS.md          # 전자정부프레임워크 분석
└── PUBLIC-PROJECT-WITHOUT-EGOV.md      # 공공 프로젝트 eGov 미사용 근거
```

## 📦 모듈 설명

### Application Modules (서로 독립)

#### app/api (Spring Framework 4.3.30, WAR)
```
역할: REST API 서버
배포: Apache Tomcat 9.0.112
포트: 8080
경로: /api/*

주요 기능:
- REST API 제공
- Kafka Producer (Task 전송)
- Redis 캐싱
- JWT 인증
- Swagger UI (개발 환경)

엔드포인트:
- GET  /api/health              # 헬스체크
- POST /api/test                # Redis + Kafka 통합 테스트
- POST /api/user-test           # 사용자별 순차 처리 테스트 (1000개)
- POST /api/clean-user-test     # 테스트 데이터 초기화
- POST /api/get-user-test-log   # 처리 결과 조회
- GET  /api/swagger-ui.html     # API 문서 (개발 환경)

의존성: common-core, common-infra, common-web
```

#### app/web-static (Spring Framework 4.3.30, WAR)
```
역할: SSR 템플릿 서버 (Server-Side Rendering)
배포: Apache Tomcat 9.0.112
포트: 8080
경로: /* (ROOT)
템플릿: Thymeleaf 3.0.15

주요 기능:
- Thymeleaf 기반 SSR
- Cocostudio 연동 준비
- 정적 리소스 서빙
- Spring Security

엔드포인트:
- GET / - 홈 페이지
- GET /about - 소개 페이지
- GET /cocostudio-example - Cocostudio 예시

의존성: common-core, common-web
```

#### app/batch (Spring Boot 2.7.18, JAR)
```
역할: 스케줄 기반 배치 작업
실행: java -jar batch.jar (run-batch.bat)
포트: 없음 (Non-Web)

주요 기능:
- Spring Scheduler (@Scheduled)
- 주기적 작업 실행
- Kafka Producer (필요 시)
- Redis 캐싱 (필요 시)

예시 작업:
- SampleScheduler: 10초마다 실행
- 데이터 정리, 집계 등

의존성: common-core
설정: application.yml
```

#### app/worker (Spring Boot 2.7.18, JAR)
```
역할: 비동기 작업 처리 (Kafka Consumer)
실행: java -jar worker.jar (run-worker.bat)
      docker-compose up -d (3 workers)
포트: 없음 (Non-Web)

주요 기능:
- Kafka Consumer (메시지 수신)
- HeavyTask 처리
- GameObject 파싱
- 사용자별 순차 처리 (Kafka 파티셔닝)
- Redis 읽기/쓰기

Task Consumer:
- TestHeavyTaskConsumer: 일반 작업 (병렬 처리)
- TestUserHeavyTaskConsumer: 사용자별 작업 (순차 처리)

의존성: common-core, common-infra
설정: application.yml
Kafka Concurrency: 1 (순서 보장)
```

### Common Modules (서로 독립)

#### common/core
```
역할: 프로젝트 전반의 공통 기능
의존성: 없음 (완전 독립)

포함 내용:
- 유틸리티 클래스
- 전역 상수 정의
- 공통 DTO/VO
- 예외 클래스
```

#### common/infra
```
역할: 인프라 연동 (Kafka, Redis)
의존성: 없음 (완전 독립)

포함 내용:
├── config/
│   ├── RedisConfig        # Redis 설정, ObjectMapper 빈
│   └── KafkaConfig        # Kafka 설정
├── cache/
│   └── CacheService       # Redis 캐싱 (String + Object 지원)
├── messaging/
│   └── KafkaProducerService  # Kafka 메시지 전송
├── gameobject/
│   ├── GameObject         # 게임 오브젝트 추상 클래스
│   ├── TestObject         # 테스트 오브젝트 구현
│   └── GameObjectParser   # Factory Pattern 파서
├── heavytask/
│   ├── HeavyTask          # 작업 기본 클래스
│   ├── HeavyTaskByUser    # 사용자별 순차 작업
│   ├── TestHeavyTask      # 일반 테스트 작업
│   └── TestUserHeavyTask  # 사용자 순차 테스트 작업
├── consumer/
│   └── BaseTaskConsumer   # Kafka Consumer 베이스 클래스
└── annotation/
    └── TaskConsumer       # Custom 어노테이션

주요 기능:
✅ CacheService.setObject() - Object 자동 JSON 변환
✅ Jackson Polymorphic Deserialization
✅ GameObject Factory Pattern
✅ BaseTaskConsumer 추상화
✅ Kafka 파티셔닝 기반 순차 처리
```

#### common/web
```
역할: 웹 관련 공통 기능
의존성: 없음 (완전 독립)

포함 내용:
- WebMvcConfig (Spring MVC 설정)
- GlobalExceptionHandler (전역 예외 처리)
- JwtTokenProvider (JWT 토큰)
- ApiResponse (API 응답 DTO)
- BusinessException (비즈니스 예외)
- 필터/인터셉터
```

## 🛠️ 기술 스택

### Core
- **JDK**: 17 (Eclipse Temurin)
- **Build Tool**: Maven 3.9.11
- **Application Server**: Apache Tomcat 9.0.112 (WAR 배포)

### Framework & Libraries

#### Spring Framework 4.3.30 (API, Web-Static)
```
- spring-core, spring-beans, spring-context
- spring-webmvc, spring-aop, spring-tx
- spring-security 4.2.13.RELEASE
```

#### Spring Boot 2.7.18 (Batch, Worker)
```
- spring-boot-starter
- spring-boot-starter-data-redis
- spring-kafka
- 자동 설정 (Auto-Configuration)
```

### Infrastructure
- **Messaging**: Apache Kafka 2.8.2
  - Spring Kafka 1.3.11 (Spring Framework)
  - Spring Boot Kafka Auto-config (Spring Boot)
  - 파티셔닝 기반 순차 처리
- **Cache**: Redis 6.2
  - Jedis 2.9.0
  - Spring Data Redis 1.8.23
  - StringRedisTemplate, ObjectMapper
- **Database**: MyBatis 3.5.13, HikariCP 4.0.3
- **Documentation**: Springfox Swagger 2.9.2 (개발 환경)
- **Container**: Docker, Docker Compose

### Development
- **Logging**: SLF4J 1.7.36 + Logback 1.2.13
- **JSON**: Jackson 2.9.10 + jackson-datatype-jsr310
- **JWT**: JJWT 0.9.1
- **Validation**: Hibernate Validator 6.2.5
- **Utilities**: Apache Commons Lang3 3.12.0
- **Lombok**: 1.18.30
- **Testing**: JUnit 4.13.2, Mockito 3.12.4

## 🚀 빌드 및 실행

### 전체 빌드
```bash
# 개발 환경 (Swagger 활성화)
mvn clean package -P development

# 운영 환경 (Swagger 비활성화)
mvn clean package -P production

# 또는 배치 스크립트 사용
mvn-build.bat           # 기본 (development)
mvn-dev-build.bat       # 개발
mvn-prod-build.bat      # 운영
```

### 개별 모듈 빌드
```bash
# API 서버
mvn clean package -P development -pl app/api -am

# Web Static 서버
mvn clean package -P development -pl app/web-static -am

# Batch 애플리케이션
mvn clean package -P development -pl app/batch -am

# Worker 애플리케이션
mvn clean package -P development -pl app/worker -am
```

### 실행

#### 1. Docker 인프라 시작 (필수)
```bash
cd docker
docker-compose -f docker-compose.infra.yml up -d

# 확인
docker ps

서비스:
- Redis: localhost:6379
- Kafka: localhost:9092
- Zookeeper: localhost:2181
- Kafka UI: localhost:8090
- Redis Commander: localhost:8081
```

#### 2. API 서버 (Tomcat)
```bash
# 빌드 후 배포
deploy-tomcat.bat

# 접속
http://localhost:8080/api/health
http://localhost:8080/api/swagger-ui.html
```

#### 3. Web Static 서버 (Tomcat)
```bash
# API와 함께 배포됨 (deploy-tomcat.bat)

# 접속
http://localhost:8080/
```

#### 4. Batch 애플리케이션
```bash
run-batch.bat

# 로그 확인
tail -f logs/application.log
```

#### 5. Worker 애플리케이션

**단일 Worker:**
```bash
run-worker.bat
```

**3 Workers (Docker, 테스트용):**
```bash
cd docker
.\build-and-start-workers.bat

# 로그 확인
docker logs -f worker-1
docker logs -f worker-2
docker logs -f worker-3

# 중지
.\stop-workers.bat
```

## 🧪 테스트

### API 테스트

#### 1. 헬스체크
```bash
curl http://localhost:8080/api/health
```

#### 2. Redis + Kafka 통합 테스트
```bash
# TestObject 생성 → Redis 저장 → Kafka 전송
curl -X POST http://localhost:8080/api/test

# Worker 로그에서 GameObject 조회 결과 확인
```

#### 3. 사용자별 순차 처리 테스트
```bash
# 초기화
curl -X POST http://localhost:8080/api/clean-user-test

# 1000개 Task 전송 (userId=1, userIndex 0~999)
curl -X POST http://localhost:8080/api/user-test

# 결과 확인 (순차 처리 검증: 0, 1, 2, ..., 999)
curl -X POST http://localhost:8080/api/get-user-test-log
```

### Swagger UI
```
개발 환경에서 접속:
http://localhost:8080/api/swagger-ui.html

운영 환경에서는 비활성화됨
```

## ⚙️ 환경 설정

### 프로파일
- **development** (기본): Swagger 활성화, 디버그 로그
- **production**: Swagger 비활성화, 정보 로그

### API 서버 (application.properties)
```properties
# Profile (Maven 빌드 시 자동 설정)
spring.profiles.active=@spring.profiles.active@

# Redis
redis.host=localhost
redis.port=6379
redis.password=

# Kafka
kafka.bootstrap-servers=localhost:9092

# JWT
jwt.secret=mySecretKey12345678901234567890
jwt.expiration=3600000
```

### Worker/Batch (application.yml)
```yaml
spring:
  profiles:
    active: development
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: worker-group
    listener:
      concurrency: 1  # 순차 처리 보장
```

## 🐳 Docker 환경

### 인프라 서비스
```bash
# 시작
cd docker
docker-compose -f docker-compose.infra.yml up -d

# 중지
docker-compose -f docker-compose.infra.yml down

# 로그
docker-compose -f docker-compose.infra.yml logs -f
```

### 관리 도구
- **Kafka UI**: http://localhost:8090
  - Topic 관리, 메시지 확인
- **Redis Commander**: http://localhost:8081
  - Redis 데이터 조회

### 3 Worker 테스트
```bash
# Worker 이미지 빌드 및 시작
cd docker
.\build-and-start-workers.bat

# 테스트 실행
curl -X POST http://localhost:8080/api/clean-user-test
curl -X POST http://localhost:8080/api/user-test
curl -X POST http://localhost:8080/api/get-user-test-log

# 확인: userIndex 0~999 순차적으로 처리됨

# 중지
.\stop-workers.bat
```

## 📚 아키텍처 특징

### 1. 완전 독립적인 모듈 구조
```
✅ app 모듈 간: 의존성 없음 (Kafka/REST로 통신)
✅ common 모듈 간: 의존성 없음 (완전 독립)
✅ 순환 의존: 없음 (단방향만)

→ 개별 모듈 독립 개발 가능
→ 빌드 병렬화 가능
→ 확장성 극대화
```

### 2. Kafka 파티셔닝 기반 순차 처리
```
메시지 전송:
- userId를 Kafka message key로 사용
- 같은 userId → 같은 파티션

파티션 할당:
- 파티션당 1개 Worker 할당
- concurrency=1 설정

결과:
✅ 같은 userId는 순차 처리
✅ 다른 userId는 병렬 처리
✅ Redis 동시성 제어 불필요
```

### 3. CacheService - Object 자동 변환
```java
// Before (수동)
String json = testObject.toJsonString();
cacheService.set("key", json);

// After (자동)
cacheService.setObject("key", testObject);  // ✨ 자동 JSON 변환

// 조회
TestObject obj = cacheService.getObject("key", TestObject.class);
```

### 4. 전자정부프레임워크 미사용
```
이유:
✅ 신기술 도입 필요 (Kafka, Redis, Docker, MSA)
✅ 최신 Spring Boot 활용
✅ 커스터마이징 자유
✅ 개발 효율성 향상

→ 공공기관 발주이지만 신기술 도입 프로젝트
→ 순수 Spring이 더 적합
→ 최근 공공 프로젝트 트렌드
```

## 📖 문서

### 아키텍처
- **MODULE-DEPENDENCY-ARCHITECTURE.md** - 모듈 의존성 구조 및 설계 원칙
- **KAFKA-SEQUENCING-ARCHITECTURE.md** - Kafka 기반 순차 처리 아키텍처

### 개발 가이드
- **CACHE-SERVICE-GUIDE.md** - CacheService 사용 가이드 (setObject 포함)
- **QUICKSTART.md** - 빠른 시작 가이드
- **docker/README.md** - Docker 환경 가이드

### 프로젝트 배경
- **EGOV-FRAMEWORK-ANALYSIS.md** - 전자정부프레임워크 분석
- **EGOV-FRAMEWORK-USE-CASES.md** - 전자정부프레임워크 사용처
- **PUBLIC-PROJECT-WITHOUT-EGOV.md** - 공공 프로젝트에서 eGov 미사용 근거

### 테스트
- **THREE-WORKERS-TEST-GUIDE.md** - 3 Worker 테스트 가이드
- **TEST-USER-SEQUENTIAL.md** - 사용자별 순차 처리 테스트

## 🔧 트러블슈팅

### Maven 빌드 실패
```bash
# 캐시 삭제 후 재빌드
mvn dependency:purge-local-repository
mvn clean install

# 개별 모듈만 빌드
mvn clean package -pl app/api -am
```

### Tomcat 배포 실패
```bash
# Tomcat 로그 확인
Get-Content C:\apache-tomcat-9.0.112\logs\catalina.*.log -Tail 50

# 포트 충돌 확인
netstat -ano | findstr :8080

# Tomcat 재시작
deploy-tomcat.bat restart
```

### Docker 서비스 연결 실패
```bash
# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs redis
docker logs kafka

# 네트워크 확인
docker network ls
docker network inspect backend-network

# 재시작
docker-compose -f docker-compose.infra.yml restart
```

### Kafka 메시지 전송 실패
```bash
# Kafka UI 접속
http://localhost:8090

# Topic 확인
- test-tasks
- user-test-tasks

# Consumer Group 확인
- worker-group
```

### Redis 연결 실패
```bash
# Redis Commander 접속
http://localhost:8081

# Redis CLI로 확인
docker exec -it redis redis-cli
> ping
> keys *
> get test:object:latest
```

### Worker가 메시지를 처리하지 않음
```bash
# Worker 로그 확인
docker logs -f worker-1

# Kafka Consumer Group 상태 확인 (Kafka UI)
http://localhost:8090

# application.yml 확인
- spring.kafka.consumer.group-id: worker-group
- spring.kafka.listener.concurrency: 1
```

## 💡 개발 팁

### 1. Profile 전환
```bash
# 개발 환경
mvn clean package -P development

# 운영 환경
mvn clean package -P production
```

### 2. 특정 모듈만 재빌드
```bash
# API만 빌드 (의존 모듈 포함)
mvn clean package -pl app/api -am

# Worker만 빌드
mvn clean package -pl app/worker -am
```

### 3. 로그 레벨 조정
```properties
# application.properties
logging.level.root=DEBUG
logging.level.com.example=DEBUG
```

### 4. Docker 이미지 재빌드
```bash
cd docker
docker-compose -f docker-compose.three-workers.yml build --no-cache
```

## 🎯 다음 단계

### 추천 학습 순서
1. **기본 이해**: README.md (이 파일)
2. **빠른 시작**: QUICKSTART.md
3. **모듈 구조**: MODULE-DEPENDENCY-ARCHITECTURE.md
4. **Kafka 처리**: KAFKA-SEQUENCING-ARCHITECTURE.md
5. **CacheService**: CACHE-SERVICE-GUIDE.md
6. **Docker 환경**: docker/README.md
7. **테스트**: THREE-WORKERS-TEST-GUIDE.md

### 프로젝트 확장
- [ ] 실제 비즈니스 로직 구현
- [ ] DB 연동 (MyBatis)
- [ ] 사용자 인증/인가 강화
- [ ] 모니터링 (Prometheus, Grafana)
- [ ] CI/CD 파이프라인
- [ ] Kubernetes 배포

## 📞 문의 및 지원

### 프로젝트 정보
- **프레임워크**: Spring Framework 4.3.30 + Spring Boot 2.7.18
- **빌드 도구**: Maven 3.9.11
- **JDK**: 17
- **전자정부프레임워크**: 미사용 (신기술 도입 프로젝트)

### 주요 기능
- ✅ REST API (Spring MVC)
- ✅ SSR 템플릿 (Thymeleaf)
- ✅ 스케줄러 (Spring Boot)
- ✅ 비동기 처리 (Kafka Consumer)
- ✅ Redis 캐싱
- ✅ Kafka 메시징
- ✅ Docker 컨테이너화
- ✅ 사용자별 순차 처리
