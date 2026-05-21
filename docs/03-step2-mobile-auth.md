# 03. 2단계 — 모바일 앱 요청에서도 로그인 유지하기

> 이 단계의 핵심 질문: **웹에서 만든 인증 흐름을 모바일에 그대로 확장할 것인가, 모바일에 맞게 다른 방식을 선택할 것인가?**
>
>
> 이 문서는 **4번 대화 (Sonnet)** 에서 채워진다.
>

---

## 1. 요구사항 요약 (가이드 발췌)

### 목표

- 모바일 앱 요청에서도 로그인한 사용자를 식별한다.
- 모바일 앱 사용자가 인증이 필요한 API를 사용할 수 있게 한다.
- 인증 정보가 없거나 유효하지 않은 요청을 거부한다.
- 웹 인증 구현과 모바일 인증 구현의 **공통점과 차이점**을 설명한다.
- 세션과 토큰 중 선택한 방식의 이유를 설명한다.

### 요구사항

- **모바일 로그인:** 로그인 가능 / 이후 요청에서 사용할 인증 정보 받음 / 매 요청에서 사용자 식별 가능한 형태.
- **모바일 인증 요청:** 인증 정보 함께 전달 / 서버는 검증 / 유효하면 처리, 아니면 거부.
- **웹 인증과의 관계:** 공통점·차이점 설명 가능 / 중복된 인증 로직 줄이기.

### 구현 조건

- 세션과 토큰 중 하나 선택 또는 조합 가능.
- 선택 외 다른 후보도 검토.
- 인증 정보 전달 위치를 명확히 정한다.
- 컨트롤러는 웹/모바일에 따라 인증 로직이 과도하게 갈라지지 않도록 한다.

### 완료 기준

- [ ]  모바일 앱 요청에서도 로그인한 사용자를 식별할 수 있다.
- [ ]  인증 정보가 없거나 유효하지 않은 모바일 요청을 거부한다.
- [ ]  선택한 인증 방식과 다른 후보를 비교해 설명했다.
- [ ]  웹 인증 흐름과 모바일 인증 흐름의 공통점과 차이점을 설명했다.

---

## 2. 1단계에서 이어진 상태

- **1단계에서 선택하고 확정된 방식:** **`HttpSession` + 쿠키.웹에서 자연스럽고, 사전학습 코드 샘플과 일치.** `GlobalExceptionHandler` 구조와 잘 어울렸고, Interceptor에서 예외를 던지면
  기존 체계가 그대로 처리해줌을 실제로 확인. 세션 고정 공격 방어(`invalidate()` 후 재발급)도 구현 완료.
- **모바일에 그대로 적용했을 때 예상되는 마찰점 (1단계 구현에서 확인):**
    - 가이드 자료 1의 시그널: *"모바일 앱에서는 `Authorization` 헤더 기반 토큰 방식이 더 자주 선택된다."*
    - 모바일 앱은 쿠키 자동 첨부 혜택이 없음. `Set-Cookie` 저장과 `Cookie` 헤더 첨부를 앱이 직접 처리해야 해 어색함.
    - 인증 정보를 꺼내는 층은 `LoginMemberArgumentResolver`의 `session.getAttribute(LoginController.SESSION_KEY)` 한 줄. 모바일 확장 시 **이 한 줄만 바뀌면 컨트롤러는 무수정** — `@LoginMember Member member` 파라미터 덕분.
- **이 단계의 핵심 학습 지점:** 1단계 세션 선택의 **"청구서"가 이 단계.** 모바일 확장 작업을 하며 두 방식의 트레이드오프를 직접 비교하는 것이 의도된 학습.
- **이 단계에서 실제로 손댄 파일들:**
    - 신규: `JwtTokenProvider`, `AuthenticationExtractor` 인터페이스, `SessionAuthenticationExtractor`, `TokenAuthenticationExtractor`, `MobileLoginController`, `MobileLoginResponse`
    - 수정: `LoginCheckInterceptor`, `LoginMemberArgumentResolver`, `WebMvcConfig`(추출기 주입 + `/api/login` 경로 추가), `application.properties`(시크릿 키 추가)
- **예상과 달라진 것:**
    - 예상: `LoginResponse`에 토큰 필드 추가 → **실제**: 웹/모바일 응답 형태 자체가 달라서 `MobileLoginResponse` 별도 DTO로 분리 + `/api/login` 엔드포인트 분리.
    - 예상: `LoginStepTest` 깨짐 → **실제**: 웹 `/login` 응답 형태가 그대로라 깨지지 않음.
- **깨질 것으로 예상했지만 깨지지 않은 테스트:** `LoginStepTest` — 웹 로그인 응답은 `{ "name": "..." }` 그대로 유지됨.

---

## 2.5 생각해 볼 점 (가이드 원문)

- [x]  **브라우저와 모바일 앱은 인증 정보를 어떻게 다르게 다루는가?** → 3.2
- [x]  **세션을 모바일 앱에서 그대로 사용하면 어떤 점이 편하고 어떤 점이 불편한가?** → 3.1
- [x]  **토큰을 사용하면 어떤 점이 편하고 어떤 점이 불편한가?** → 3.1 + 3.4
- [x]  **로그아웃과 만료는 어떻게 처리할 것인가?** → 3.4
- [x]  **서버가 상태를 기억하는 방식과 기억하지 않는 방식은 어떤 차이가 있는가?** → 3.1 + 3.3

---

## 3. 가이드가 명시한 선택 카테고리

### 3.1 모바일 앱 요청의 인증 방식

**가이드 질문:** 세션 유지 / 토큰 발급 / 웹과 모바일을 다르게 처리?

> 📋 **01에서 잠정 결정:** **D (웹은 세션, 모바일은 토큰 — 공존).** 통일(웹도 토큰으로)을 택할 수도 있었지만, 그러면 미션 2단계의 "두 방식 비교"라는 학습 목표가 사라짐. 공존을 통해 트레이드오프를 직접 비교하는 길을 택함.
**PR에 남길 판단(01 인용):** "웹은 세션, 모바일은 토큰으로 공존시켰다. 통일할 수도 있었지만 두 방식의 트레이드오프를 직접 비교하려고 공존을 택했다."
**2단계 구현하며 검증할 것:** 공존이 만드는 복잡도가 학습 가치에 비례하는가? 만약 구현이 너무 무거워지면 통일 쪽도 재고.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 세션 그대로 (쿠키 기반) | 모바일도 쿠키 핸들링 | 1단계 코드 변경 없음 | 앱이 쿠키를 직접 관리해야 해 어색함. 쿠키 자동 처리는 브라우저 전용 혜택 | 낮음 — 모바일에 쿠키 강제는 부자연스러움 |
| B. JWT 토큰 발급 (stateless) | 서명만으로 검증, 서버 상태 없음 | 서버 확장 용이, 모바일 표준 패턴 | 발급된 토큰 즉시 무효화 불가, 로그아웃 처리 복잡 | 높음 — 모바일에 자연스러운 방식 |
| C. 직접 만든 Opaque 토큰 (서버 저장소) | 무작위 문자열 + 서버 DB 조회 | 즉시 무효화 가능 | 매 요청마다 DB 조회 발생, 세션과 큰 차이 없음 | 중간 — 무효화 필요 시 고려 |
| D. 웹은 세션, 모바일만 토큰 (혼합) | 클라이언트별 분리 | 두 방식의 트레이드오프를 직접 비교 가능 | 두 방식을 동시에 지원하는 복잡도 | 높음 — 학습 목표와 일치 |

**선택:** D (웹은 세션, 모바일은 JWT 토큰 - 공존)

**선택 이유:** 웹을 토큰으로 통일하면 2단계의 학습 목표인 "두 방식 비교"가 사라진다. 세션과 토큰의 트레이드오프를 코드로 직접 체험하려면 둘이 공존해야 한다. 또한 `AuthenticationExtractor` 추상화 덕분에 공존이 만드는 복잡도가 Interceptor·ArgumentResolver 내부에 완전히 격리되어, 컨트롤러는 어느 방식인지 모른 채 동일하게 `@LoginMember Member member`를 받는다.

**받아들인 트레이드오프:** 두 방식이 공존하는 만큼 인증 인프라 코드가 늘어난다(`AuthenticationExtractor` 인터페이스 + 구현체 2개). 단일 방식(토큰 통일)보다 파일 수가 많다. 단, 각 파일의 책임이 명확해서 유지보수 부담은 크지 않다.

**다시 결정할 조건:** 웹도 SPA로 전환되어 쿠키 기반 세션이 불필요해지면, 전체를 JWT로 통일하는 쪽을 재검토한다.

→ 관련 ADR: `decisions/001-session-vs-token.md` (1단계와 통합 갱신 가능)

---

### 3.2 인증 정보 전달 위치

**가이드 질문:** 쿠키 / Authorization 헤더 / 그 외 헤더?

> 📋 **01에서의 정리:** 쿠키 = 브라우저가 자동으로 챙기는 길, 헤더 = 클라이언트가 직접 챙기는 길. 모바일은 자동 혜택이 없으므로 헤더가 자연스러움.
**잠정 방향:** **웹은 쿠키 유지 (1단계 그대로), 모바일은 `Authorization: Bearer <token>`.** 3.1의 공존 결정과 자연스럽게 묶임.
**2단계 구현하며 검증할 것:** 두 길을 동시에 지원하려면 추출 코드가 어떻게 통합되는가 → 3.3과 직결.
>

**후보**

**후보**

| 후보 | 형식 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 쿠키 (`Set-Cookie`) | 브라우저 자동 처리 | 추가 코드 없음 | 모바일 앱이 쿠키를 직접 저장·첨부해야 해 어색함 | 웹 ○ / 모바일 ✗ |
| B. `Authorization: Bearer <token>` | HTTP 표준 헤더 | 모바일 표준 패턴, 플랫폼 무관 | 클라이언트가 토큰을 직접 저장·관리해야 함 | 모바일 ◎ |
| C. 커스텀 헤더 (`X-Auth-Token` 등) | 임의 헤더 | 단순 명시 | 비표준, 일부 프록시/CORS 설정에서 문제 가능 | 낮음 |
| D. 쿠키 + 헤더 동시 지원 | 클라이언트별 자유 | 유연 | 추출 로직이 양쪽 모두 필요해 복잡 | 불필요한 복잡도 |

**선택:** 웹은 A (쿠키), 모바일은 B (`Authorization: Bearer`)

**선택 이유:** 브라우저는 쿠키를 자동으로 붙여줘서 개발자가 신경 쓸 게 없다. 반대로 모바일 앱은 쿠키 자동 처리 혜택이 없고, `Authorization` 헤더가 HTTP 표준이자 모바일 SDK들이 당연하게 지원하는 패턴이다. 각 클라이언트에 자연스러운 방식을 선택했다.

**받아들인 트레이드오프:** `TokenAuthenticationExtractor`의 `supports()` 조건이 `"Authorization: Bearer "` 형식에 강하게 결합된다. 모바일 앱이 다른 헤더 형식으로 바꾸면 추출기를 수정해야 한다.

**다시 결정할 조건:** 모바일 앱이 커스텀 헤더를 요구하는 레거시 명세를 따라야 할 때.

---

### 3.3 웹/모바일 인증 흐름의 통합 방식

**가이드 질문:** 같은 검증 로직을 재사용할 것인가, 클라이언트별 어댑터를 둘 것인가?

> 📋 **01에서 잠정 결정:** **A (`AuthenticationExtractor` 인터페이스로 추상화 — 방식 2).** 코드를 세 층으로 나눠보면 모바일 확장에서 **바뀌는 건 [A] "인증 정보를 꺼내는 층" 하나뿐.** Interceptor / ArgumentResolver / 컨트롤러 / 인가 / 서비스는 손대지 않음.
**설계 골격 (01에서 도출):**
>
>
> ```java
> interface AuthenticationExtractor {
>     boolean supports(HttpServletRequest request);
>     Long extractMemberId(HttpServletRequest request);
> }
> class SessionAuthenticationExtractor implements AuthenticationExtractor { ... }
> class TokenAuthenticationExtractor implements AuthenticationExtractor { ... }
> ```
>
> Interceptor·ArgumentResolver는 추출기 목록에 위임만. 새 인증 방식이 늘면 구현체 하나만 추가(OCP).
> **비교 대상 (방식 1 — if 분기):** 동작은 하지만 인증 방식이 늘 때마다 같은 분기가 Interceptor·ArgumentResolver 두 곳에 중복.
> **2단계 구현하며 검증할 것:** 추상화 비용이 학습 가치에 비례하는가? 미션 가이드의 "트레이드오프를 자기 손끝으로 경험하라"는 의도를 살리는 방향인가.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. `AuthenticationExtractor` 인터페이스 + 구현체 분리 | 쿠키/헤더에서 인증 정보 꺼내는 부분만 다형성 적용 | 새 인증 방식 추가 시 구현체만 추가(OCP). Interceptor·ArgumentResolver·컨트롤러 무수정 | 인터페이스 + 구현체 2개 파일이 늘어남 | 높음 |
| B. 단일 메서드에 if-else로 처리 | `if (헤더 있으면) 토큰, else 세션
(단순)` | 파일 수 최소 | 인증 방식이 늘 때마다 Interceptor·ArgumentResolver 두 곳에 동일한 분기가 중복 | 낮음 — 확장 시 중복 발생 |
| C. 두 컨트롤러를 완전 분리 (`/web/...`, `/api/...`) | 웹용·모바일용 컨트롤러를 각각 만들어 흐름 자체를 분리 | 흐름이 독립적으로 명확 | 컨트롤러·서비스 로직 중복. 요구사항 변경 시 두 곳 모두 수정 필요 | 낮음 — 비즈니스 로직 중복 과다 |

**선택:** A (`AuthenticationExtractor` 인터페이스 + 구현체 2개)

**선택 이유:** 코드를 세 층으로 나누면 모바일 확장에서 바뀌는 건 [A] 인증 정보를 꺼내는 층 하나뿐이다. Interceptor·ArgumentResolver·컨트롤러·서비스는 손대지 않는다. B(if-else)로 가면 인증 방식이 하나 더 늘 때 Interceptor와 ArgumentResolver 두 곳에 같은 분기를 추가해야 한다. 실제로 구현하며 "추상화의 가치는 두 번째 확장이 생길 때 드러난다"는 것을 확인했다.

**받아들인 트레이드오프:** 인터페이스 하나에 구현체 두 개가 생겨 파일이 늘어난다. 미션 규모에서는 B(if-else)도 충분히 동작한다. 추상화의 학습 비용이 있지만, 미션의 "트레이드오프를 자기 손끝으로 경험하라"는 의도에 더 충실한 방향이라 택했다.

**다시 결정할 조건:** 인증 방식이 세션·토큰 두 가지로 영구 고정될 게 확실하다면 B(if-else)도 충분하다.

---

### 3.4 만료와 로그아웃 처리 방식

**가이드 질문:** 서버에서 상태 제거 / 토큰 만료에 맡김 / 별도 무효화 저장소?

> 📋 **01에서 미결.** 단, `01-concepts`에서 강하게 경고함: 토큰 방식에서 로그아웃·동시로그인 같은 요구가 들어오면 *"토큰은 stateless하다"는 가정이 깨진다.* 4단계 학습 지점과 직결.
**2단계에서 결정할 것:** 모바일(토큰)의 로그아웃 처리를 어디까지 구현할지 — best-effort(만료에 맡김) / 블랙리스트 / Refresh Token. 4단계 동시로그인 요구를 미리 의식하고 고름.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 서버 세션 무효화 | `session.invalidate()` | 즉시 로그아웃 가능. 이미 웹에서 구현 완료 | 세션 방식에만 해당. 토큰에는 적용 불가 | 웹 ○ / 모바일 ✗ |
| B. JWT 만료 시간만 짧게 | 토큰에 `exp` 클레임 설정(서버 상태 없음) | 서버 상태 없음. stateless 유지 | 만료 전 로그아웃 불가. 탈취된 토큰이 만료까지 유효 | 모바일 △ — 단순하나 즉시 무효화 불가 |
| C. 블랙리스트 저장소 | 로그아웃된 토큰 ID(`jti`)를 DB/캐시에 기록 | 즉시 무효화 가능 | 매 요청마다 블랙리스트 조회 발생. stateless 이점 사라짐 | 높음 — 단, 4단계에서 구현 |
| D. Refresh Token | 짧은 액세스 토큰 + 긴 리프레시 토큰 | 보안·UX 균형 | 구현 복잡도 높음. 미션 범위 초과 | 실무 ◎ / 미션 범위 초과 |

**선택:** 웹은 A (세션 무효화), 모바일은 B (만료 시간 — 단, **2단계에서는 만료 미설정**으로 단순화)

**선택 이유:** 2단계의 핵심은 "세션과 토큰 두 방식을 공존시키고 트레이드오프를 비교하는 것"이다. 만료·무효화 전략까지 완성하면 4단계 학습 주제가 사라진다. 지금은 best-effort(토큰 자체를 클라이언트가 버리면 로그아웃 효과)로 단순화하고, 동시로그인 방지 요구가 왔을 때 C(블랙리스트)나 D(Refresh Token)를 검토한다.

**받아들인 트레이드오프:** 모바일 토큰에 만료가 없으므로 탈취 시 영구 유효하다. 로그아웃도 클라이언트가 토큰을 삭제하는 것뿐이라 서버 측 강제 무효화가 안 된다. 이 취약점을 의식하고 4단계에서 정면으로 다루기로 했다.

**다시 결정할 조건:** 4단계 동시로그인 방지 요구, 또는 보안 감사에서 즉시 무효화가 필수로 지적될 때.

> 이 결정은 4단계(동시 로그인 방지)에 직접 영향을 준다. "stateless한 토큰을 무효화하려면 어디에 상태를 추가해야 하는가"가 4단계의 핵심 질문이 된다.
>

---

### 3.5 인증 실패 응답 방식

**가이드 질문:** 웹과 모바일에 같은 에러 응답 / 클라이언트별 응답 분리?

> 📋 **01에서 잠정 결정:** **A (동일하게 401 + `{message}`).** 1단계에서 `UnauthorizedException` → `GlobalExceptionHandler`로 잡는 인프라가 이미 있음. 추출기가 어느 방식으로도 사용자를 못 찾으면 동일하게 예외를 던지면 됨. 에러 응답이 일관되게 유지됨.
**단, 로그인 *성공* 응답은 갈라짐 (01 인사이트 5):** 웹은 `Set-Cookie`, 모바일은 응답 본문에 토큰. → 이건 3.2와 묶이며, "성공 응답을 클라이언트별로 다르게 줄지, 항상 토큰을 본문에 주고 웹은 쿠키도 같이 줄지" 결정해야 함.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 동일하게 401 + `{"message": "..."}` | 클라이언트 무관, 단일 응답 형식 | 기존 GlobalExceptionHandler 재사용. 추가 코드 없음 | 웹 UX 관점에서 redirect가 더 자연스러울 수 있음 | 높음 — REST API 기준에 충분 |
| B. 웹은 redirect, 모바일은 401 JSON | User-Agent나 Accept 헤더로 분기,UX 친화 | 웹 UX 친화적 | 서버가 클라이언트 종류를 판단해야 함. 분기 로직 추가 | 낮음 — 복잡도 대비 이득 적음 |
| C. `WWW-Authenticate` 헤더로 방식 알림 | HTTP 표준 헤더 추가 | 엄격한 HTTP 표준 준수 | 추가 구현 필요. 클라이언트가 이 헤더를 실제로 활용하지 않으면 의미 없음 | 낮음 — 미션 범위 초과 |

**선택:** A (동일하게 401 + `{"message": "..."}`)

**선택 이유:** `UnauthorizedException`을 던지면 `GlobalExceptionHandler`가 잡아 `{ "message": "로그인이 필요합니다." }`를 반환하는 인프라가 1단계에서 이미 완성됐다. `SessionAuthenticationExtractor`든 `TokenAuthenticationExtractor`든 인증 실패 시 동일하게 `UnauthorizedException`을 던지면 되므로 추가 코드가 전혀 없다. 에러 응답 일관성도 자연스럽게 유지된다.

**로그인 성공 응답은 갈라진다 (이건 의도된 분리):** 웹은 `Set-Cookie`로 세션, 모바일은 응답 본문에 토큰. 이 차이 때문에 `/login`과 `/api/login`을 분리하고, `LoginResponse`와 `MobileLoginResponse` DTO를 각각 만들었다.

**받아들인 트레이드오프:** 웹 브라우저가 401을 받으면 로그인 페이지로 redirect하는 처리를 클라이언트(JavaScript)가 담당해야 한다. 서버가 redirect를 내려주지 않는다.

**다시 결정할 조건:** 서버 사이드 렌더링(SSR) 구조로 바뀌어 서버가 redirect를 처리해야 할 때.

---

## 4. 구현 중 새로 마주친 결정들

### 4.1 모바일 로그인 엔드포인트 분리 — `/login` 공유 vs `/api/login` 별도

- **후보:** A. `/login` 하나에서 클라이언트 종류(헤더 등)에 따라 분기 / B. `/api/login` 별도 엔드포인트
- **선택:** B (`/api/login` 별도)
- **이유:** A는 컨트롤러가 클라이언트 종류를 알아야 한다. 웹은 `Set-Cookie`로 세션을 심고 이름만 반환, 모바일은 쿠키 없이 토큰을 본문에 담아 반환하는 방식이 응답 구조 자체가 달라서 진입점 분리가 명확하다. 각 엔드포인트가 자기 클라이언트에 최적화된 응답을 단순하게 반환한다.

### 4.2 모바일 로그인 응답 DTO 분리 — `LoginResponse` 재사용 vs `MobileLoginResponse` 별도

- **후보:** A. `LoginResponse`에 `token` 필드 추가 (nullable) / B. `MobileLoginResponse` 별도 DTO
- **선택:** B (`MobileLoginResponse` 별도)
- **이유:** A는 웹 응답에 `token: null`이 포함되거나, 조건부로 필드를 숨기는 직렬화 처리가 필요하다. 두 응답의 의미가 다르므로 DTO도 분리하는 것이 응집도상 맞다.

### 4.3 JWT 페이로드에 담을 정보 — `memberId`만 vs 추가 정보

- **후보:** A. `memberId`만 / B. `memberId` + `email` + `name` / C. `memberId` + `role`(권한)
- **선택:** A (`memberId`만)
- **이유:** 토큰에 많이 담을수록 DB 조회는 줄지만, 토큰 발급 후 DB 값이 바뀌어도 토큰 안의 값은 그대로다. 지금은 `memberId`만 담고 나머지는 매 요청마다 DB에서 최신값을 조회하는 방식이 단순하고 안전하다. 3단계에서 매장 권한 확인이 필요해지면 재검토.

### 4.4 JWT 시크릿 키 관리 — 하드코딩 vs 프로퍼티 vs 환경변수

- **후보:** A. 코드에 하드코딩 / B. `application.properties`에 명시 / C. 환경변수로 주입
- **선택:** B (`application.properties`)
- **이유:** A는 키가 코드 저장소에 그대로 노출되어 절대 불가. C가 실무 정답이지만 미션 환경에서 환경변수 관리는 과하다. B는 파일을 `.gitignore`에 추가하거나 테스트용·운영용 파일을 분리하는 방식으로 관리 가능. 단, 실제 배포 시에는 환경변수로 교체 필요.
- **부수 발견:** `application.properties`가 `main`과 `test` 두 곳에 있고, `test` 파일이 있으면 Spring Boot가 main 파일을 덮어쓰는 방식으로 동작한다. `security.jwt.secret-key`를 main에만 넣으면 테스트에서 `PlaceholderResolutionException`이 발생한다 — test 파일에도 추가해야 한다.

### 4.5 jjwt의 `Long` 역직렬화 문제

- **현상:** `claims.get(MEMBER_ID_CLAIM, Long.class)`가 `Cannot convert existing claim value of type 'class java.lang.Double' to desired type 'class java.lang.Long'` 예외를 던짐.
- **원인:** jjwt가 JSON 숫자를 파싱할 때 값의 크기에 따라 `Integer` 또는 `Double`로 역직렬화함. `Long.class`로 직접 꺼내면 타입 불일치 발생.
- **후보:** A. `Number`로 꺼내서 `.longValue()` / B. 토큰에 문자열로 저장하고 `Long.parseLong()`으로 꺼내기
- **선택:** A
- **이유:** `memberId`는 본질적으로 숫자. 페이로드에 `"memberId": 1`로 저장하는 게 의미상 맞고, `Number`로 받으면 `Integer`든 `Double`이든 흡수할 수 있다. B는 숫자를 문자열로 저장하는 타입 왜곡.

---

## 5. 구현 메모 (시간순)

- ✅ `JwtTokenProvider` 추가 — HS256 서명, `memberId` claim, `application.properties` 키 주입
- ✅ `AuthenticationExtractor` 인터페이스 정의 — `supports()` + `extractMemberId()`
- ✅ `SessionAuthenticationExtractor` — 1단계 세션 로직 이전. `getSession(false)`로 세션 존재 확인
- ✅ `TokenAuthenticationExtractor` — `Authorization: Bearer` 헤더 파싱 + `JwtTokenProvider` 위임
- ✅ `LoginCheckInterceptor` 수정 — `List<AuthenticationExtractor>` 주입받아 `anyMatch(supports)` 방식으로 위임
- ✅ `LoginMemberArgumentResolver` 수정 — `filter(supports).findFirst().map(extractMemberId)` 방식으로 위임
- ✅ `WebMvcConfig` 수정 — `List<AuthenticationExtractor>` 주입 추가, `/api/login` `excludePathPatterns` 추가
- ✅ `MobileLoginController` + `MobileLoginResponse` 추가 — `POST /api/login` → 토큰 발급
- ⚠️ `test/application.properties`에 `security.jwt.secret-key` 누락 → `PlaceholderResolutionException` 발생 → test 파일에도 추가
- ⚠️ `MobileAuthStepTest`에서 `helper.insertMember("브라운", "brown@email.com", "password")` 호출 — 헬퍼 시그니처는 `(email, password, name)` 순서라 DB에 이메일·비밀번호·이름이 뒤바뀌어 저장돼 로그인 401 → 순서 수정
- ⚠️ `GET /user/reservations/mine` 경로 없음 — 실제 엔드포인트는 `GET /user/reservations`. Spring이 `{id}="mine"`으로 해석하다 타입 변환 실패 → 400 → 경로 수정
- ⚠️ `세션과_토큰_공존` 테스트에서 `helper.login()`이 내부에서 `statusCode(200)`을 검증하다가 DB 상태 문제로 실패 → 인라인 RestAssured 호출로 교체
- ⚠️ `claims.get(MEMBER_ID_CLAIM, Long.class)` → `Double` 역직렬화 오류 → `Number.longValue()`로 수정
- ✅ `MobileAuthStepTest` 작성 — 로그인 성공/실패, 토큰 인증, 토큰 없음/위조/Bearer 누락, 세션+토큰 공존

---

## 6. 테스트 변경 사항

| 테스트 파일 | 변경 유형 | 사유 |
| --- | --- | --- |
| `MobileAuthStepTest` | 신규 추가 | 모바일 인증 시나리오 전체 검증 (로그인 성공/실패, Bearer 토큰 인증, 인증 실패, 공존) |
| `LoginStepTest` | 변경 없음 | 웹 `/login` 응답 형태 (`{ "name": "..." }`)가 그대로라 기존 테스트 유효 |

---

## 7. 얻은 인사이트

1. **"바뀌는 건 [A] 한 층뿐"이라는 설계 원칙이 실제 코드로 검증됐다.** `AuthenticationExtractor`를 도입하자 컨트롤러는 물론 `LoginCheckInterceptor`, `LoginMemberArgumentResolver`의 핵심 로직도 전혀 바뀌지 않았다. 세션이든 토큰이든 `extractMemberId()`가 `Long`을 반환하는 순간부터 코드는 동일하다.
2. **추상화의 가치는 두 번째 확장이 생길 때 드러난다.** if-else(방식 1)로 가면 세션과 토큰 두 가지에서 끝나지만, 세 번째 방식(API 키 등)이 생기면 Interceptor와 ArgumentResolver 두 곳에 동일한 분기가 추가된다. `AuthenticationExtractor`(방식 2)는 구현체 하나만 추가하면 끝난다.
3. **엔드포인트 분리가 컨트롤러 단순화를 만든다.** `/login`과 `/api/login`을 분리하자 각 컨트롤러가 자기 클라이언트에만 집중할 수 있게 됐다. 하나로 합쳐서 클라이언트 종류를 분기했다면 컨트롤러 안에 웹/모바일 판단 로직이 들어갔을 것이다.
4. **jjwt의 숫자 역직렬화는 실제로 만나봐야 아는 함정이다.** `Long.class`로 꺼내면 된다고 생각했지만, jjwt는 JSON 숫자를 `Double`로 역직렬화한다. `Number`로 받아 `.longValue()`로 꺼내는 패턴을 기억해두면 된다.
5. **`application.properties`의 main/test 관계를 다시 한번 확인했다.** `test/application.properties`가 있으면 Spring Boot는 test 파일 기준으로 컨텍스트를 구성하고, main 파일에만 있는 키는 resolve하지 않는다. 1단계에서 `cleanDatabase()`와 `data.sql`의 관계로 배웠던 것과 같은 맥락 — "설정 파일도 테스트와 main이 분리돼 있다."
6. **stateless의 편함과 로그아웃의 불편함은 동전의 양면이다.** 토큰은 서버가 아무것도 기억하지 않아서 확장이 쉽지만, 정확히 그 이유로 "발급한 토큰을 무효화할 수 없다". 세션 방식의 `session.invalidate()` 한 줄이 얼마나 강력한 것이었는지 토큰을 구현하고 나서야 실감했다.

---

## 8. 평가 기준 충족 한 줄씩

1. **공통 처리 분리:** `AuthenticationExtractor` 인터페이스로 "인증 정보를 꺼내는 층"을 추상화해 `LoginCheckInterceptor`와 `LoginMemberArgumentResolver`가 세션/토큰 방식을 모른 채 위임만 한다. 컨트롤러는 1단계와 동일하게 `@LoginMember Member member`만 받는다.
2. **다른 후보와 비교 설명:** 3.1~3.5 슬롯과 4.1~4.5 결정 모두 후보 표 + 선택 이유 + 트레이드오프 기록. 특히 if-else(방식 1) vs 인터페이스 추상화(방식 2)를 직접 비교해 OCP의 실용적 의미를 정리.
3. **트레이드오프 인식:** 토큰의 stateless 이점과 즉시 무효화 불가능이 동전의 양면임을 확인. 공존 방식의 복잡도가 `AuthenticationExtractor` 추상화로 Interceptor·ArgumentResolver 내부에 격리된다는 것을 구현으로 검증.

---

## 8.5 "생각해 볼 점"에 대한 2단계 최종 답변

### Q1. 브라우저와 모바일 앱은 인증 정보를 어떻게 다르게 다루는가?

브라우저는 서버가 내려준 `Set-Cookie`를 자동으로 저장하고 이후 요청마다 `Cookie` 헤더에 자동으로 첨부한다. 개발자가 신경 쓸 게 없다. 반면 모바일 앱은 이 자동 처리 혜택이 없다. 서버 응답에서 토큰을 직접 꺼내 앱 저장소에 저장하고, 이후 요청마다 `Authorization: Bearer <token>` 헤더를 직접 첨부해야 한다. "인증 정보를 누가 챙기는가" — 브라우저는 자동, 앱은 수동이다.

### Q2. 세션을 모바일 앱에서 그대로 사용하면 어떤 점이 편하고 어떤 점이 불편한가?

편한 점은 서버 코드를 전혀 바꾸지 않아도 된다는 것이다. 앱이 쿠키를 직접 저장·관리하고 매 요청에 첨부하면 서버는 기존 세션 방식 그대로 동작한다. 불편한 점은 앱이 쿠키를 수동으로 관리해야 한다는 것인데, 이는 모바일 앱에서 어색한 패턴이다. 또한 세션은 서버 메모리에 상태가 있어서 서버가 여러 대가 되면 세션 저장소 공유 문제가 생긴다.

### Q3. 토큰을 사용하면 어떤 점이 편하고 어떤 점이 불편한가?

편한 점은 서버가 아무것도 기억하지 않아도 된다는 것이다. 토큰 자체에 서명이 있어서 서버는 매 요청마다 서명만 검증하면 된다. 서버를 여러 대로 늘려도 세션 저장소를 공유할 필요가 없다. 불편한 점은 로그아웃과 토큰 무효화가 어렵다는 것이다. 발급된 토큰을 서버 측에서 즉시 무효화할 방법이 없다. 클라이언트가 토큰을 삭제하면 로그아웃 효과가 나지만, 탈취된 토큰은 만료 시간까지 계속 유효하다.

### Q4. 로그아웃과 만료는 어떻게 처리할 것인가?

웹은 1단계 그대로 `session.invalidate()`로 즉시 무효화한다. 모바일 토큰은 2단계에서 만료 없이 단순화했다. 앱이 토큰을 삭제하면 사실상 로그아웃이지만, 서버 측 강제 무효화는 안 된다. "동시 로그인을 막으려면 서버가 토큰 상태를 기억해야 하는데, 그러면 stateless가 아니다" — 이 모순이 4단계의 핵심 질문이 된다.

### Q5. 서버가 상태를 기억하는 방식과 기억하지 않는 방식은 어떤 차이가 있는가?

세션은 서버가 상태를 기억한다. 서버 메모리에 `memberId`가 있고, 클라이언트는 세션 ID(증표)만 들고 다닌다. 서버가 세션을 지우면 즉시 무효화된다. 토큰은 클라이언트가 상태를 들고 다닌다. 서버는 매 요청마다 토큰의 서명만 검증하고, 저장된 게 없으므로 무효화가 어렵다. 두 방식이 공존하는 이번 구현에서는 `AuthenticationExtractor` 추상화가 이 차이를 Interceptor·ArgumentResolver 내부에 격리해, 컨트롤러와 서비스는 어느 방식인지 모른 채 동일하게 동작한다.

---

## 9. 다음 단계 (인가)로 넘기는 질문

- 인증된 사용자 정보 안에 권한(매니저인지 일반 사용자인지)을 어디까지 담을 것인가? 토큰 claim에 넣을 것인가, 매 요청마다 DB에서 조회할 것인가?
- 토큰 페이로드에 `memberId`만 있는데, 매장 권한 확인을 위해 `storeId`를 추가할 것인가? 그러면 매장이 바뀔 때 토큰을 재발급해야 한다.
- 모바일 클라이언트에서도 인가 실패(403)를 동일하게 처리할 것인가? 인가 실패 응답 형태는 웹과 다를 필요가 있는가?

---

## 10. PR 본문 조각

```
2단계 - 모바일 앱 요청 인증

선택 도구:
- 인증 방식: 웹은 HttpSession(쿠키), 모바일은 JWT(Authorization: Bearer 헤더)
- 통합 구조: AuthenticationExtractor 인터페이스 + SessionAuthenticationExtractor / TokenAuthenticationExtractor 구현체
- 모바일 로그인 엔드포인트: POST /api/login (웹 /login과 분리)

다른 후보:
- 세션 그대로 모바일에도 적용 → 앱이 쿠키를 수동 관리해야 해 어색
- Interceptor/ArgumentResolver에 if-else 분기 → 인증 방식이 늘 때 두 곳에 중복
- 컨트롤러 완전 분리(/web, /api) → 비즈니스 로직 중복 과다

선택 이유:
- AuthenticationExtractor 추상화로 "인증 정보를 꺼내는 층"만 교체하면 Interceptor·ArgumentResolver·컨트롤러는 무수정
- /api/login 분리로 각 클라이언트에 최적화된 응답(웹: Set-Cookie, 모바일: 토큰 본문)을 단순하게 반환

불편하거나 아쉬운 점:
- 모바일 토큰 즉시 무효화 불가 — 탈취 시 만료 전까지 유효. 4단계에서 블랙리스트 또는 Refresh Token으로 보완 예정
- jjwt의 숫자 역직렬화(Double 문제) — Number.longValue()로 우회했지만 라이브러리 내부 동작에 의존
```
