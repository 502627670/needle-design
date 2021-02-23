package org.needleframe.utils;

import java.io.File;

public class FileUtils {
	
	public static String getParentPath(String filePath) {
		int i = filePath.lastIndexOf("/");
		int j = filePath.lastIndexOf("\\");
		int max = i > j ? i : j;
		if(max >= 0) {
			return filePath.substring(0, max + 1);
		}
		return filePath;
	}
	
	public static String getBasename(String fileOrPath) {
		String[] array = null;
		if(fileOrPath.indexOf(File.separator) > -1) {
			array = fileOrPath.split("\\" + File.separator);
		}
		else {
			String regex = File.separator.indexOf("\\") > -1 ? "\\/" : "\\\\";
			array = fileOrPath.split(regex);
		}
		String basename = array[array.length - 1];
		int i = basename.lastIndexOf(".");
		if(i > -1) {
			basename = basename.substring(0, i);
		}
		return basename;
	}
	
	public static String getSuffix(String filename) {
		String suffix = "";
		int i = filename.lastIndexOf(".");
		if(i > -1) {
			suffix = filename.substring(filename.lastIndexOf(".") + 1);
		}
		return suffix;
	}
	
	public static String getSuffix(String filename, String defaultSuffix) {
		String suffix = defaultSuffix;
		int i = filename.lastIndexOf(".");
		if(i > -1) {
			suffix = filename.substring(filename.lastIndexOf(".") + 1);
		}
		return suffix;
	}
	
	public static void main(String[] args) {
		System.out.println(getBasename("logo.png"));
		System.out.println(getBasename("d:/aa/bb/cc.jpg"));
		System.out.println(getBasename("d:\\aa\\bb\\dd.jpg"));
	}
	
}
