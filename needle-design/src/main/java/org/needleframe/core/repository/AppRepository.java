package org.needleframe.core.repository;

import org.needleframe.core.domain.App;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AppRepository extends PagingAndSortingRepository<App, Long> {
	
}
