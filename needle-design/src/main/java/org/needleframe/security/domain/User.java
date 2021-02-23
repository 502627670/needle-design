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

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="t_user")
@EntityListeners(AuditingEntityListener.class)
public class User {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="username", length=60)
	private String username;
	
	@Column(name="password", length=120)
	private String password;
	
	@Column(name="fullname", length=100)
	private String fullname;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="group_id")
	private Group group;
	
	@Column(name="description", length=100)
	private String description;
	
	@Enumerated(EnumType.STRING)
	@Column(name="user_status", length=30)
	private UserStatus userStatus = UserStatus.ENABLE;
	
	@Column(name="profile", length=120)
	private String profile;
	
	@Column(name="sort_order")
	private Integer sortOrder = 0;
	
	public User() {}
	
	public User(Long id) {
		this.id = id;
	}
	
	public static enum UserStatus {
		DISABLE,
		ENABLE;
	}
	
}
