package org.needleframe.core.web.app;

import org.needleframe.core.service.app.AppService;
import org.needleframe.core.web.response.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {
	
	@Autowired
	private AppService appService;
	
	@GetMapping("/app.json")
	@ResponseBody
	public ResponseMessage getApp() {
		return ResponseMessage.success(appService.getApp());
	}
	
}
