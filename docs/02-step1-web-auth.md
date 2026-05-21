# 02. 1단계 — 웹에서 로그인하기 (인증)

> 이 단계의 핵심 질문: **웹 로그인 상태를 어떻게 유지하고, 그 정보를 애플리케이션 코드에 어떻게 전달할 것인가?**
>
>
> 이 문서는 **3번 대화 (Sonnet)** 에서 채워진다.
>

---

## 1. 요구사항 요약 (가이드 발췌)

### 목표

- 사용자가 웹에서 로그인할 수 있다.
- 로그인한 사용자를 기준으로 예약을 생성한다.
- 로그인한 사용자를 기준으로 예약을 조회한다.
- 로그인하지 않은 사용자는 인증이 필요한 기능을 사용할 수 없다.
- 인증 로직이 컨트롤러마다 반복되지 않게 한다.

### 요구사항

- **로그인:** 사용자는 로그인할 수 있다 / 로그인 성공 후 같은 사용자를 식별할 수 있다 / 실패 시 적절한 응답.
- **예약 생성:** 로그인한 사용자 기준으로 예약 생성. 요청 이름이 아닌 로그인 사용자 기준.
- **예약 조회:** 로그인한 사용자가 자신의 예약을 조회. 비로그인은 조회 불가.
- **인증 공통 처리:** 로그인 여부 확인 로직을 컨트롤러마다 반복 ✗. 인증 필요/불필요 API 구분. 인증 실패 시 일관된 응답.

### 구현 조건

- Spring Security 사용 금지.
- 세션 또는 직접 구현한 인증 흐름 가능.
- 공통 처리는 Interceptor, ArgumentResolver 등 적절한 도구로 분리.
- 컨트롤러는 가능한 한 로그인 사용자 정보를 직접 꺼내지 않는다.

### 완료 기준

- [ ]  로그인한 사용자를 기준으로 예약을 생성한다.
- [ ]  로그인하지 않은 요청은 인증이 필요한 기능을 사용할 수 없다.
- [ ]  인증 로직이 여러 컨트롤러에 중복되어 있지 않다.
- [ ]  인증 실패 상황을 테스트했다.

---

## 2. 시작 전 코드 상태

- **이어받는 곳:** 미션2까지의 코드 (이름 기반 식별)
- **이 단계에서 손댄 파일들:**
    - 신규: `Member`, `MemberRepository`, `JdbcMemberRepository`, `MemberService`, `UnauthorizedException`, `@LoginMember`, `LoginCheckInterceptor`, `LoginMemberArgumentResolver`, `WebMvcConfig`, `LoginController`, `LoginRequest/Response`, `AdminReservationRequest`
    - 수정: `Reservation`(name→Member), `JdbcReservationRepository`, `ReservationRepository`, `ReservationCreateCommand`, `ReservationUpdateCommand`, `ReservationResult`, `ReservationRequest`, `ReservationUpdateRequest`, `ReservationResponse`, `ReservationService`, `UserReservationController`, `AdminReservationController`, `schema.sql`, `data.sql`
- **깨진 테스트와 처리:**
    - `MyReservationStepTest` → 전면 재작성 (로그인 쿠키 기반)
    - `ReservationPolicyStepTest` → `ReservationCreateCommand` 시그니처 수정,InputValidationPolicy 일부 교체
    - `ReservationTest` → `Reservation.name` → `Member` 교체
    - `MissionStepTest` → 예약 생성 body `name` → `memberId`
    - `UserReservationStepTest` → 예약 생성 body 수정, 로그인 쿠키 추가
    - `ErrorResponseStepTest` → `AuthError` 중첩 클래스( 인증 실패 케이스) 추가, 예약 생성 body 수정
    - `PopularThemeStepTest` → `insertReservation` 시그니처 수정, member 삽입 추가
    - `IntegrationTest` → `cleanDatabase()`에 member 테이블 추가

---

### 📋 01에서 넘어온 미결 질문들 (이 단계에서 답해야 할 것)

1. **세션으로 시작은 합의됐지만** — 실제 코드를 짜며 검증. → **3.1 슬롯**
2. **Interceptor + ArgumentResolver 조합** vs 단일 도구 — 학습 효율과 단순성 사이의 실측. → **3.3 슬롯**
3. **인증 실패 처리** — Interceptor의 `setStatus` 함정 회피, 예외 던지기로 통일. → **3.5 슬롯**
4. **`Reservation.name` 필드** — 즉시 제거? 점진 마이그레이션? → **4번 섹션**
5. **`MyReservationStepTest`의 `?name=`** — 재작성 방향. → **6번 섹션**

## 2.5 생각해 볼 점 (가이드 원문)

- [x]  로그인 상태는 어디에 저장할 것인가? → 3.1
- [x]  요청마다 사용자를 어떻게 확인할 것인가? → 3.3, 3.4
- [x]  컨트롤러가 세션이나 요청 헤더를 직접 다루는 게 적절한가? → 3.4
- [x]  인증 실패와 잘못된 요청은 어떻게 구분할 것인가? → 3.5
- [x]  기존 이름 입력 필드는 어디까지 제거하거나 대체할 수 있는가? → 4번

## 3. 가이드가 명시한 선택 카테고리

### 3.1 로그인 상태 유지 방식

**가이드 질문:** 세션을 사용할 것인가, 직접 만든 토큰 흐름을 사용할 것인가?

> 📋 **01에서 잠정 결정:** **세션으로 시작.** 사전학습 코드 샘플이 세션 기반이고, 미션이 "웹 → 모바일 확장"으로 트레이드오프를 경험하게 설계되어 있어서. 1단계는 웹만이라 세션의 자연스러움(쿠키 자동)이 살아남고, 2단계 모바일 확장에서 토큰을 덧대며 두 방식을 공존시키는 학습 경로를 택함.
**1단계 구현하며 검증할 것:** 세션 흐름이 내 `GlobalExceptionHandler` 구조와 잘 어울리는가? 코드 실제로 짜보니 토큰이 더 단순했을 가능성은 없는가?
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. `HttpSession` | 서버 메모리에 로그인 상태 저장, 쿠키로 세션 ID 주고받음 | 브라우저가 쿠키 자동 처리, Spring이 기본 지원 | 서버 상태 유지, 모바일 확장 시 쿠키 핸들링 필요 | **선택** — 1단계 웹 환경에 자연스럽고, 2단계에서 토큰 대비를 의도적으로 체험 |
| B. 직접 만든 토큰 (서명/저장소 직접) | 로그인 시 토큰 발급, 헤더나 쿠키로 전달, 서버 무상태 | stateless, 모바일 확장 용이 | 초기 구현 비용, 무효화 전략 별도 필요 | 2단계에서 도입 예정 |
| C. (그 외 — 떠오르면 추가) |  |  |  |  |

**선택:**

**선택 이유:1단계는 웹 브라우저 환경만이라 세션의 자연스러움(쿠키 자동)이 살아남. 사전학습 코드 샘플과 일치. 2단계에서 모바일 확장 시 세션의 한계를 직접 체험해 트레이드오프를 비교하는 것이 이 미션의 설계 의도.**

**받아들인 트레이드오프:서버가 상태를 기억해야 함. 모바일 앱이 쿠키를 자동 처리하지 않아 2단계에서 마찰 예상 — 이것이 의도된 학습.**

**다시 결정할 조건: 서버가 수평 확장되어 여러 인스턴스가 뜰 때. 또는 2단계에서 모바일 쿠키 처리가 너무 어색할 때.**

→ 관련 ADR: `decisions/001-session-vs-token.md`

---

### 3.2 인증이 필요한 API 구분 방식

**가이드 질문:** 경로 패턴으로 구분할 것인가, 어노테이션이나 별도 정책으로 표현할 것인가?

> 📋 **01에서 도출한 원칙:** **Secure by default / Fail-safe default — 깜빡했을 때 뚫리는 게 아니라 막히는 방향으로 설계.** 그래서 블랙리스트(`addPathPatterns`로 막을 것만 지정)보다는 화이트리스트(전부 막고 예외만 열기) 또는 어노테이션 기반(인증 요구사항을 API 정의 옆에)이 우위.
**잠정 방향:** 어노테이션 기반(예: `@LoginRequired`, 또는 `@LoginMember` 파라미터 존재 자체를 마크로 사용) 검토. 응집도(같이 바뀌어야 하는 것을 가까이) 원칙과도 맞음.
**1단계 구현하며 검증할 것:** 어노테이션 방식이 5~6개 API 규모에서도 과한 설계는 아닌가? 단순 경로 패턴으로도 충분히 깔끔한가?
>

**후보**

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 경로 패턴 (`addPathPatterns`) | Interceptor 등록 시 문자열로 명시 | 한눈에 전체 규칙 보임 | Fail-open (새 API 추가 시 깜빡하면 뚫림), API와 인증 설정이 분리 | 부분 보조 역할 |
| B. `@LoginMember` 파라미터 존재 = 인증 필요 | `HandlerMethod` 파라미터 검사 | API 정의 바로 옆에 인증 요구사항 (응집도 ↑), 잊으면 2차에서 잡힘 | Interceptor가 리플렉션으로 파라미터 검사 필요 | **선택** |
| C. 별도 `@LoginRequired` 어노테이션 | 메서드/클래스에 표시 | 명시적, 파라미터 타입과 무관 | 빠뜨리면 뚫림 (B와 같은 한계) | B보다 추가 어노테이션이 필요해 채택 안 함 |

**선택:** B (`@LoginMember` 파라미터 존재 = 인증 필요)

**선택 이유:** "인증이 필요한 API"라는 사실이 API를 정의하는 컨트롤러 메서드 시그니처에 바로 담긴다. 응집도 원칙에 맞음. 별도 어노테이션(`@LoginRequired`)을 추가하지 않아도 `@LoginMember Member member` 파라미터 자체가 "이 API는 로그인이 필요하다"는 선언이 된다.

**받아들인 트레이드오프:** `@LoginMember` 파라미터를 빠뜨리면 인증 없이 통과 (Fail-open). 단, 2차 안전망(ArgumentResolver)이 값을 채울 때 세션을 검사하므로 NPE 500이 아닌 401이 나온다.

**다시 결정할 조건:** `@LoginMember` 파라미터 없이 인증이 필요한 API가 생기면 (e.g., 응답 본문에 사용자 정보가 필요 없는 단순 권한 체크). 그 때는 C로 보완.

---

### 3.3 인증 공통 처리 위치

**가이드 질문:** Interceptor에서 막을 것인가, ArgumentResolver에서 꺼낼 것인가, 둘을 함께 쓸 것인가?

> 📋 **01에서 잠정 결정:** **C (둘 다 처리) — 단, D의 정신을 섞어서.**
>
> - Interceptor: **1차 방어**, 메인. 인증 필요 경로에서 비로그인을 차단.
> - ArgumentResolver: **안전망**, 보조. Interceptor가 빠뜨린 경로에서 `UnauthorizedException`을 던져 NPE 500 대신 깔끔한 401을 보장.
> - (선택) 검사 로직 중복이 거슬리면 `AuthService.getLoginMemberId(request)` 같은 공통 메서드로 빼고 둘 다 호출 → 코드 중복 제거 + 검사 시점 이중화 유지.
    > **근거:** 둘은 경쟁이 아니라 분업. Interceptor 설정은 사람이 관리하는 문자열 목록이라 언젠가 깨진다. 그 가정 위에서 다층 방어(defense in depth) 원칙.
    > **1단계 구현하며 검증할 것:** AuthService 추출까지 갈지, 우선은 C만 적용할지. 두 곳에 같은 코드가 진짜 거슬리는 수준인지 직접 보고 결정.

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. Interceptor만 | 401 차단, 컨트롤러는 세션 직접 조회 | 단순 | 컨트롤러가 세션 의존, 인증 방식 바꾸면 컨트롤러도 수정 | 제외 |
| B. ArgumentResolver만 | 차단도 ArgumentResolver에서 예외로 | 단순 | `@LoginMember` 없는 API는 인증 안 됨, 차단 시점 늦음 | 제외 |
| C. Interceptor + ArgumentResolver 조합 | Interceptor: 1차 방어, ArgumentResolver: 안전망 + 주입 | 다층 방어, 컨트롤러 세션 의존 없음 | 같은 세션 검사 코드가 두 곳 | **선택** |
| D. Filter | Servlet 레벨 처리 | Spring MVC 바깥에서 처리 가능 | `HandlerMethod` 접근 불가, 스프링 DI 연동 복잡 | 제외 |

**선택:** C (Interceptor + ArgumentResolver 조합)

**선택 이유:** Interceptor는 `HandlerMethod`를 통해 `@LoginMember` 파라미터 존재를 확인하고 1차 차단. ArgumentResolver는 통과한 요청에서 세션을 다시 검사해 값을 주입 (2차 안전망). Interceptor 설정에서 경로를 빠뜨린 경우에도 NPE 500이 아닌 깔끔한 401을 보장. 둘은 경쟁 관계가 아니라 분업 관계.

**받아들인 트레이드오프:** 세션 검사 코드가 두 곳에 있음. 단, 인증 검사는 가볍고(세션 조회 한 번) 이중화의 안전 효과가 훨씬 큼. 01에서 언급한 D의 정신(`AuthService.getLoginMemberId(request)`)은 두 곳 검사 코드가 진짜 거슬리는 수준이 될 때 도입 고려.

**다시 결정할 조건:** 인증 검사 로직이 복잡해져 두 곳의 중복이 실질적 유지보수 부담이 될 때 → `AuthService` 추출로 리팩터.

→ 관련 ADR: `decisions/002-interceptor-vs-resolver.md`

---

### 3.4 컨트롤러에 로그인 사용자 정보 전달 방식

**가이드 질문:** 세션을 직접 조회할 것인가, 별도 `LoginMember` 같은 객체를 받을 것인가?

> 📋 **3.3 결정과 묶임:** 3.3에서 ArgumentResolver를 쓰기로 한 순간 **B (`@LoginMember Member member`) 방향이 자연스럽게 따라옴.** 컨트롤러를 인증 저장 방식(세션/토큰)으로부터 격리하는 것이 2단계 모바일 확장의 핵심 자산.
**남은 결정:** B vs C — 컨트롤러 파라미터 타입을 도메인 `Member` 그대로 받을지, `LoginMember`라는 인증 컨텍스트 전용 값 객체를 만들지. 2단계에서 토큰 페이로드로 들어가는 정보 범위와 관련됨.
**1단계 구현하며 검증할 것:** 컨트롤러가 `member.getId()` 외에 무엇을 더 쓰는가? 거의 ID만 쓴다면 별도 값 객체(C)가 과한 추상화일 수 있음.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. `HttpSession`을 컨트롤러 파라미터로 | `session.getAttribute(...)` 직접 호출 | 단순 | 컨트롤러가 세션에 강결합, 토큰으로 바꾸면 컨트롤러도 수정 | 제외 |
| B. `@LoginMember Member member` | ArgumentResolver가 도메인 `Member` 주입 | 컨트롤러가 인증 저장 방식에 무관, 2단계 모바일 확장 시 컨트롤러 무수정 | Member 전체를 넘기므로 불필요한 필드까지 포함 | **선택** |
| C. `LoginMember`라는 별도 값 객체 | 인증 컨텍스트 전용 (id, email 등) | 도메인 `Member`와 인증 컨텍스트 분리 명확 | 추가 타입 관리 비용 | 컨트롤러가 `member.getId()` 외에 거의 사용 안 함 → 과한 추상화 |

**선택:** B (`@LoginMember Member member`)

**선택 이유:** 컨트롤러 코드를 실제로 짜보니 `member.getId()` 외에 사용하는 필드가 없었음. C의 별도 값 객체는 이 규모에서는 과한 추상화. 핵심 자산은 **컨트롤러가 세션/토큰 같은 인증 저장 방식으로부터 격리된**다는 점 — B로도 충분히 달성.

**받아들인 트레이드오프:** 컨트롤러에 도메인 `Member`가 노출됨. 2단계에서 토큰 페이로드로 들어갈 정보 범위가 달라지면 ArgumentResolver가 흡수해주므로 컨트롤러 수정은 불필요.

**다시 결정할 조건:** 컨트롤러가 `member.getEmail()`, `member.getName()` 등 여러 필드를 사용하게 되거나, 인증 컨텍스트와 도메인 `Member`의 생명주기가 달라질 때 C를 재검토.

---

### 3.5 인증 실패 응답 방식

**가이드 질문:** 어떤 상태 코드와 에러 메시지를 사용할 것인가?

> 📋 **01에서 발견한 함정:** 가이드 Interceptor 샘플은 `response.setStatus(401); return false;`로 끝남 → `@ExceptionHandler`를 거치지 않아 **응답 본문이 비어버림.** 내 기존 에러 응답 규칙(`{message}`)이 깨짐.
**잠정 결정:** **A (401 + `ErrorResponse { message }` — 기존 포맷 재사용).** Interceptor에서도 `setStatus + return false` 대신 **`UnauthorizedException`을 던져** `GlobalExceptionHandler`로 연결. 내 `RoomeScapeException` 체계와 자동 결합되어 추가 핸들러 작성 불필요.
**근거:** 기존 미션의 에러 응답 일관성 유지. `@ExceptionHandler(RoomeScapeException.class)`가 `e.getStatus()`로 자동 처리하므로, `UnauthorizedException extends RoomeScapeClientException`만 만들면 끝.
**1단계 구현하며 검증할 것:** Interceptor에서 예외를 던지면 `HandlerExceptionResolver`를 통해 정말로 `GlobalExceptionHandler`가 잡는지 실제 동작 확인.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 401 + `{"message": "..."}` (기존 포맷 재사용) | `UnauthorizedException` → `GlobalExceptionHandler` | 기존 에러 응답 규칙 일관성 유지 | 특별 없음 | **선택** |
| B. 401 + `WWW-Authenticate` 헤더 추가 | HTTP 표준 따르기 | RFC 7235 준수 | 웹 브라우저에서 기본 인증 팝업 뜨는 부작용 | 제외 |
| C. 비로그인은 redirect (302) | 전통 웹 방식 | 브라우저 사용성 좋음 | SPA/API 클라이언트와 맞지 않음 | 제외 |

**선택:** A (401 + `{"message": "..."}`)

**선택 이유:** 01에서 발견한 함정(`setStatus + return false` → 응답 본문 비어버림)을 피하기 위해 Interceptor에서도 `UnauthorizedException`을 던지는 방식을 채택. 기존 `RoomeScapeClientException → GlobalExceptionHandler` 체계에 자연스럽게 편입. `UnauthorizedException`만 추가하면 핸들러 수정 불필요.

**받아들인 트레이드오프:** `WWW-Authenticate` 헤더는 없어 엄격한 HTTP 표준 준수는 아님. 그러나 REST API 관점에서는 응답 본문에 메시지가 있는 401이 클라이언트 친화적.

**다시 결정할 조건:** 외부 API를 만들어 HTTP 표준 준수가 요구될 때 B로 전환.

---

## 4. 구현 중 새로 마주친 결정들

> 가이드 카테고리 밖에서, 구현하다 보면 추가로 결정해야 할 것들. 번호 매겨 누적.
>

### 4.1 `Reservation.name` 필드 즉시 제거 vs 점진 마이그레이션

- **후보:** A. 즉시 제거(name → Member 교체) / B. name 유지하며 member_id 추가
- **선택:** A (즉시 제거)
- **이유:** 어드민도 memberId 기반으로 통일하기로 결정했으므로, 이름 기반 식별의 필요성이 완전히 사라짐. 점진 마이그레이션은 두 방식이 공존하는 복잡성만 추가. 테스트가 컴파일 단계에서 깨지니 범위도 즉시 확인됨.
- **결과로 깨진 테스트:** `ReservationTest`, `ReservationPolicyStepTest`, `MyReservationStepTest`, `MissionStepTest`, `ErrorResponseStepTest`, `UserReservationStepTest`, `PopularThemeStepTest` — 모두 수정 완료.

### 4.2 `LoginMember` 별도 값 객체 vs 도메인 `Member` 직접 사용

- **후보:** `LoginMember(id, email)` 별도 값 객체 / 도메인 `Member` 그대로
- **선택:** 도메인 `Member` 그대로 (`@LoginMember Member member`)
- **이유:** 실제 컨트롤러 코드에서 `member.getId()` 외에 사용하는 필드 없음. 별도 타입이 2단계 토큰 확장에서 유리할 수 있지만, 그 시점에 ArgumentResolver만 수정하면 컨트롤러는 무수정. 지금 단계에서는 과한 추상화.
- **2단계 재검토 조건:** 토큰 페이로드에 id 외 다른 정보(role 등)가 필요해질 때.

### 4.3 AdminReservationController의 예약 생성 DTO 분리

- **후보:** A. `ReservationRequest` 그대로 (name을 memberId로 이름만 변경) / B. `AdminReservationRequest` 별도 DTO
- **선택:** B (`AdminReservationRequest` 별도 DTO)
- **이유:** 어드민은 `memberId`를 직접 지정하는 반면, 사용자는 `@LoginMember`에서 자동 주입. 동일 DTO를 공유하면 어드민 요청에 불필요한 제약(e.g., `@NotBlank name`)이 끼거나, 반대로 사용자 요청이 느슨해짐. 입구가 다른 흐름은 DTO도 분리하는 게 응집도상 맞음.

### 4.4 `LoginController.SESSION_KEY` 공유 방식

- **후보:** A. `LoginController`에 `static final` 상수 두고 Interceptor/ArgumentResolver가 참조 / B. 별도 상수 클래스
- **선택:** A
- **이유:** 현재 규모에서 단순한 게 낫다. `SESSION_KEY` 값을 변경할 때 한 곳만 수정하면 됨.

### 4.5 로그인 성공 응답 본문

- **후보:** A. 이름만 (`{ "name": "브라운" }`) / B. id + 이름 / C. 응답 본문 없음 (204)
- **선택:** A (이름만)
- **이유:** 클라이언트가 로그인 후 "브라운님 환영합니다" 메시지를 보여주기 위한 최소 정보. id는 세션에서 관리되므로 클라이언트가 직접 들고 다닐 필요 없음. 보안 관점에서 id를 노출하지 않는 것이 약간 유리.

### 4.6 `auth` 패키지 위치

- **후보:** A. `controller.auth` (Spring MVC 기술 클래스들과 같은 곳) / B. `auth` (최상위) / C. `config.auth`
- **선택:** B (`roomescape.auth` 최상위)
- **이유:** `LoginCheckInterceptor`, `LoginMemberArgumentResolver`, `@LoginMember`는 "인증 처리 인프라"라는 같은 관심사. 특정 컨트롤러와 같은 레벨에 있는 게 어색하고, `config`는 "설정"이지 "인증 로직"이 아님. `LoginController`는 컨트롤러가 맞으니 그대로 `controller` 패키지에 둠.

### 4.7 `data.sql` 회원 시드 데이터 처리

- **후보:** A. `data.sql`에 회원 시드 포함 / B. `data.sql`에서 제거, 각 테스트가 `@BeforeEach`에서 직접 삽입
- **선택:** B (제거)
- **이유:** `data.sql`이 테스트 컨텍스트 로드 시 실행되고, `IntegrationTest.cleanDatabase()`는 `member` 테이블을 비우지 않았어서 두 번째 테스트부터 `UNIQUE` 제약 위반이 발생했음. 근본 해결책은 `cleanDatabase()`에 `member` 테이블 추가 + `data.sql`에서 회원 시드 제거. 테스트는 필요한 회원을 직접 만들고 격리를 보장하는 게 더 명확.

---

## 5. 구현 메모 (시간순)

- ✅ `Member` 도메인 + `schema.sql` member 테이블 추가, reservation.name → member_id FK 교체
- ✅ `JdbcMemberRepository` 구현 — `findByEmail`, `findById` 두 메서드만 (현재 요구사항 최소)
- ✅ `UnauthorizedException` 추가 — `GlobalExceptionHandler` 수정 불필요 확인 (`RoomeScapeClientException` 상속으로 자동 처리)
- ✅ `MemberService.login()` — 이메일/비밀번호 검증, 실패 시 동일 메시지 (정보 노출 방지 — 이메일/비밀번호 구분 안 함)
- ✅ `LoginController` — 로그인 성공 시 세션 고정 공격 방어 코드 추가 (`session.invalidate()` 후 재발급)
- ✅ `@LoginMember` 어노테이션 + `LoginCheckInterceptor` — `HandlerMethod` 파라미터 검사로 어노테이션 기반 구분
- ✅ `LoginMemberArgumentResolver` — 세션에서 `memberId` 꺼낸 뒤 `MemberService.findById()`로 `Member` 재조회
- ✅ `WebMvcConfig` — Interceptor, ArgumentResolver 등록. `/admin/**`는 1단계에서 `excludePathPatterns`에 포함 (3단계에서 역할 기반 인가 추가 예정)
- ✅ `auth` 패키지 위치 논의 끝에 `roomescape.auth` 최상위로 결정
- ✅ `Reservation.name` → `Reservation.member` 교체 — `JdbcReservationRepository` member 테이블 JOIN 쿼리 재작성
- ✅ `ReservationService` — `findByName` → `findByMember`, `deleteByOwner(id, name)` → `deleteByOwner(id, memberId)`
- ✅ `UserReservationController` — `@RequestParam String name` 제거, `@LoginMember Member member` 주입
- ✅ Interceptor에서 예외 던지는 방식 검증 — `UnauthorizedException`이 `HandlerExceptionResolver`를 통해 `GlobalExceptionHandler`에 정상 도달 확인
- ⚠️ `data.sql`에 회원 시드 넣었다가 테스트 전체 UNIQUE 위반 — `IntegrationTest.cleanDatabase()`에 member 누락이 원인. `cleanDatabase()`에 member 추가 + `data.sql`에서 회원 시드 제거로 해결.
- ✅ 테스트 7개 파일 수정 + `LoginStepTest` 신규 추가

---

## 6. 테스트 변경 사항

| 테스트 파일 | 변경 유형 | 사유 |
| --- | --- | --- |
| `IntegrationTest` | 수정 | `cleanDatabase()`에 `member` 테이블 DELETE + ID 리셋 추가. 
누락 시 data.sql 시드와 충돌 |
| `ReservationTestHelper` | 수정 | `insertReservation(name→memberId)`, `insertMember()`, `login()` 헬퍼 추가 |
| `ReservationTest` | 수정 | `Reservation.name` → `Member` 교체. 
이름 관련 검증 테스트 → member null 검증으로 교체.
이름 길이/공백 검증 테스트 제거 (Member 도메인으로 이전) |
| `ReservationPolicyStepTest` | 수정 | `ReservationCreateCommand(name,...)` → `(memberId,...)`. `setUp`에 `insertMember()` 추가.
`InputValidationPolicy`: `이름_null_거부` 제거 → `memberId_null_거부`로 교체.
날짜/시간/테마 null 검증은 그대로 유지 |
| `MyReservationStepTest` | 전면 재작성 | `?name=` 쿼리 → 로그인 세션 쿠키 기반.
`setUp`에 `insertMember()` + `login()` 추가.
`name_누락 → 400` 제거 → `비로그인 → 401`로 대체.
`이미_지난_예약`, `시간_충돌`, `존재하지_않는_시간`, `새_시간이_과거` 케이스 쿠키 추가 후 유지 |
| `MissionStepTest` | 수정 | 예약 생성 body `name` → `memberId`. `setUp`에 `insertMember()` 추가 |
| `UserReservationStepTest` | 수정 | 예약 생성 body `name` 제거, 로그인 쿠키 추가.
응답 검증 `"name"` → `"memberName"`. `같은_시간_다른_테마는_각각_예약_가능` 테스트도 동일하게 수정 |
| `ErrorResponseStepTest` | 수정 + 추가 | 예약 생성 body 수정, 로그인 쿠키 추가.
`빈_이름`, `이름_30자_초과` 제거 (name 필드 삭제).
`AuthError` 중첩 클래스 추가 (비로그인 401, 잘못된 비밀번호 401, 존재하지 않는 이메일 401).
기존 `중복_예약`, `존재하지_않는_시간`, `존재하지_않는_테마`, `timeId_누락`, `themeId_누락`, `잘못된_JSON_날짜_형식` 복원 |
| `PopularThemeStepTest` | 수정 | `insertReservation(name→memberId)`.
`setUp`에 `insertMember()` 추가 |
| `LoginStepTest` | 신규 추가 | 로그인/로그아웃 흐름 통합 테스트.  성공(200+이름+쿠키), 잘못된 비밀번호(401), 존재하지 않는 이메일(401), 이메일 누락(400), 로그아웃 후 접근(401) 검증 |

---

## 7. 얻은 인사이트

1. **Interceptor에서 예외를 던지면 `GlobalExceptionHandler`가 잡는다.** `setStatus + return false`는 응답 본문이 비는 함정이고, `UnauthorizedException`을 throw하면 `DispatcherServlet`의 `HandlerExceptionResolver`가 받아 `@ExceptionHandler`로 연결된다. 사전학습에서 이론으로 알았던 것을 실제 동작으로 확인.
2. **`@LoginMember` 파라미터 존재가 곧 인증 요구 선언이다.** 별도 어노테이션 없이 파라미터 타입과 어노테이션의 공존으로 "이 API는 인증이 필요하다"는 사실을 API 정의 바로 옆에 표현할 수 있었다. 응집도(같이 바뀌어야 하는 것을 가까이) 원칙을 자연스럽게 충족.
3. **ArgumentResolver의 2차 방어 역할이 실제로 의미 있다.** `WebMvcConfig`에서 `/admin/**`를 `excludePathPatterns`에 넣었는데, 만약 어드민 컨트롤러에 `@LoginMember`를 붙였다면 Interceptor가 막지 않아도 ArgumentResolver가 세션을 검사해 NPE 대신 401을 냈을 것이다.
4. **`Reservation.name` 즉시 제거가 점진 마이그레이션보다 명확했다.** 두 방식이 공존하는 중간 상태는 테스트 의도를 흐리게 만든다. 컴파일 에러로 범위가 즉시 드러났고, 수정 후 테스트가 새 설계를 직접 문서화한다.
5. **세션 고정 공격 방어를 로그인 시점에 자연스럽게 넣을 수 있었다.** `session.invalidate()` 후 `getSession(true)` — 실무에서 Spring Security가 기본 처리해주는 것을 직접 짜보니 그 의미가 선명해졌다.
6. **`data.sql`과 `cleanDatabase()`의 관계를 놓쳤다.** `data.sql`은 테스트 컨텍스트 로드 시 실행되는데 `cleanDatabase()`가 `member`를 비우지 않아 두 번째 테스트부터 UNIQUE 위반이 발생했다. `IntegrationTest`에 새 테이블이 추가되면 `cleanDatabase()`도 반드시 함께 수정해야 한다는 교훈.
7. **세션에는 `memberId`만 저장하는 게 맞았다.** `Member` 객체 전체를 세션에 넣으면 직렬화 문제 + 회원 정보 변경 시 stale 데이터 문제가 생긴다. `memberId`만 저장하고 필요할 때마다 DB에서 조회하면 항상 최신 정보를 쓴다. 요청마다 DB 조회가 한 번 추가되는 비용은 현재 규모에서 무시 가능.

---

## 8. 평가 기준 충족 한 줄씩

1. **공통 처리 분리:** `LoginCheckInterceptor`(1차 차단)와 `LoginMemberArgumentResolver`(2차 안전망 + 주입)으로 인증 로직을 컨트롤러 밖에 완전히 분리. 컨트롤러는 `@LoginMember Member member`만 선언하면 끝. 세션/토큰 방식이 바뀌어도 컨트롤러는 무수정.
2. **다른 후보와 비교 설명:** 5개 슬롯 모두 후보 표 + 선택 이유 + 트레이드오프 기록. 특히 3.3(공통 처리 위치)에서 Interceptor만 / ArgumentResolver만 / 조합 / Filter 네 후보를 비교해 조합을 선택한 이유를 정리. 3.2에서 경로 패턴 vs 어노테이션 기반의 Fail-open 문제를 비교.
3. **트레이드오프 인식:** 세션의 Fail-open 특성, 어노테이션 기반 구분의 한계, 도메인 `Member` 노출 vs 별도 값 객체 추상화, 세션 검사 코드의 이중화 등 각 결정마다 받아들인 트레이드오프와 재검토 조건을 명시.

---

## 8.5 "생각해 볼 점"에 대한 1단계 최종 답변

### Q1. 로그인 상태는 어디에 저장할 것인가?

`HttpSession` — 서버 메모리에 `LOGIN_MEMBER_ID` 키로 `memberId(Long)`만 저장. 세션 ID는 `JSESSIONID` 쿠키로 브라우저가 자동 처리. `Member` 객체 전체 대신 ID만 저장해 세션 크기를 최소화하고, 실제 Member 정보는 요청마다 DB에서 조회해 항상 최신 상태를 보장.

### Q2. 요청마다 사용자를 어떻게 확인할 것인가?

`LoginCheckInterceptor`가 `@LoginMember` 파라미터 존재를 검사 → 세션에서 `memberId` 확인. 통과하면 `LoginMemberArgumentResolver`가 `memberId`로 `MemberService.findById()` 호출 → `Member` 객체 반환 → 컨트롤러 파라미터에 주입. 컨트롤러는 받은 `Member`를 사용하기만 하면 된다.

### Q3. 컨트롤러가 세션이나 요청 헤더를 직접 다루는 게 적절한가?

아니다. 컨트롤러가 `HttpSession`을 직접 다루면 인증 저장 방식이 바뀔 때(세션→토큰) 모든 컨트롤러를 수정해야 한다. `@LoginMember`로 추상화해 컨트롤러가 저장 방식에 무관하도록 했다. 2단계 토큰 전환 시 ArgumentResolver만 수정하면 컨트롤러는 무수정.

### Q4. 인증 실패와 잘못된 요청은 어떻게 구분할 것인가?

인증 실패(`UnauthorizedException`) → 401. 잘못된 요청(`InvalidCommandException`, `@Valid` 실패) → 400. 리소스 없음(`ResourceNotFoundException`) → 404. 각각 별도 예외 클래스로 표현하고 `GlobalExceptionHandler`가 `e.getStatus()`로 자동 매핑. "이메일이 없습니다" / "비밀번호가 틀렸습니다"를 구분하지 않고 동일 메시지를 반환해 이메일 열거 공격도 방어.

### Q5. 기존 이름 입력 필드는 어디까지 제거하거나 대체할 수 있는가?

`Reservation.name` 필드를 완전히 제거하고 `Member` 참조로 교체. `ReservationCreateCommand`, `ReservationUpdateCommand`의 `name` 파라미터 제거. 컨트롤러 DTO(`ReservationRequest`, `ReservationUpdateRequest`)의 `name` 필드 제거. `?name=` 쿼리 파라미터 완전 제거. `schema.sql`의 `name VARCHAR` 컬럼을 `member_id BIGINT FK`로 교체. 모든 이름 기반 식별이 `memberId` 기반으로 교체됨.

---

## 9. 다음 단계 (모바일 인증)로 넘기는 질문

1. 내가 선택한 세션 방식은 모바일 앱에서도 자연스러운가? 브라우저가 없는 환경에서 쿠키를 직접 처리해야 할 때 얼마나 어색한지 — 토큰으로 전환할지, 공존시킬지는 2단계에서 직접 체험 후 결정.
2. **ArgumentResolver의 DB 조회 비용** — 인증이 필요한 모든 요청마다 `MemberService.findById()`가 DB를 한 번 더 조회함. 현재 규모에서는 무시 가능하지만, 트래픽이 커지면 세션에 `Member` 객체를 캐싱하거나 토큰 클레임에 필요 정보를 넣는 방향 검토 필요.
3. **`/admin/**` 인증/인가 처리** — 현재 Interceptor에서 `/admin/**`를 `excludePathPatterns`에 넣어 인증 없이 통과 가능. 3단계에서 역할 기반 인가를 추가할 때 이 경로에 대한 인증도 같이 처리해야 함.
4. **비밀번호 평문 저장** — 미션 범위 내 의도적 단순화. 실제로는 BCrypt 등 해시 필요.
5. 인증 공통 처리(Interceptor/ArgumentResolver)는 모바일 요청에 그대로 적용 가능한가? 현재 구조에서 인증 정보를 꺼내는 부분(`session.getAttribute`)만 바꾸면 컨트롤러는 무수정인가?

---

## 10. PR 본문 조각

```
## 1단계 — 웹에서 로그인하기 (인증)

### 선택한 방식
- 로그인 상태 유지: HttpSession (쿠키 기반)
- 인증 공통 처리: LoginCheckInterceptor (1차 차단) + LoginMemberArgumentResolver (2차 안전망 + 주입)
- 인증 필요 API 구분: @LoginMember 파라미터 존재 = 인증 필요
- 인증 실패 응답: UnauthorizedException → 401 + {"message": "..."} (기존 에러 포맷 통일)

### 다른 후보들과 비교한 이유
- 세션 대신 토큰: 2단계 모바일 확장에서 세션의 한계를 직접 체험하기 위해 1단계는 세션으로 시작
- Interceptor만 / ArgumentResolver만: 각각 "컨트롤러 세션 의존" 또는 "차단 시점 늦음"이라는 약점. 조합으로 역할 분담
- @LoginRequired 별도 어노테이션: @LoginMember 파라미터 존재 자체가 인증 선언이 되어 추가 어노테이션 불필요

### 불편했던 점 / 트레이드오프
- @LoginMember 파라미터를 빠뜨리면 Interceptor를 통과함 (Fail-open). ArgumentResolver가 2차로 잡아주지만, 설계상 완전히 막지는 못함
- 매 요청마다 ArgumentResolver에서 DB 조회 1회 추가 (memberId → Member)
- /admin/** 경로가 현재 인증 없이 통과됨 — 3단계에서 역할 기반 인가 추가 예정
```
