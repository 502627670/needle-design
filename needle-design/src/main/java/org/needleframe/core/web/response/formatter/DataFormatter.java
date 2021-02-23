package org.needleframe.core.web.response.formatter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.needleframe.core.web.response.DataModule;
import org.springframework.data.domain.Page;

public interface DataFormatter {
	
	public Locale getLocale();
	
	public void format(DataModule module);
	
	public void format(DataModule module, Map<String,Object> data);
	
	public void format(DataModule module, List<Map<String,Object>> dataList);
	
	public void format(DataModule module, Page<Map<String,Object>> page);
	
}
