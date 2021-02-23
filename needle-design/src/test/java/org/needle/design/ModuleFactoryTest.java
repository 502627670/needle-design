package org.needle.design;

import org.needleframe.context.ModuleFactory;
import org.needleframe.core.builder.ModuleBuilder;
import org.needleframe.security.domain.User;

public class ModuleFactoryTest {
	
	static ModuleFactory mf = new ModuleFactory();
	
	public static void main(String[] args) {
		ModuleBuilder builder = mf.build(User.class).showName("用户")
			.securityGroup("group")
			.prop("group")
			.name("用户组")
			.fk()
			.show("name")
			.filter().eq().end();		
		System.out.println(builder.getModule().getProp("group").getRuleType());
	}
	
}
