package roomescape.repository;

import java.util.Optional;
import roomescape.domain.Manager;

public interface ManagerRepository {

    Manager save(Manager manager);

    /**
     * memberId로 매니저를 조회한다.
     * 인가 판단 시 "이 로그인 사용자가 매니저인가, 어느 매장 담당인가"를 확인하는 핵심 쿼리.
     */
    Optional<Manager> findByMemberId(Long memberId);
}
