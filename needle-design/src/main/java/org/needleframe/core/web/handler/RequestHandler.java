package org.needleframe.core.web.handler;

@FunctionalInterface
public interface RequestHandler {
	
    Object doRequest() throws Exception;
    
}