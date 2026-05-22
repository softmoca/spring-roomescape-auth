package roomescape.repository;

import java.util.Optional;
import roomescape.domain.Store;

public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(Long id);
}
