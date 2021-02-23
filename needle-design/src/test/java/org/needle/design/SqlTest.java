package org.needle.design;

public class SqlTest {
	
	public static void main(String[] args) {
		String regex = "(?<!\\?)2";
		System.out.println("1 or 2".replaceFirst(regex, ""));
		System.out.println("1 and 2".replaceFirst(regex, ""));
		System.out.println("1 and (2 or 3)".replaceFirst(regex, ""));
		System.out.println("2 and 3".replaceFirst(regex, ""));
		System.out.println("<![CDATA[1 or 2]]>".replaceFirst(regex, ""));
		
	}
	
}
