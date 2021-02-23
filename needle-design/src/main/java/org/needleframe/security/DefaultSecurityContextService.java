package org.needleframe.security;

import org.needleframe.AbstractContextService;
import org.needleframe.context.ModuleFactory;
import org.needleframe.context.ModuleFactory.ActionFactory;
import org.needleframe.context.ModuleFactory.MenuFactory;
import org.needleframe.core.domain.App;
import org.needleframe.core.model.ModuleProp.RuleType;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.security.domain.Group;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.User;
import org.needleframe.security.domain.User.UserStatus;
import org.needleframe.security.domain.UserRoles;
import org.needleframe.security.service.SecurityContextService;
import org.springframework.data.domain.Sort;

public class DefaultSecurityContextService extends AbstractContextService implements SecurityContextService {
	
	@Override
	protected void defModules(ModuleFactory mf) {
		mf.build(User.class).showName("用户")
			.securityGroup("group")
			.prop("group").name("用户组").fk().show("name").filter().eq().showInfo().showList().showForm().showEdit()
			.prop("username").name("用户名").filter(Op.RLIKE).rule(RuleType.REQUIRED, RuleType.NO_UPDATE)
			.prop("password").name("密码").rule().addRule(RuleType.HIDE_INFO).addRule(RuleType.HIDE_LIST).required()
			.prop("fullname").name("姓名")
			.prop("userStatus").name("用户状态").filter().eq().rule().required().defaultValue(UserStatus.ENABLE)
			.prop("sortOrder").name("排序")
			.prop("description").name("备注")
			.prop("profile").name("头像").image().end()
			.addUnique("username")
			.sort(Sort.by("sortOrder").ascending())
			.addCRUD()
			.addChild(UserRoles.class).showName("用户角色").addCRUD()
			.prop("user").name("用户").fk().cascadeDelete().show("username").filter().eq().rule().required()
			.prop("role").name("角色").fk().cascadeDelete().ref("id").show("name").rule().required().end()
			.and()
			.build(Role.class).showName("角色")
			.addUnique("name")
			.prop("name").name("名称").filter().eq().rule().required()
			.prop("description").name("描述").text().showList()
			.prop("group").name("用户组").fk().show("name").end()
			.addCRUD()
			.addChild(Permission.class).showName("权限")
			.prop("role").name("角色").fk().show("name").rule().required()
			.prop("name").name("名称").rule().required()
			.prop("resource").name("授权").rule().required()
			.prop("resourceType").name("资源类型").rule().required()
			.prop("resourceUri").name("资源URI").rule().required().end()
			.and()
			.build(Group.class).showName("用户组")
			.addUnique("name")
			.refSelf(true)
			.prop("parent").name("上级组").fk().show("name").filter().eq().rule().noUpdate()
			.prop("name").name("组名").rule().required().filter().eq()
			.prop("description").name("备注").text()
			.prop("path").hide().system().end()
			.prop("offspringNumber").hide().system().end()
			.addCRUD();
		
		mf.build(App.class).showName("应用设置")
			.prop("name").name("应用名称").showList().end()
			.prop("logo").name("应用图标").absoluteImage().showList().end();
	}
	
	@Override
	protected void defActions(ActionFactory af) {
		// 第一个Act的module自动设定为Action的target
	}
	
	protected void defMenus(MenuFactory mf) {
		mf.build("权限管理", Integer.MAX_VALUE).uri("/security").icon("el-icon-s-cooperation")
			.addItem(User.class).name("用户").icon("el-icon-user")
			.addItem(Role.class).name("角色").icon("role")
			.addItem(Group.class).name("用户组").icon("el-icon-s-data")
			.addItem(App.class).name("应用设置").icon("el-icon-s-tools");
	}
	
}
