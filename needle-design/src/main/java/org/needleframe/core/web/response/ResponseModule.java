package org.needleframe.core.web.response;

import org.needleframe.core.MessageCode;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class ResponseModule extends ResponseMessage {
	
	@Setter
	private DataModule module;
	
	public ResponseModule(String status, String message) {
		super(status, message);
	}
	
	public ResponseModule(String status, int code, String message) {
		super(status, code, message);
	}
	
	public static ResponseModule success(DataModule module, Object data) {
		ResponseModule responseModule = new ResponseModule(SUCCESS, MessageCode.OK.getCode(), null);
		responseModule.module = module;
		responseModule.data(data);
		return responseModule;
	}
	
	public static ResponseModule failed(String message) {
		return failed(MessageCode.FAILED.getCode(), message);
	}
	
	public static ResponseModule failed(int code, String message) {
        return new ResponseModule(FAILED, code, message);
    }
	
//	public ResponseModule data(Object data) {
//		super.data(data);
//		return this;
//	}
//	
//	public ResponseModule module(Module module) {
//		this.setModule(new DataModule(module));
//		return this;
//	}
	
}
