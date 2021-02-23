package org.needle.design;

import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RuleType;

public class RuleTypeTest {
	
	public static void showInfo(ModuleProp mp) {
		int ruleType = mp.getRuleType();
		int mask = RuleType.HIDE_INFO.getMask();
		if((mask & ruleType) == mask) {
			ruleType ^= mask;
		}
		mp.setRuleType(ruleType);
	}
	
	
	public static void test() {
		ModuleProp mp = new ModuleProp();
		int ruleType = mp.getRuleType();
		
		showInfo(mp);
		System.out.println(mp.getRuleType());
		
		System.out.println(ruleType |= RuleType.HIDE_FORM.getMask());
		System.out.println(ruleType ^= RuleType.HIDE_FORM.getMask());
		
		ruleType ^= RuleType.HIDE_FORM.getMask();
		mp.setRuleType(ruleType);
		System.out.println(ruleType);
		
		ruleType = mp.getRuleType();
		System.out.println(ruleType);
		ruleType ^= RuleType.HIDE_LIST.getMask();
		mp.setRuleType(ruleType);
		System.out.println(ruleType);
		
		ruleType = mp.getRuleType();
		System.out.println(ruleType);
		ruleType ^= RuleType.HIDE_EDIT.getMask();
		mp.setRuleType(ruleType);
		System.out.println(ruleType);
		
		ruleType = mp.getRuleType();
		System.out.println(ruleType);
		ruleType ^= RuleType.HIDE_INFO.getMask();
		mp.setRuleType(ruleType);
		System.out.println(ruleType);
	}
	
	public static boolean hasRuleType(int mask, RuleType ruleType) {
		return (ruleType.getMask() & mask) == ruleType.getMask();
	}
	
	public static void main(String[] args) {
		test();
		System.out.println(hasRuleType(1552, RuleType.ABSOLUTE_FILE));
	}
	
}
