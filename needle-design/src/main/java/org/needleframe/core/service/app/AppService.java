package org.needleframe.core.service.app;

import org.needleframe.core.domain.App;
import org.needleframe.core.repository.AppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AppService {
		
	@Autowired
	private AppRepository appRepository;
	
	public App getApp() {
		Page<App> apps = appRepository.findAll(PageRequest.of(0, 10));
		if(apps.getContent().isEmpty()) {
			App app = new App();
			app.setName("绣花针微内核应用框架");
			return app;
		}
		return apps.getContent().get(0);
	}
	
}
