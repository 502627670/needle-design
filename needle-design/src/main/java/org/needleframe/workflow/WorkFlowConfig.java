package org.needleframe.workflow;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkFlowConfig {
	
	static WorkFlowContextService extendContextService = new DefaultWorkFlowContextService();
	
}
