package org.needleframe.security.repository;

import org.needleframe.security.domain.UserRoles;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRolesRepository extends PagingAndSortingRepository<UserRoles, Long> {
	
}
