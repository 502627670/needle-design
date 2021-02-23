package org.needleframe.security.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.security.domain.Resource.ResourceType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="t_permission")
@EntityListeners(AuditingEntityListener.class)
public class Permission {
		
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="role_id")
	private Role role;
	
	// 权限名称
	@Column(name="name", length=100)
	private String name;
	
	// 资源标识
	@Column(name="resource", length=100)
	private String resource;
	
	@Column(name="resource_type", length=30)
	@Enumerated(EnumType.STRING)
	private ResourceType resourceType;
	
	@Column(name="resource_uri", length=120)
	private String resourceUri;
	
}
