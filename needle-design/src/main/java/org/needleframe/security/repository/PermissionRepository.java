package org.needleframe.security.repository;

import java.util.List;

import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.domain.Role;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PermissionRepository extends PagingAndSortingRepository<Permission, Long> {

	public List<Permission> findByRoleIn(List<Role> roles);
	
	public List<Permission> findByRoleInAndResourceType(List<Role> roles, ResourceType resourceType);
	
}
