package org.needleframe.core.repository;

import org.needleframe.security.domain.Group;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface GroupRepository extends PagingAndSortingRepository<Group, Long> {

}
