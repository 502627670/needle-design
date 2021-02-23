package org.needleframe.workflow.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.core.domain.AbstractEntity;
import org.needleframe.security.domain.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流程
 * @author admin
 */
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="t_work_flow")
@EntityListeners(AuditingEntityListener.class)
public class WorkFlow extends AbstractEntity {
	
	// 主题
	@Column(name="title", length=120)
	private String title;
	
	/** 业务项 */
	@Column(name="module", length=80)
	private String module;
	
	@Column(name="module_name", length=100)
	private String moduleName;
		
	// 报告人
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="reporter_user_id")
	private User reporterUser;
	
	// 报告人
	@Column(name="reporter_name", length=120)
	private String reporterName;
	
}
