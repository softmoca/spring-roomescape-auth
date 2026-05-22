package roomescape.domain;

import roomescape.domain.exception.InvalidDomainException;

public class Manager {

    private final Long id;
    private final Long memberId;
    private final Long storeId;

    private Manager(Long id, Long memberId, Long storeId) {
        validateMemberId(memberId);
        validateStoreId(storeId);
        this.id = id;
        this.memberId = memberId;
        this.storeId = storeId;
    }

    public static Manager create(Long memberId, Long storeId) {
        return new Manager(null, memberId, storeId);
    }

    public static Manager reconstitute(Long id, Long memberId, Long storeId) {
        return new Manager(id, memberId, storeId);
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new InvalidDomainException("매니저의 회원 ID는 비어 있을 수 없습니다.");
        }
    }

    private static void validateStoreId(Long storeId) {
        if (storeId == null) {
            throw new InvalidDomainException("매니저의 매장 ID는 비어 있을 수 없습니다.");
        }
    }

    /**
     * 이 매니저가 해당 예약을 관리할 권한이 있는지 확인한다.
     * 예약의 테마가 속한 매장과 이 매니저의 담당 매장이 같아야 한다.
     */
    public boolean canManage(Reservation reservation) {
        return this.storeId.equals(reservation.getTheme().getStoreId());
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getStoreId() {
        return storeId;
    }
}
