package org.needle.design;

import java.util.Date;

import org.needleframe.utils.DateUtils;

public class DateTest {
	
	public static void main(String[] args) {
		System.out.println(DateUtils.formatDate(new Date(1583338745000L), "yyyy-MM-dd HH:mm:ss"));
	}
	
}
