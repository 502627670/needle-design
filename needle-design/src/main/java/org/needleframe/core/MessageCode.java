package org.needleframe.core;

public enum MessageCode {	
	
	OK(20000),
	
	FAILED(-1),
	
	ILLEGAL_TOKEN(50008),
	
	OTHER_CLIENT_LOGGED_IN(50012),
	
	SESSION_EXPIRED(50014);
	
//	PARAMETER_ERROR("参数错"),
//	
//	OBJECT_EXIST("对象已存在"),
//	
//	OBJECT_NOT_EXIST("对象不存在"),
//	
//	ITEM_STOCK_SHORTAGE("商品库存不足"),
//	
//	TO_JSON_FAILED("转换json失败"),
//	
//	FROM_JSON_FAILED("json转换失败"),
//	
//	ALIPAY_FAILED("支付宝支付失败"),
//	
//	WEIXIN_FAILED("微信支付失败"),
//	
//	MAIL_SEND_FAILED("邮件发送失败"),
//	
//	SMS_SNED_FAILED("短信发送失败"),
//	
//	CAPTCHA_EXPIRES("验证码已失效"),
//	
//	ERROR_500("内部错误"),
//	
//	ERROR_404("资源不存在");
	
	private int code;
	
	private MessageCode(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
	
}
