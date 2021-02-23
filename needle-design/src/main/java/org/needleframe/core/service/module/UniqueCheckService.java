package org.needleframe.core.service.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.service.Condition;
import org.needleframe.core.service.DefaultDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UniqueCheckService {
	
	@Autowired
	private DefaultDataService dataService;
	
	@Autowired
	private MessageSource messageSource;
	
	public void check(Module module, List<Map<String,Object>> dataList) {
		dataList.forEach(data -> {
			check(module, data);
		});
	}
	
	public void check(Module module, Map<String,Object> data) {
		List<String[]> uniqueProps = module.getUniqueProps();
		Pageable pageable = PageRequest.of(0, 1);
		List<ViewProp> viewProps = ViewProp.from(module.getPk());
		uniqueProps.forEach(props -> {
			StringBuilder messageBuilder = new StringBuilder();
			List<ViewFilter> viewFilters = new ArrayList<ViewFilter>();
			for(int i = 0; i < props.length; i++) {
				String prop = props[i];
				ModuleProp mp = module.getProp(prop);
				String name = messageSource.getMessage(mp.getName(), new Object[0], mp.getName(), Locale.getDefault());
				Object value = data.get(prop);
				if(value == null || value.toString().trim().equals("")) {
					return;
				}
				viewFilters.add(ViewFilter.eq(prop, value));
				if(mp.getValues().size() > 0) {
					value = messageSource.getMessage(value.toString(), new Object[0], value.toString(), Locale.getDefault());
				}
				messageBuilder.append("[").append(name).append("=").append(value).append("]");
			}
			
			Condition condition = new Condition(viewProps, viewFilters);
			List<Map<String,Object>> results = 
					dataService.findList(module, condition, pageable);
			if(!results.isEmpty()) {
				throw new ServiceException("数据已存在：" + messageBuilder);
			}
		});
	}
	
}
