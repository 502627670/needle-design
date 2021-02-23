package org.needleframe.security.repository;

import java.util.List;

import org.needleframe.security.domain.User;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {
	
	public List<User> findByUsername(String username);
	
}
