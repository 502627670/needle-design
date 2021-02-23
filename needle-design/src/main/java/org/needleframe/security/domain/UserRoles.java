package org.needleframe.security.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="t_user_roles")
@EntityListeners(AuditingEntityListener.class)
public class UserRoles {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id")
	private User user;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="role_id")
	private Role role;
	
//	@ManyToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="group_id")
//	private Group group;
	
	@CreatedDate
	@Column(name="created_date")
	private Date createdDate;
	
	@CreatedBy
	@Column(name="created_by", length=30)
	private String createdBy;
	
	@LastModifiedDate
	@Column(name="last_modified_date")
	private Date lastModifiedDate;
	
	@LastModifiedBy
	@Column(name="last_modified_by", length=30)
	private String lastModifiedBy;
}
