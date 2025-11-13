# 🎨 Thymeleaf + Cocostudio 통합 가이드

## 개요

`web-static` 모듈은 **Thymeleaf 3.0.15**를 템플릿 엔진으로 사용합니다.  
이를 통해 **Cocostudio에서 생성한 HTML**을 거의 수정 없이 사용하면서, 서버에서 동적 데이터를 주입할 수 있습니다.

---

## 📂 프로젝트 구조

```
app/web-static/
├── src/main/
│   ├── java/com/example/web/
│   │   ├── config/
│   │   │   ├── WebAppInitializer.java    # Servlet 초기화
│   │   │   ├── RootConfig.java           # Root Context
│   │   │   └── WebConfig.java            # Thymeleaf 설정 ✅
│   │   └── controller/
│   │       └── HomeController.java       # 컨트롤러
│   └── resources/
│       └── templates/                    # Thymeleaf HTML 템플릿 ✅
│           ├── home.html
│           ├── about.html
│           └── cocostudio-example.html   # Cocostudio 통합 예시
└── pom.xml                               # Thymeleaf 의존성 추가됨
```

---

## ✅ Thymeleaf 설정 (WebConfig.java)

```java
@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/");  // 템플릿 경로
        templateResolver.setSuffix(".html");                  // 확장자
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);                 // 개발: false, 프로덕션: true
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        return viewResolver;
    }
}
```

---

## 🚀 Cocostudio HTML을 Thymeleaf로 변환하는 방법

### 1️⃣ Cocostudio 원본 HTML

```html
<!DOCTYPE html>
<html>
<head>
    <title>{title}</title>
    <script>
        const config = {
            apiEndpoint: '{api-endpoint}',
            secretKey: '{encrypted-secretKey}',
            userId: '{user-id}'
        };
    </script>
</head>
<body>
    <div class="user-info">
        사용자: {username}
        이메일: {email}
    </div>
</body>
</html>
```

### 2️⃣ Thymeleaf로 변환

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">  <!-- ✅ Thymeleaf 네임스페이스 추가 -->
<head>
    <title th:text="${title}">Default Title</title>  <!-- ✅ 서버에서 주입 -->
    
    <!-- ✅ JavaScript 변수에 서버 데이터 주입 -->
    <script th:inline="javascript">
        /*<![CDATA[*/
        const config = {
            apiEndpoint: /*[[${apiEndpoint}]]*/ 'http://localhost:8080/api',
            secretKey: /*[[${encryptedSecretKey}]]*/ 'default-key',
            userId: /*[[${userId}]]*/ '12345'
        };
        /*]]>*/
    </script>
</head>
<body>
    <div class="user-info">
        사용자: <span th:text="${username}">홍길동</span>
        이메일: <span th:text="${email}">test@example.com</span>
    </div>
</body>
</html>
```

### 3️⃣ 컨트롤러에서 데이터 전달

```java
@Controller
public class HomeController {

    @GetMapping("/cocostudio-page")
    public String cocostudioPage(Model model) {
        // ✅ 서버에서 동적 값 주입
        model.addAttribute("title", "My Page");
        model.addAttribute("username", "김철수");
        model.addAttribute("email", "kim@example.com");
        model.addAttribute("apiEndpoint", "https://api.production.com/api");
        model.addAttribute("encryptedSecretKey", "AES256_ENCRYPTED_KEY_12345");
        model.addAttribute("userId", "USER_67890");
        
        return "cocostudio-page";  // templates/cocostudio-page.html
    }
}
```

### 4️⃣ 결과 (클라이언트가 받는 HTML)

```html
<!DOCTYPE html>
<html>
<head>
    <title>My Page</title>
    <script>
        const config = {
            apiEndpoint: 'https://api.production.com/api',
            secretKey: 'AES256_ENCRYPTED_KEY_12345',
            userId: 'USER_67890'
        };
    </script>
</head>
<body>
    <div class="user-info">
        사용자: <span>김철수</span>
        이메일: <span>kim@example.com</span>
    </div>
</body>
</html>
```

---

## 📋 Thymeleaf 주요 문법

### HTML 속성 치환

```html
<!-- 텍스트 치환 -->
<span th:text="${username}">Default Name</span>

<!-- HTML 치환 (이스케이프 안 함) -->
<div th:utext="${htmlContent}"></div>

<!-- 속성 치환 -->
<img th:src="${imageUrl}" th:alt="${imageAlt}" />

<!-- 조건부 렌더링 -->
<div th:if="${user != null}">
    환영합니다, <span th:text="${user.name}"></span>!
</div>

<!-- 반복문 -->
<ul>
    <li th:each="item : ${items}" th:text="${item.name}">Item Name</li>
</ul>
```

### JavaScript 변수 주입

```html
<script th:inline="javascript">
    /*<![CDATA[*/
    const serverData = {
        apiKey: /*[[${apiKey}]]*/ 'default-key',
        userId: /*[[${userId}]]*/ '12345',
        environment: /*[[${environment}]]*/ 'development'
    };
    
    console.log('Server Data:', serverData);
    /*]]>*/
</script>
```

### URL 생성

```html
<!-- 절대 경로 -->
<a th:href="@{/about}">소개</a>

<!-- 파라미터 포함 -->
<a th:href="@{/user/{id}(id=${userId})}">사용자 프로필</a>

<!-- 쿼리 스트링 -->
<a th:href="@{/search(q=${query},page=${page})}">검색</a>
```

---

## 🌍 환경별 설정 (Development vs Production)

### WebConfig.java - 환경별 캐시 설정

```java
@Bean
public SpringResourceTemplateResolver templateResolver() {
    SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    // ...
    
    // 개발 환경: false (변경 즉시 반영)
    // 프로덕션: true (성능 향상)
    boolean isProduction = System.getenv("SPRING_PROFILES_ACTIVE").equals("production");
    resolver.setCacheable(isProduction);
    
    return resolver;
}
```

### 컨트롤러 - 환경별 API Endpoint 주입

```java
@Controller
public class HomeController {

    @Value("${api.endpoint}")
    private String apiEndpoint;

    @Value("${app.secret.key}")
    private String secretKey;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("apiEndpoint", apiEndpoint);         // Dev: localhost:8080
        model.addAttribute("encryptedSecretKey", secretKey);    // Prod: ENCRYPTED_KEY
        return "home";
    }
}
```

---

## 🎯 Cocostudio 산출물 통합 워크플로우

### Step 1: Cocostudio에서 HTML 생성
디자이너가 Cocostudio에서 HTML/CSS/JS를 생성합니다.

### Step 2: Thymeleaf 템플릿으로 변환
1. HTML 파일을 `src/main/resources/templates/`에 복사
2. `<html>` 태그에 Thymeleaf 네임스페이스 추가:
   ```html
   <html xmlns:th="http://www.thymeleaf.org">
   ```

### Step 3: 동적 데이터 부분 식별
- `{placeholder}` → `th:text="${variable}"`
- JavaScript 설정 → `th:inline="javascript"` + `/*[[${variable}]]*/`

### Step 4: 컨트롤러에서 데이터 주입
```java
@GetMapping("/your-page")
public String yourPage(Model model) {
    model.addAttribute("variable", "value");
    return "your-page";
}
```

### Step 5: 빌드 & 배포
```bash
mvn clean package -P development
```

---

## 🔒 보안 Best Practices

### 1. Secret Key는 서버에서만 관리
```java
@Value("${app.secret.key}")
private String secretKey;

model.addAttribute("encryptedSecretKey", encryptionService.encrypt(secretKey));
```

### 2. XSS 방지 (자동 이스케이프)
```html
<!-- 자동 이스케이프 (안전) -->
<span th:text="${userInput}"></span>

<!-- 이스케이프 안 함 (신뢰할 수 있는 HTML만) -->
<div th:utext="${trustedHtml}"></div>
```

---

## 📚 참고 자료

- [Thymeleaf 공식 문서](https://www.thymeleaf.org/documentation.html)
- [Thymeleaf + Spring 통합 가이드](https://www.thymeleaf.org/doc/tutorials/3.0/thymeleafspring.html)
- 프로젝트 예시: `http://localhost:8080/cocostudio-example`

---

## ✅ 체크리스트

### Cocostudio HTML 통합 시:
- [ ] `xmlns:th` 네임스페이스 추가
- [ ] 동적 데이터 부분을 `th:text` 또는 `/*[[${...}]]*/`로 변환
- [ ] 컨트롤러에서 `Model`에 데이터 추가
- [ ] 템플릿 파일을 `src/main/resources/templates/`에 배치
- [ ] 개발 환경에서 테스트 (캐시 비활성화)
- [ ] 프로덕션 환경에서 캐시 활성화

---

**작성일**: 2024-11-12  
**버전**: 1.0  
**템플릿 엔진**: Thymeleaf 3.0.15  
**Spring Framework**: 4.3.30

