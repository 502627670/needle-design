package org.needleframe.workflow.repository;

import java.util.List;

import org.needleframe.workflow.domain.WorkFlow;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface WorkFlowRepository extends PagingAndSortingRepository<WorkFlow, Long> {
	
	public List<WorkFlow> findByModule(String module);
	
}
