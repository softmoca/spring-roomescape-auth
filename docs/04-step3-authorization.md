# 04. 3단계 — 인가: 자기 매장 예약만 관리하기

> 이 단계의 핵심 질문: **인가 판단을 어디에 두고, 어떤 정보로 권한을 확인할 것인가?**
>
>
> 이 문서는 **5번 대화 (Sonnet)** 에서 채워진다.
>

---

## 1. 요구사항 요약 (가이드 발췌)

### 목표

- 인증과 인가를 **구분**한다.
- 로그인한 매니저가 어떤 매장에 속해 있는지 확인한다.
- 접근하려는 예약이 어떤 매장에 속해 있는지 확인한다.
- 매장 매니저가 자기 매장의 예약만 관리할 수 있게 한다.
- 인가 실패와 인증 실패를 **구분해** 처리한다.

### 요구사항

- **매장 매니저 식별:** 로그인한 사용자가 매니저인지 확인 / 어떤 매장 관리인지 확인.
- **예약 접근 제한:** 자기 매장만 조회/변경/삭제 / 다른 매장 접근은 거부.
- **실패 처리:** 비로그인 = 인증 실패 / 로그인 + 권한 없음 = 인가 실패 / 둘을 뭉개지 않는다.

### 구현 조건

- 로그인한 사용자가 어떤 매장에 속해 있는지 확인할 수 있어야 한다.
- 접근하려는 예약이 어떤 매장에 속해 있는지 확인할 수 있어야 한다.
- 두 정보를 비교해 요청을 허용 / 거부.
- 인가 판단 로직이 컨트롤러에 과도하게 흩어지지 않도록 한다.

### 완료 기준

- [ ]  매장 매니저는 자기 매장의 예약만 관리할 수 있다.
- [ ]  다른 매장 예약에 접근하는 요청은 거부된다.
- [ ]  인증 실패와 인가 실패가 구분된다.
- [ ]  인가 판단 위치와 선택 이유를 설명했다.

---

## 2. 2단계까지의 상태

- 인증 인프라 (1·2단계 확정) 끝났고, 이제 권한이 필요한 지점:
    - **웹 인증:** `HttpSession` + 쿠키. `POST /login` → 세션 발급 → `JSESSIONID` 쿠키로 이후 요청 식별.
    - **모바일 인증:** JWT(HMAC-SHA256) + `Authorization: Bearer` 헤더. `POST /api/login` → 토큰 발급 → 매 요청마다 헤더로 전달.
    - **공통 추출 구조:** `AuthenticationExtractor` 인터페이스 + `SessionAuthenticationExtractor` / `TokenAuthenticationExtractor` 두 구현체. `LoginCheckInterceptor`와 `LoginMemberArgumentResolver`는 추출기 목록에 위임만 하며 방식을 모른다.
    - **컨트롤러:** `@LoginMember Member member` 파라미터로 사용자를 받는다. 세션/토큰 어느 방식이든 컨트롤러는 무수정.
    - **인증 실패 응답:** `UnauthorizedException(401)` → `GlobalExceptionHandler` → `{ "message": "..." }`.
- 도입해야 할 새 개념:
    - `Store` 도메인 + `store` 테이블 — 매장 단위 권한 경계
    - `Manager` 또는 `Member.role` 같은 권한 표현
    - 매니저와 매장의 관계 표현 (`Manager` 별도 도메인 / `Member.storeId` / 매핑 테이블 — 3.2에서 결정) →매니저가 여러 매장을 관리할 수 있도록 열어두기 잠정 선택
    - 예약과 매장의 연결 (`Reservation.storeId` 직접 / `Theme.storeId` 경유 — 4번에서 결정)
    - `ForbiddenException(403)` — `RoomeScapeClientException` 상속으로 `GlobalExceptionHandler` 수정 불필요
- 이 단계에서 손대야 할 파일들 (예상):
    - 신규: `Store` 도메인·레포지토리, `ForbiddenException`, 매니저-매장 관계 표현 클래스
    - 수정: `Reservation` 또는 `Theme`에 매장 연결 / `ReservationService`의 변경·삭제 메서드에 인가 추가
    - `schema.sql` 갱신 (store 테이블, reservation 또는 theme에 store_id FK)
- 이월되는 미결 질문 (2단계에서 결론 못 낸 것)
    - 모바일 토큰의 즉시 무효화 — 2단계에서 best-effort로 단순화. 블랙리스트/Refresh Token은 4단계로 이월.
    - JWT 페이로드에 `storeId`나 `role`을 추가할지 — 2단계에서 `memberId`만 담기로 했으나, 3단계 인가에서 매 요청마다 매니저 DB 조회가 발생하면 재검토 가능. → 3.5에서 결정.

---

## 2.5 생각해 볼 점 (가이드 원문)

가이드가 단계 시작 전 던지는 사고 자극 질문들. 답은 슬롯·4번에서 점차 나옴.
체크박스는 단계 끝에서 "이 질문에 대한 내 답이 어딘가 들어갔는가" 점검용.

- [x]  **인가 판단은 Controller / Service / Domain 중 어디에 두는 것이 적절한가?** → 3.1
- [x]  **예약 조회와 예약 변경에서 인가 판단 위치가 달라질 수 있는가?** → 3.1 + 3.3 (조회는 권한 조건 포함 쿼리, 변경은 조회 후 비교 등 분기 가능성)
- [x]  **인가 실패 시 어떤 응답을 반환할 것인가?** → 3.4 (일반 사용자=404 / 매니저=403 정책)
- [x]  **매장 매니저가 여러 매장을 관리하게 되면 구조를 어떻게 바꿀 것인가?** → 3.2 (관계 모델링의 확장성)
- [x]  **인증된 사용자 정보를 인가 판단에 어떻게 전달할 것인가?** → 3.5 (ID만 전달 / 매장 정보까지 전달 / 토큰 클레임)

---

## 3. 가이드가 명시한 선택 카테고리

### 3.1 인가 판단 위치

**가이드 질문:** Controller에서 먼저 막을 것인가, Service에서 검증할 것인가, 도메인 객체의 규칙으로 표현할 것인가?

> 📋 **01에서 잠정 결정 (질문 4의 결론):** **B (Service)가 주 방어선, E (Interceptor 거친 체)는 보조.원리 한 줄 (01 인용):** *"판단에 필요한 정보가 모이는 가장 이른 지점에서 처리한다."*
>
> - 인증·역할 같은 거친 체 인가(매니저 역할인가?) → Interceptor (요청 정보만으로 판단 가능)
> - 세밀한 인가(이 예약이 네 매장 것인가?) → Service (DB 조회 후 대상 데이터가 손에 들어온 뒤에야 판단 가능)
    > **선례 (01 인용):** 미션 2의 `findByIdAndName`이 이미 Service에 둔 인가. `name` 비교를 `storeId` 비교로 바꾸기만 하면 됨. **자리는 이미 옳았음.다층 방어 (01 인사이트 3):** Interceptor 거친 체로 명백한 경우를 거름(매니저 아닌 사람의 `/admin/**` 진입 차단 → 불필요한 DB 조회 절약). Service 고운 체로 정밀 판단. Repository 쿼리 조건은 보조 방어선(선택).
    > **왜 다른 후보가 안 맞나 (01 검증):**
> - A (Controller): 다른 입구(다른 Service 메서드, 콘솔 UI)가 생기면 그대로 뚫림
> - C (Domain): 매력적이지만, 매장 비교를 위해선 결국 Service가 데이터를 손에 쥐고 위임해야 함 → Service 안에서 도메인 메서드 호출 형태로 결합 가능 (B + C 조합)
> - D (`AccessPolicy`): `FutureOnlyPolicy` 결의 정책 객체로 빼는 길. 인가 규칙이 복잡해지면 매력 ↑. 단순한 매장 비교 한 줄에는 과한 추상화 가능성
> - E 단독: Interceptor 시점엔 예약 ID만 알지 그 예약의 매장은 모름 → 단독으로는 불가
    > **3단계 구현하며 검증할 것:** Service에 인가 추가 시 메서드 시그니처 변경 범위가 어디까지 번지는가. `Member`를 통째로 받을지, `memberId`만 받을지.

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. Controller에서 먼저 차단 | `@LoginMember` 받아 컨트롤러가 검사 | 빠르게 차단, 코드 가시성 | 다른 진입점(다른 Service 호출, 콘솔 UI)이 생기면 그대로 뚫림. 인가가 Controller에 결합 | 제외 |
| B. Service에서 검증 | `deleteByManager(reservationId, memberId)` 안에서 권한 확인 | 진입점이 달라져도 인가는 항상 통과. DB 조회 후 실제 데이터를 손에 쥔 시점에서 비교 가능 | 매 요청마다 Manager DB 조회 추가 | **주 방어선** |
| C. Domain 객체 메서드로 표현 | `manager.canManage(reservation)` | 인가 의도가 코드에 직접 드러남. 업무 언어와 일치 | 단독으로는 Service가 데이터를 손에 쥐고 위임해야 하므로 B 없이 쓸 수 없음 | B와 조합 |
| D. 별도 `AccessPolicy` 객체 | `FutureOnlyPolicy`와 같은 결의 정책 객체 | 인가 규칙이 복잡해지면 매력 ↑ | 매장 비교 한 줄에 과한 추상화. 현재 규모에서 이득 없음 | 제외 |
| E. Interceptor 단독 (거친 체) | 어노테이션 + Interceptor | 역할 체크처럼 "매니저인가"는 가능 | Interceptor 시점엔 예약을 아직 조회하지 않음 → "자기 매장 것인가"는 판단 불가 | 거친 체 보조로만 |

**선택:** B(Service 주 방어선) + C(도메인 위임) + E(Interceptor 거친 체 보조)

**선택 이유:**

판단에 필요한 정보가 모이는 가장 이른 지점에서 처리한다는 원칙을 따랐다.

- **거친 체 (E — Interceptor):** `ManagerCheckInterceptor`가 `/admin/**` 진입 시 매니저 역할을 확인한다. "이 사람이 매니저인가?"는 요청 정보 + DB 1회 조회만으로 판단 가능하다. 매니저가 아니면 이 지점에서 차단해 예약 조회 자체를 막는다. 불필요한 DB 조회를 절약하는 효과도 있다.
- **고운 체 (B — Service):** `deleteByManager()`에서 예약을 조회한 뒤 `manager.canManage(reservation)`으로 매장을 비교한다. "이 예약이 이 매니저의 매장 것인가?"는 예약을 DB에서 꺼낸 뒤에야 알 수 있으므로 Service가 맞는 자리다.
- **도메인 위임 (C — canManage):** Service가 데이터를 손에 쥔 상태에서 `manager.canManage(reservation)`에 판단을 위임한다. `storeId` 단순 비교를 직접 쓸 수도 있지만, 이 메서드는 "이 매니저가 이 예약을 관리할 수 있는가"라는 업무 언어를 코드에 그대로 표현해 의도가 즉시 드러난다.

**받아들인 트레이드오프:**

매 요청마다 `findByMemberId(memberId)` DB 조회가 두 번 발생한다 (Interceptor 1회, Service 1회). 현재 규모에서는 무시 가능하다. 캐싱이나 토큰 클레임 임베드(3.5 C안)가 필요해지면 그 시점에 재검토한다.

**다시 결정할 조건:**

Manager 조회 DB 비용이 실측치로 병목이 되거나, 인가 규칙이 복잡해져 `canManage()` 한 줄로 표현하기 어려워지면 D(AccessPolicy)를 재검토.

→ 관련 ADR: `decisions/003-authz-judgement-location.md`

---

### 3.2 매장 매니저와 매장의 관계 표현 방식

### 3.2 매장 매니저와 매장의 관계 표현 방식

**가이드 질문:** 매니저가 하나의 매장만 / 여러 매장 관리? 매니저가 여러 매장을 관리할 수 있도록 열어두기 잠정 선택

---

**후보 구조 비교**

| 후보 | 데이터 모델 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. `Manager(id, member_id, store_id)` (별도 도메인, 1:1 매니저당 매장 하나 시작) | `Manager` 테이블이 `Member`와 `Store`를 연결. 지금은 1:1이지만 구조상 확장 가능 | `Member` 테이블 무수정. 기존 `insertMember()` 시그니처 그대로. `Member`(인증) / `Manager`(인가) 관심사 분리 명확 | `Manager` 테이블 + 도메인 클래스 추가. Service에서 memberId → Manager 조회 1번 더 필요 | **선택** — 기존 테스트 영향 최소, 관심사 분리 명확 |
| B. `manager_store(manager_id, store_id)` 매핑 테이블 (N:M) | `Manager` 도메인 + 별도 매핑 테이블. 매니저 하나가 여러 매장을 가질 수 있음 | 1:N 요구사항을 스키마 변경 없이 흡수 가능 | 현재 요구사항(1:1)에 비해 구조 과잉. Service 인가 로직이 `storeId` 단순 비교 → `storeIds` 컬렉션 포함 비교로 복잡해짐. 테스트 픽스처에 매핑 테이블 데이터도 삽입 필요 | 확장 시 고려 — 지금은 과함 |
| C. `Member.role` + `Member.store_id` 컬럼 추가 | `Member` 테이블에 `role ENUM`, `store_id FK` 컬럼 추가. `Manager` 도메인 없음 | 조회 단계가 `Member` 하나로 끝남. 구조 단순 | `Member` 테이블에 인가 책임이 섞임 — 인증 도메인이 인가 데이터를 들고 다님. 기존 `insertMember()` 시그니처 변경 → 모든 테스트(`PopularThemeStepTest`, `UserReservationStepTest` 등)의 호출부가 영향받음. 매니저가 여러 매장을 가지면 1:N이 불가 | 제외 — 테스트 파급이 크고, 관심사 오염 |
| D. `Store.manager_id` FK (역방향, 매장 중심) | `store` 테이블에 `manager_id FK` 컬럼. "이 매장의 매니저는 누구인가"를 매장 기준으로 표현 | 매장당 매니저 하나가 고정이면 단순 | 매니저 기준 조회(`이 매니저가 어느 매장을 관리하나?`)가 역방향 JOIN이 됨. 매니저가 여러 매장을 담당하면 `store` 테이블에 같은 `manager_id`가 여러 행에 분산 — 컬럼 의미가 흔들림 | 제외 — 조회 방향이 역방향이고, 확장 시 의미 불명확 |

---

**선택:** A — `Manager(id, member_id, store_id)` 별도 도메인, 1:1로 시작

**선택 이유:**

세 가지 기준이 A를 가리킨다.

1. **기존 테스트 영향 최소.** C를 선택하면 `insertMember()` 시그니처가 바뀌어 `PopularThemeStepTest`, `UserReservationStepTest`, `MyReservationStepTest`, `ReservationPolicyStepTest` 등 기존 테스트 전체의 호출부가 깨진다. A는 `Member` 테이블을 손대지 않으므로 기존 픽스처가 그대로 살아남는다.
2. **관심사 분리.** `Member`는 "누구인가(인증)"의 개념이고, `Manager`는 "무엇을 할 수 있는가(인가)"의 개념이다. 이 둘을 같은 테이블에 섞으면(C안) `Member`를 조회할 때마다 인가 컬럼이 따라온다. `Manager`를 별도 도메인으로 두면 인가 검사가 필요할 때만 `Manager`를 조회하고, 인증 흐름(`@LoginMember Member member`)은 그대로 유지된다.
3. **확장 경로가 열려 있다.** 지금은 1:1로 시작하지만, "한 매니저가 여러 매장 관리" 요구가 오면 B안(매핑 테이블)으로 전환할 수 있다. 그 시점의 변경 범위는 `manager` 테이블 분리 + `ManagerRepository` + Service 쿼리 수정으로 한정된다. 반면 D안은 역방향 설계라 "매니저 기준" 조회가 처음부터 어색하다.

**받아들인 트레이드오프:**

인가 판단 시점마다 `memberId → Manager` 조회가 DB 조회 1회 추가된다. 현재 규모(단일 서버, 학습용)에서는 무시 가능하다. 트래픽이 커지면 `Manager`를 세션/토큰에 캐싱하는 방향(3.5 C안)을 재검토하면 된다.

**다시 결정할 조건:**

- "한 매니저가 여러 매장을 관리해야 한다"는 요구 → B안(매핑 테이블)으로 전환. `manager` 테이블을 `manager_store(manager_id, store_id)` 매핑으로 분리.
- "매 요청마다 Manager DB 조회가 병목"이라는 실측치 → 3.5 C안(토큰 클레임에 storeId 임베드) 또는 세션 캐싱 검토.

---

**각 후보의 발전 경로 (나중에 요구사항이 바뀌면)**

**A → B (1:1 → 1:N 확장):**

```java
현재 A안 스키마:
  manager(id, member_id, store_id)   ← store_id가 단일 값

B안으로 전환 시:
  manager(id, member_id)             ← store_id 컬럼 제거
  manager_store(manager_id, store_id) ← 매핑 테이블 추가

바뀌는 코드:
  - schema.sql: manager 테이블 분리 + manager_store 추가
  - JdbcManagerRepository: findStoreIdByMemberId() → findStoreIdsByMemberId() (List 반환)
  - ReservationService 인가 로직: storeId 단순 비교 → storeIds.contains(reservation.getStoreId())
  - ReservationTestHelper: insertManagerStore() 헬퍼 추가
  컨트롤러·Interceptor는 무수정.
```

**C안 (Member.role + Member.store_id) — 왜 지금은 안 하는가, 언제 매력적인가:**

```java
C안이 매력적인 조건:
  - 매니저와 일반 사용자를 role 하나로 분기하는 단순한 RBAC가 전부일 때
  - "Manager" 개념이 없고 Member.role만으로 인가를 표현하는 소규모 서비스
  - DB 조회 1회를 아끼는 것이 핵심 제약일 때

지금 C안을 선택하지 않는 이유:
  - Member 테이블이 인증(email, password)과 인가(role, store_id)를 동시에 들고 다님
  - 나중에 역할이 늘어나면 (ADMIN, MANAGER, STAFF 등) Member 테이블에 컬럼이 계속 추가됨
  - store_id가 nullable이 되는데, "일반 사용자의 store_id는 null"이라는 암묵적 규칙이 생김
  - 가장 즉각적인 문제: 기존 insertMember() 호출 수십 곳이 한꺼번에 깨짐
```

**D안 (Store.manager_id) — 왜 역방향인가:**

```java
D안의 본질적 문제:
  "이 예약이 내 매장 것인가?"를 확인하려면 매니저 → 매장 방향이 자연스럽다.
  D안은 매장 → 매니저 방향이라 항상 역방향 JOIN이 필요하다.

  // A안: 자연스러운 방향
  Long storeId = managerRepository.findStoreIdByMemberId(memberId);  // 매니저의 매장을 바로 꺼냄
  
  // D안: 역방향 JOIN
  Long storeId = storeRepository.findByManagerId(memberId).getId();  // 매장 테이블에서 역방향 조회
  
  매니저가 여러 매장을 담당하게 되면 D안은 Store 행이 여러 개가 돼야 하는데,
  그러면 Store.manager_id 컬럼의 의미("이 매장의 담당 매니저")가 흔들린다.
  실제로 D안은 "매장당 매니저가 1명으로 고정"인 서비스(반대 방향의 1:1)에서만 자연스럽다.
```

→ 관련 ADR: `decisions/003-authz-judgement-location.md` (인가 판단 위치와 묶어서 기록)

---

### 3.3 예약 접근 권한 확인 방식

**가이드 질문:** 예약을 조회한 뒤 매장 권한을 비교할 것인가, 처음부터 권한 조건을 포함해 조회할 것인가?

> 📋 **01에서 발견한 선례:** 미션 2의 `ReservationService.findByIdAndName`이 이미 **A (조회 후 비교)** 패턴. `.filter(r -> r.getName().equals(name))`로 이름 비교를 한 것을 매장 비교로 바꾸기만 하면 됨.
**잠정 방향:** A 기본 + Repository에 B의 권한 조건 쿼리를 보조 방어선으로(선택).
**3단계 구현하며 검증할 것:** A의 응답 코드는 자연스럽게 403, B는 404 — 3.4와 묶임. 일반 사용자 영역(404 유지) vs 매니저 영역(403 권장)이 갈리므로 두 영역에서 패턴이 달라질 수 있음.
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 조회 후 비교 | `findById(id)` → `manager.canManage(reservation)`으로 비교 → 불일치 시 403 | 코드 흐름이 직관적. 존재 확인과 권한 확인이 분리되어 각각의 에러(404 vs 403)를 명확히 낼 수 있음 | DB 조회 1회 + 비교 1회 | **선택** |
| B. 권한 조건 포함 조회 | `findByIdAndStoreId(id, managerStoreId)` → 없으면 404 | DB 1회로 끝남 | "존재하지 않음"과 "권한 없음"이 구분되지 않아 항상 404. 매니저 영역에서 403이 필요한 3.4 정책과 충돌 | 제외 |
| C. 도메인 메서드 위임 | `manager.canManage(reservation)` | 의도가 코드에 드러남 | 단독 후보가 아님 — A와 결합해서 사용 | A와 조합 |

**선택:** A 기본 + C 결합

**선택 이유:**

미션 2의 `findByIdAndName`이 이미 A 패턴이었다. 예약을 먼저 꺼낸 뒤 `filter(r -> r.getMember().getId().equals(memberId))`로 비교했다. 3단계는 그 비교 대상을 `memberId` → `storeId`로 바꾼 것이다. 자리는 이미 옳았다.

B를 선택하면 존재하지 않는 예약과 다른 매장 예약을 구분할 수 없어 항상 404가 나온다. 3.4에서 매니저 영역은 403을 내기로 결정했으므로 B는 그 정책과 정면으로 충돌한다.

삭제(`deleteByManager`)와 조회(`findByManagerStore`) 두 메서드에서 패턴이 달라진다.

- `deleteByManager`: A 패턴 — `findById` → `canManage` → 불일치 시 403
- `findByManagerStore`: 전체 조회 후 Java 스트림으로 `theme.storeId` 필터링. SQL WHERE 조건으로 내리는 대신 메모리 필터를 택했다. 예약 수가 적은 현재 규모에서는 차이 없고, 쿼리 복잡도를 줄이는 쪽을 선택.

**받아들인 트레이드오프:**

`findByManagerStore`의 메모리 필터는 예약 수가 많아지면 비효율적이다. 트래픽이 커지면 `findByStoreId(storeId)` 쿼리로 전환한다.

**다시 결정할 조건:**

예약 건수가 늘어 `findAll()` + 메모리 필터가 응답 시간에 영향을 주면 Repository에 `findByStoreId(storeId)` 추가.

---

### 3.4 인가 실패 응답 방식

**가이드 질문:** 권한 없음으로 응답할 것인가, 리소스를 숨기는 방식으로 응답할 것인가?

> 📋 **01에서 잠정 결정 (질문 4의 인사이트 4):** **C (사용자 영역별 분리).**
>
> - **일반 사용자 영역:** 다른 사용자의 리소스 접근 시 **404.** 리소스 존재 여부 자체를 숨기는 게 더 중요. (미션 2의 `MyReservationStepTest` 정책 유지)
> - **매니저/관리자 영역:** 다른 매장 예약 접근 시 **403.** 내부 운영자에게는 *"다른 매장의 예약은 관리할 수 없습니다"*가 *"존재하지 않는 예약입니다"*보다 명확한 피드백.
    > **근거 (01 인용):** *"매니저는 일반 사용자가 아니라 예약 관리 권한을 일부 가진 내부 운영자. 따라서 다른 매장의 예약을 관리하려고 했을 때 '존재하지 않는 예약'보다 '다른 매장의 예약은 관리할 수 없다'가 더 명확한 피드백."***신규 예외:** `ForbiddenException extends RoomeScapeClientException` 추가. `RoomeScapeException` 체계가 자동 처리하므로 `GlobalExceptionHandler` 수정 불필요.
    > **3단계 구현하며 검증할 것:** 두 영역에서 응답 코드가 갈리는 게 클라이언트에 혼란을 주지 않는가. 테스트는 영역별로 분리 검증.

**후보**

| 후보 | 응답 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 403 Forbidden 통일 | 모든 인가 실패 → 403(명시적 거절) | 단순. 권한 없음이 명시적 | 리소스 존재 여부가 노출됨. 일반 사용자 영역에서 정보 유출 위험 | 매니저 영역에만 적용 |
| B. 404 Not Found 통일 | 모든 인가 실패 → 404(정보노출 차단) | 리소스 존재 여부 노출 차단 | 매니저가 다른 매장 예약을 건드렸을 때 "존재하지 않는 예약"은 부정확한 피드백 | 사용자 영역에만 적용 |
| C. 영역별 분리 (사용자=404, 매니저=403) | 사용자가 타인 예약 접근 → 404 / 매니저가 타 매장 예약 접근 → 403(상황별 분기) | 각 영역의 목적에 맞는 피드백. 기존 `MyReservationStepTest` 정책(404) 유지 | 클라이언트가 두 가지 패턴을 인지해야 함 | **선택** |

**선택:** C — 영역별 분리 (일반 사용자 = 404, 매니저 = 403)

**선택 이유:**

두 영역의 목적이 다르다.

**일반 사용자 영역** — 다른 사람의 예약 ID를 알아도 건드릴 수 없어야 한다. "이 예약은 존재하지만 당신 것이 아닙니다(403)"라고 알려주면 공격자가 예약 ID를 열거할 수 있다. "없는 예약입니다(404)"로 존재 여부를 숨기는 게 맞다. 미션 2의 `MyReservationStepTest` 정책을 그대로 유지한다.

**매니저 영역** — 매니저는 이미 인증된 내부 운영자다. 다른 매장 예약에 접근했을 때 "존재하지 않는 예약입니다(404)"는 오해를 만든다. "다른 매장의 예약에는 접근할 수 없습니다(403)"이 명확하고 디버깅에도 유용한 피드백이다.

구현상으로는 `deleteByOwner`의 `findByIdAndMember` 필터 패턴(→ 404)과 `deleteByManager`의 `findById` + `canManage` 검증(→ 403) 두 가지 경로로 자연스럽게 분리된다.

**받아들인 트레이드오프:**

동일한 `/admin/**` 경로에서도 비로그인은 401, 로그인+매니저 아님은 403, 매니저+타 매장은 403 — 클라이언트가 세 가지 상태를 구분해서 처리해야 한다. 각각 의미가 다르므로 섞는 게 더 혼란스럽다.

**다시 결정할 조건:**

보안 감사에서 "403이 예약 존재 여부를 노출한다"는 지적이 오면 매니저 영역도 404로 통일 검토. 단, 운영 디버깅 편의성과 트레이드오프.

→ 관련 ADR: `decisions/004-403-vs-404.md`

---

### 3.5 인증 정보와 도메인 정보의 연결 방식

**가이드 질문:** 로그인 사용자 ID로 매니저를 조회 / 로그인 객체에 매장 정보 포함?

> 📋 **01에서 미결. 단, 1단계 구현 결과 확정됨:**
1단계 3.4에서 **도메인 `Member` 그대로** (`@LoginMember Member member`)를 선택했다.
`LoginMember(id, email)` 별도 값 객체는 "지금 단계에서는 과한 추상화"로 판단해 채택하지 않았다.
→ 컨트롤러는 `member.getId()`만 사용하고, 나머지 정보(매장 소속 등)는 인가 시점에 DB 조회로 얻는다. 이 결정이 3.5의 A안(로그인 객체에는 ID만, 인가 시점에 매니저 조회)과 자연스럽게 이어진다.
재검토 조건: 토큰 클레임에 매장 정보를 넣어 DB 조회를 없애고 싶어질 때 (C안) — 단, 매장 이동 시 토큰 무효화 문제 수반.
**3단계 구현하며 검증할 것:** A의 DB 조회 비용이 거슬리는 수준인가. 2단계에서 토큰 방식을 도입한다면 C(토큰 클레임에 매장 정보)가 다시 매력적이 될 수 있음 — 단, 매니저 매장 이동 시 토큰 무효화 필요(4단계 학습과 연결).
>

**후보**

| 후보 | 어떻게 동작 | 장점 | 단점 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| A. 로그인 객체에는 ID만, 인가 시점에 매니저 조회 | `@LoginMember Member member` → `member.getId()` → `managerRepository.findByMemberId()` | 인증 객체 단순. 매니저 정보는 항상 DB 최신값 | 매 요청마다 Manager DB 조회 1회 추가 | **선택** |
| B. 로그인 객체에 매장 정보까지 포함 | `@LoginMember` 주입 시 Manager도 함께 조회해서 컨트롤러에 전달 | Service에서 Manager 추가 조회 불필요 | 인증 흐름(`ArgumentResolver`)에 인가 책임이 섞임. 매니저가 아닌 일반 사용자 요청에서도 Manager 조회 시도 발생 | 제외 |
| C. (토큰 사용 시) 토큰 클레임에 매장 정보 임베드 | JWT 발급 시 `storeId` 클레임 포함 → DB 조회 없이 토큰에서 꺼냄 | Manager DB 조회 0회 | 매니저 매장 이동 시 기존 토큰의 storeId가 stale해짐 → 토큰 무효화 필요. 4단계 동시 로그인 방지와 얽힘 | 4단계 이후 재검토 |

**선택:** A — 로그인 객체에는 ID만, 인가 시점에 매니저 조회

**선택 이유:**

1단계 3.4에서 `@LoginMember`에 도메인 `Member` 그대로를 주입하고 `member.getId()`만 사용하기로 확정했다. `LoginMember(id, email)` 별도 값 객체나 매장 정보 포함은 "지금 단계에서 과한 추상화"로 판단해 채택하지 않았다. 이 결정이 3.5 A안으로 자연스럽게 이어진다.

B는 `ArgumentResolver`(인증 책임)가 Manager(인가 책임)까지 들고 다니게 된다. 관심사가 섞이고, 일반 사용자 요청에서도 Manager 조회를 시도하다 Optional.empty()를 처리해야 하는 불필요한 분기가 생긴다.

C는 매력적이지만 2단계에서 JWT 페이로드에 `memberId`만 담기로 확정했고, `storeId`를 추가하면 매니저 매장 이동 시 토큰 무효화 문제가 생긴다. 이는 4단계 동시 로그인 방지 학습과 연결되므로 지금 건드리지 않는다.

**받아들인 트레이드오프:**

매 `/admin/**` 요청마다 `findByMemberId(memberId)` 조회가 Interceptor 1회, Service 1회 총 2회 발생한다. 현재 규모에서 무시 가능하다.

**다시 결정할 조건:**

Manager 조회 비용이 실측치로 병목이 되면 C(토큰 클레임 임베드)를 재검토. 단, 토큰 무효화(4단계)가 먼저 해결돼야 안전하게 도입 가능하다.

---

## 4. 구현 중 새로 마주친 결정들

### 4.1 예약-매장 연결을 어디에 둘 것인가

**질문:** 인가 판단 시 "이 예약이 어느 매장 것인가"를 어떻게 알아낼 것인가?
`Reservation`이 직접 `storeId`를 들고 있을 것인가, `Theme.storeId`를 경유할 것인가?

---

**후보**

| 후보 | 스키마 변화 | 인가 비교 코드 | 예약 생성 시 storeId 처리 | 내 서비스에서의 적합도 |
| --- | --- | --- | --- | --- |
| X. `Theme.store_id` 경유 | `theme` 테이블에 `store_id FK` 추가 | `reservation.getTheme().getStoreId()` | 불필요 — 테마 선택 시 매장이 자동 결정됨 | **선택** |
| Y. `Reservation.store_id` 직접 | `reservation` 테이블에 `store_id FK` 추가 | `reservation.getStoreId()` | 명시적으로 넣어야 함 (요청에서 받거나 Theme 경유 조회) |  |
| Z. 별도 `reservation_store` 매핑 테이블 | 새 매핑 테이블 추가 | 추가 JOIN 필요 | 불필요하지만 INSERT 시 매핑 행 추가 필요 | 과잉 |

---

**선택:** X — `Theme.store_id` 경유

**선택 이유:**

세 가지 관찰이 X안을 가리킨다.

**1. 도메인 언어와 일치한다.**
방탈출 서비스에서 테마는 매장이 소유한다. "무인도 탈출" 테마는 강남점 것, "도시 탈출"은 홍대점 것이다. 예약은 테마를 선택하는 행위이고, 테마가 어느 매장 소속인지는 이미 테마에 담겨 있다. "이 예약의 매장은 어디인가?" → "이 예약의 테마가 속한 매장이 어디인가?"가 자연스러운 질문 경로다.

**2. 예약 생성 흐름이 그대로 유지된다.**
Y안에서 `reservation.store_id`를 채우려면 두 가지 중 하나를 선택해야 한다.

- 요청 바디에 `storeId`를 추가 → `POST /user/reservations` 스펙 변경, 클라이언트 수정 필요
- 서버가 `themeId → storeId`를 DB 조회해서 자동으로 채움 → 결국 Theme을 경유하는 셈이라 X안과 DB 조회 수가 같음

어느 쪽이든 Y안이 X안보다 나은 이유가 없어진다. 반면 X안은 예약 생성 API(`POST /user/reservations`)의 요청 스펙(`date`, `timeId`, `themeId`)이 그대로 유지된다.

**3. 데이터 일관성 책임이 단순하다.**
Y안처럼 `Reservation`과 `Theme` 양쪽에 `storeId`가 존재하면 동기화 책임이 생긴다. 테마의 매장이 바뀌면 기존 예약들의 `store_id`도 따라 바꿔야 하는가? X안은 이 질문 자체가 없다. 테마의 매장이 바뀌면 그 테마를 참조하는 예약은 자동으로 새 매장 소속이 된다.

---

**받아들인 트레이드오프:**

`Theme.reconstitute()` 시그니처에 `storeId`가 추가된다. `JdbcThemeRepository`의 RowMapper, `JdbcReservationRepository`의 BASE_SELECT와 RowMapper가 수정된다. `insertTheme()` 픽스처 헬퍼에 `storeId` 인수가 추가되어 `PopularThemeStepTest`, `UserReservationStepTest`, `MyReservationStepTest`, `ReservationPolicyStepTest`가 함께 영향받는다.

Y안을 선택해도 `insertReservation()`이 깨지는 범위는 동일하다. 따라서 이 트레이드오프는 X/Y 중 어느 쪽을 선택해도 피할 수 없는 것이고, X안이 도메인 언어상 더 옳기 때문에 받아들인다.

---

**다시 결정할 조건:**

"같은 테마가 여러 매장에서 동시에 운영된다"는 요구가 들어오면 재검토. 그 시점엔 `Theme.storeId` 1:1 관계가 깨지고, `theme_store` 매핑 테이블 또는 `Reservation.storeId` 직접 방식(Y안)으로 전환이 필요하다.

---

### 4.2 매니저-매장 관계 모델링 발전 경로 상세

각 후보가 어떻게 발전할 수 있는지, 언제 재검토해야 하는지 — 별도 섹션(3.2 아래의 상세 정리)에 기록.

**A → B (1:1 → 1:N 확장):**

```java
현재 A안:
  manager(id, member_id, store_id)

B안 전환 시:
  manager(id, member_id)             ← store_id 컬럼 제거
  manager_store(manager_id, store_id) ← 매핑 테이블 추가

바뀌는 코드:
  - schema.sql: manager 테이블 분리 + manager_store 추가
  - JdbcManagerRepository: findStoreIdByMemberId() → findStoreIdsByMemberId() (List 반환)
  - ReservationService 인가 로직: storeId 단순 비교 → storeIds.contains(reservation.getTheme().getStoreId())
  - ReservationTestHelper: insertManagerStore() 헬퍼 추가
  컨트롤러·Interceptor는 무수정.
```

### 4.3 ManagerCheckInterceptor와 LoginCheckInterceptor의 /admin/** 처리 분리

**결정:** `/admin/**`를 `LoginCheckInterceptor`의 `excludePathPatterns`에 유지하고, `ManagerCheckInterceptor`가 독립적으로 인증+인가를 모두 처리한다.

**이유:** `ManagerCheckInterceptor`가 이미 비로그인을 401로, 매니저 아님을 403으로 나눠서 처리한다. `LoginCheckInterceptor`까지 통과하게 하면 `/admin/**`에서 인증 체크가 이중으로 발생한다. 역할이 명확히 분리된다 — `LoginCheckInterceptor`는 `@LoginMember` 파라미터 기반 일반 API 인증, `ManagerCheckInterceptor`는 매니저 전용 경로 인증+인가.

### 4.4 `findByManagerStore`의 메모리 필터 vs Repository 쿼리

**결정:** 현재는 `findAll()` 후 Java 스트림으로 `theme.storeId` 필터링.

**이유:** 현재 예약 건수 규모에서는 차이 없고, Repository에 `storeId` 조건 쿼리를 추가하면 테스트에서 storeId가 있는 테마·매장을 항상 준비해야 하는 복잡도가 증가한다. 성능 병목이 실측되면 `findByStoreId(storeId)` 쿼리로 전환한다.

---

## 5. 구현 메모 (시간순)

- ✅ `schema.sql`: `store` 테이블 추가, `theme.store_id FK`, `manager` 테이블 추가. DROP 순서 조정 (reservation → manager → member → reservation_time → theme → store)
- ✅ `data.sql`: store 시드 2개(강남점·홍대점) 추가, theme INSERT에 store_id 포함
- ✅ `Store` 도메인: 단순 값 객체. `create` / `reconstitute` 팩토리 메서드 패턴 유지
- ✅ `Manager` 도메인: `memberId`, `storeId` 보유. `canManage(Reservation)` — 인가 판단을 도메인 메서드로 표현 (B + C 조합)
- ✅ `ForbiddenException`: `RoomeScapeClientException` 상속 → `GlobalExceptionHandler` 수정 불필요 확인
- ✅ `Theme`: `storeId` 필드 추가, `create/reconstitute` 시그니처 변경 → 파급 범위 즉시 확인 (Repository RowMapper, 테스트 픽스처 전체)
- ✅ `JdbcThemeRepository`: SELECT·INSERT·GROUP BY에 `store_id` 추가
- ✅ `JdbcReservationRepository`: `BASE_SELECT`에 `th.store_id AS theme_store_id` 추가, RowMapper 수정
- ✅ `JdbcManagerRepository`: `findByMemberId` 구현 — Optional 반환 (매니저 아닌 사용자는 row 없음)
- ✅ `ReservationService`: `findByManagerStore`, `deleteByManager` 추가. `findManagerOrThrow`에서 Optional.empty() → `ForbiddenException` (404 아닌 403 — 매니저 영역 정책)
- ✅ `ThemeCreateCommand`, `ThemeRequest`: `storeId` 필드 추가
- ✅ `ManagerCheckInterceptor`: `/admin/**` 거친 체. 비로그인 → 401, 매니저 아님 → 403
- ✅ `WebMvcConfig`: `/admin/**` 이중 체크 방지 구조 정리. `ManagerCheckInterceptor` 등록
- ✅ `AdminReservationController`: `@LoginMember` 추가, `findAll` → `findByManagerStore`, `delete` → `deleteByManager`
- ⚠️ `Theme.create` 시그니처 변경으로 `insertTheme()` 3인수 오버로드 사용 불가 → 기존 테스트 전체 컴파일 에러 확인. `insertStore()` + `insertTheme(name, storeId)` 패턴으로 일괄 수정
- ⚠️ `ReservationTestHelper.login()`을 `Response` 반환으로 바꿨다가 `LoginStepTest`, `MobileAuthStepTest`에서 `Response cannot be converted to String` 컴파일 에러 발생 → `String` (JSESSIONID 값) 반환으로 되돌림
- ✅ `IntegrationTest.cleanDatabase()`: store·manager 테이블 DELETE 및 AUTO_INCREMENT 리셋 추가. FK 역순 삭제 순서 정리
- ✅ 기존 테스트 5개 파일 수정 (insertStore + insertTheme(storeId) 적용)
- ✅ 도메인 단위 테스트 수정: `ThemeTest` — VALID_STORE_ID 추가 + `storeId_null이면_예외` 추가, `ReservationTest` — `Theme.reconstitute`에 storeId 추가
- ✅ `ManagerReservationStepTest` 신규: 인가 시나리오 전체 검증 (접근 제어, 자기 매장 조회·삭제, 인증·인가 구분)
- ✅ `ErrorResponseStepTest`: `AuthzError` 중첩 클래스 추가 (403 응답 형식 검증)

---

## 6. 테스트 변경 사항

| 테스트 파일 | 변경 유형 | 사유 |
| --- | --- | --- |
| `IntegrationTest` | 수정 | `cleanDatabase()`에 store·manager 테이블 DELETE + AUTO_INCREMENT 리셋 추가. FK 역순 삭제 순서 정리 |
| `ReservationTestHelper` | 수정 | `insertStore()`, `insertManager()` 추가. `insertTheme` 시그니처 변경 (3인수 제거, storeId 필수 오버로드로 교체). `login()` String 반환 유지 (기존 테스트 호환) |
| `ThemeTest` | 수정 | `VALID_STORE_ID` 상수 추가. `Theme.create/reconstitute` 호출부 전체에 storeId 전달. `storeId_null이면_예외_발생` 테스트 추가 |
| `ReservationTest` | 수정 | `VALID_THEME`의 `Theme.reconstitute`에 storeId(1L) 추가 |
| `ReservationPolicyStepTest` | 수정 | `storeId` 필드 추가. `setUp`에 `insertStore` + `insertTheme(storeId)` 적용 |
| `UserReservationStepTest` | 수정 | `setUp`에 `insertStore` + `insertTheme(storeId)` 적용 |
| `MyReservationStepTest` | 수정 | `setUp`에 `insertStore` + `insertTheme(storeId)` 적용 |
| `PopularThemeStepTest` | 수정 | `storeId` 필드 추가. `setUp`에 `insertStore` + `insertTheme(storeId)` 적용. `예약_없는_테마_제외` 테스트에서 `storeId` 재사용 |
| `ErrorResponseStepTest` | 수정 + 추가 | `setUp`에 `insertStore` + `insertTheme(storeId)` 적용. `AuthzError` 중첩 클래스 추가 (매니저 아님 403, 비로그인 401) |
| `ManagerReservationStepTest` | 신규 추가 | 3단계 인가 시나리오 전체 검증. 접근 제어(401/403), 자기 매장 조회·삭제(204), 타 매장 삭제(403), 인증·인가 구분 명시 |

---

## 7. 얻은 인사이트

1. **인가는 거친 체 / 고운 체 두 단계로 나뉜다.** `ManagerCheckInterceptor`(거친 체)는 "매니저인가?"만 판단하고, `ReservationService`(고운 체)는 "자기 매장 것인가?"를 판단한다. 두 질문이 다른 시점에 다른 정보로 답해야 하기 때문에 자연스럽게 두 층으로 나뉜다. 하나의 Interceptor로 전부 처리하려 했다면 Interceptor가 예약 DB 조회까지 해야 했을 것이다.
2. **`manager.canManage(reservation)` — 도메인 메서드가 인가 의도를 드러낸다.** `this.storeId.equals(reservation.getTheme().getStoreId())`를 Service에 직접 쓰는 것과 `manager.canManage(reservation)`를 쓰는 것은 결과는 같지만 읽는 경험이 다르다. 메서드 이름이 "이 매니저가 이 예약을 관리할 수 있는가"라는 업무 언어를 코드에 그대로 옮긴다. 이게 B + C 조합의 실체다.
3. **도메인 변경(`Theme.storeId` 추가)의 파급 범위를 과소평가했다.** `Theme.create` 시그니처 하나가 바뀌었을 뿐인데 `JdbcThemeRepository`, `JdbcReservationRepository` RowMapper, `ThemeService`, `ThemeCreateCommand`, `ThemeRequest`, 그리고 모든 테스트의 `insertTheme()` 호출이 한꺼번에 영향받았다. 도메인 변경은 파급이 크다. 3.2 합의(매니저-매장 모델링)를 코드 짜기 전에 먼저 한 것이 테스트 폭주를 막는 데 결정적이었다.
4. **인가 실패 응답 코드는 "누가 읽는가"에 따라 달라진다.** 일반 사용자는 다른 사람의 예약 존재 여부를 알 필요가 없으므로 404. 매니저는 내부 운영자이므로 "다른 매장 예약에 접근할 수 없다"는 명확한 피드백(403)이 맞다. 응답 코드 선택이 단순히 HTTP 스펙 문제가 아니라 "이 응답을 받는 사람이 누구인가"의 문제임을 확인했다.
5. **`login()` 반환 타입 하나가 기존 테스트를 모두 깼다.** `Response`로 바꾸면 더 유연하지만, 기존 `LoginStepTest`, `MobileAuthStepTest` 전체가 컴파일 에러가 됐다. 헬퍼는 기존 코드와의 하위 호환을 먼저 생각해야 한다. 새 반환 타입이 필요하면 오버로드를 추가하거나 별도 메서드를 만드는 쪽이 낫다.
6. **`ManagerCheckInterceptor`의 Manager 조회가 Service에서 중복된다.** Interceptor가 "매니저인가?"를 확인하고, Service도 "매니저인가?"를 다시 확인한다. 이 이중 조회가 불편하게 느껴지지만, 사실 각자의 이유가 있다 — Interceptor는 역할 차단, Service는 데이터를 손에 쥔 시점의 정밀 검증. 분리된 책임이 이 중복을 만든다. 이를 없애려면 Interceptor에서 확인한 Manager를 request attribute로 넘기거나 토큰 클레임에 임베드해야 한다 (3.5 C안).

---

## 8. 평가 기준 충족 한 줄씩

1. **공통 처리 분리:** `ManagerCheckInterceptor`(거친 체 — `/admin/**` 역할 확인)와 `ReservationService`(고운 체 — 매장 범위 검증)로 인가 로직이 컨트롤러 밖에서 처리된다. 컨트롤러는 `@LoginMember Member member`를 받아 Service에 `member.getId()`를 전달하기만 한다.
2. **다른 후보와 비교 설명:** 5개 슬롯 모두 후보 표 + 선택 이유 + 트레이드오프 기록. 특히 3.1에서 Controller / Service / Domain / AccessPolicy / Interceptor 다섯 후보를 비교하고 B+C+E 조합을 선택한 이유를 정리. 3.4에서 403/404/영역별 분리 세 후보를 비교해 C를 선택한 이유와 각 영역에서 다른 코드가 나오는 구조를 설명.
3. **트레이드오프 인식:** Manager 조회 이중 발생, `findByManagerStore`의 메모리 필터 한계, 403이 리소스 존재를 노출한다는 점, `login()` 반환 타입 변경의 파급 등 각 결정마다 받아들인 트레이드오프와 재검토 조건을 명시.

---

## 8.5 "생각해 볼 점"에 대한 3단계 최종 답변

### Q1. 인가 판단은 Controller / Service / Domain 중 어디에 두는 것이 적절한가?

Service를 주 방어선(B)으로, Domain 메서드를 보조(C)로, Interceptor를 거친 체(E)로 사용했다. Controller는 인가 판단을 전혀 모른다. 판단에 필요한 정보(예약의 매장, 매니저의 매장)가 모이는 가장 이른 지점이 Service이고, 그 판단 의미를 `manager.canManage(reservation)` 도메인 메서드가 표현한다.

### Q2. 예약 조회와 예약 변경에서 인가 판단 위치가 달라질 수 있는가?

달라졌다. `findByManagerStore`(조회)는 전체 예약을 꺼낸 뒤 스트림 필터로 매장을 걸러낸다. `deleteByManager`(삭제)는 예약 ID로 단건 조회 후 `canManage()`로 비교한다. 조회는 결과 범위를 좁히는 방향, 삭제는 권한 미충족 시 명시적 403을 던지는 방향으로 자연스럽게 갈렸다.

### Q3. 인가 실패 시 어떤 응답을 반환할 것인가?

영역별로 분리했다. **일반 사용자 영역** — 타인 예약 접근 시 404(존재 여부 노출 방지, 기존 정책 유지). **매니저 영역** — 타 매장 예약 접근 시 403(내부 운영자에게 명확한 피드백). 비로그인은 두 영역 모두 401. 세 상태가 코드와 메시지로 구분된다.

### Q4. 매장 매니저가 여러 매장을 관리하게 되면 구조를 어떻게 바꿀 것인가?

현재 `manager(id, member_id, store_id)` 1:1 구조를 `manager(id, member_id)` + `manager_store(manager_id, store_id)` 매핑 테이블로 전환한다. `ManagerRepository.findByMemberId()` → `findStoreIdsByMemberId()` (List 반환), Service 인가 로직 → `storeIds.contains(...)`. 컨트롤러·Interceptor는 무수정. 4.2에 발전 경로 상세 기록.

### Q5. 인증된 사용자 정보를 인가 판단에 어떻게 전달할 것인가?

`@LoginMember Member member`로 인증 객체를 받고, `member.getId()`를 Service에 넘긴다. Service가 `managerRepository.findByMemberId(memberId)`로 매니저 정보를 별도 조회한다. 인증 객체(Member)와 인가 정보(Manager)를 분리해서 각자의 책임을 유지한다.

---

## 9. 다음 단계 (동시 로그인 방지)로 넘기는 질문

- 현재 Manager 조회가 Interceptor와 Service에서 각각 발생하는 이중 구조를 없애려면 어떻게 해야 하는가? request attribute 전달, 세션 캐싱, 토큰 클레임 임베드 중 어느 게 맞는가?
- 세션 기반 인증(웹)에서 매니저 정보를 세션에 캐싱하면 매니저 매장이 바뀌었을 때 stale 데이터 문제가 생기는가? 이를 막으려면 어떤 무효화 전략이 필요한가?
- JWT 클레임에 `storeId`를 넣으면 DB 조회를 없앨 수 있다. 그런데 매니저가 매장을 바꾸면 기존 토큰은 어떻게 처리해야 하는가? 동시 로그인 방지 메커니즘과 어떻게 연결되는가?
- 인가까지 끝낸 지금, 같은 계정의 두 디바이스 동시 로그인을 막으려면 어디에 상태를 추가해야 하는가?
- 내가 선택한 인증 방식에서 동시 로그인 방지가 가장 어려운 지점은 어디인가?

---

## 10. PR 본문 조각

```
3단계 - 인가 (매장 매니저)

선택 도구:
- 인가 판단: Service 주 방어선(B) + Domain 메서드 위임(C) + Interceptor 거친 체(E) 조합
- 매니저-매장 관계: Manager 별도 도메인, 1:1 시작 (A안)
- 예약-매장 연결: Theme.storeId 경유 (X안)
- 인가 실패 응답: 영역별 분리 — 사용자=404, 매니저=403 (C안)

다른 후보:
- Controller 단독 인가 → 다른 진입점이 생기면 뚫림
- Member.role + Member.storeId → 기존 insertMember() 호출 전체 파급
- 403 통일 → 일반 사용자 영역에서 예약 존재 노출
- 404 통일 → 매니저에게 "다른 매장 예약"이라는 명확한 피드백 불가

선택 이유:
- 판단에 필요한 정보가 모이는 가장 이른 지점(Service)에서 처리
- Manager.canManage(reservation)으로 도메인 언어가 코드에 그대로 드러남
- 영역별 응답 코드 분리로 각 사용자에게 적절한 피드백 제공

인가 판단 위치:
- ManagerCheckInterceptor: "매니저인가?" 거친 체 → 비로그인 401, 매니저 아님 403
- ReservationService: "자기 매장 예약인가?" 고운 체 → 타 매장 403, 없는 예약 404

유지하거나 변경하고 싶은 점:
- 유지: Service 인가 + Domain 메서드 위임 조합 — 테스트도 단위 테스트로 검증 가능
- 변경 고려: Manager 조회 이중 발생 → 4단계 토큰 무효화 해결 후 토큰 클레임 임베드 검토
```
