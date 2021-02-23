package org.needleframe.utils;

import javax.servlet.http.Cookie;

public class CookieUtils {
	
	public static String getCookieValue(String cookieName, Cookie[] cookies) {
		if(cookies != null) {
			for(Cookie cookie : cookies) {
				if(cookie.getName().equalsIgnoreCase(cookieName)) {
					return cookie.getValue();
				}
			}
		}
		return "";
	}
	
}
