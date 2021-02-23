package org.needleframe.workflow.repository;

import java.util.List;

import org.needleframe.workflow.domain.WorkFlow;
import org.needleframe.workflow.domain.WorkNode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface WorkNodeRepository extends PagingAndSortingRepository<WorkNode, Long> {
	
	@Query("from WorkNode t where t.workFlow=?1 order by t.sortOrder asc")
	public List<WorkNode> findByWorkFlow(WorkFlow workFlow);
	
}
