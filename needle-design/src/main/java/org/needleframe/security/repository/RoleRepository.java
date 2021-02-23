package org.needleframe.security.repository;

import java.util.List;

import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface RoleRepository extends PagingAndSortingRepository<Role, Long> {
	
	@Query("select ur.role from UserRoles ur where ur.user=?1 order by ur.role.id asc")
	public List<Role> findByUser(User user);
	
}
