package org.needleframe.core.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.context.AppContextService;
import org.needleframe.core.model.Module;
import org.needleframe.core.web.response.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/data")
public class DataController extends AbstractDataController {
	
	@Autowired
	private AppContextService appContextService;
	
	@Override
	protected Module getModule(String moduleName) {
		return appContextService.getModuleContext().getModule(moduleName, true);
	}
	
	// http://localhost:8080/data/role/1
	@RequestMapping("/{module}/get")
	@ResponseBody
	public ResponseMessage getData(@PathVariable("module") String moduleName, 
			String id,
			HttpServletRequest request, HttpServletResponse response) {
		return super.getData(moduleName, id, request, response);
	}
	
	// http://localhost:8080/data/role/list
	@RequestMapping("/{module}/list")
	@ResponseBody
	public ResponseMessage findList(@PathVariable("module") String moduleName,
			String[] _vp,  // viewProps: _vp=name&_vp=role.user.name,role.id
			String _vf,    // viewFilters: _vf=[{prop:name, op:=, value:john},...]
			String _sf,    // viewFilters: _sf=[{prop:name, op:in, value:{SubFilter}},...]
			@RequestParam(defaultValue="0") int _page, 
			@RequestParam(defaultValue="10") int _size,
			String _sort,
			String _direction,
			HttpServletRequest request, HttpServletResponse response) {
		return super.findList(moduleName, _vp, _vf, _sf, _page, _size, _sort, _direction, request, response);
	}
	
	@RequestMapping("/{module}/create")
	@ResponseBody
	public ResponseMessage create(@PathVariable("module") String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return super.create(moduleName, _data, request, response);
	}
	
	// http://localhost:8080/data/role/update?data=%5B%7B%22saleId%22%3A1%2C%22id%22%3A1%2C%22user%22%3A1%7D%5D
	@RequestMapping("/{module}/update")
	@ResponseBody
	public ResponseMessage update(@PathVariable("module") String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		request.getParameter("adminMemo");
		
		return super.update(moduleName, _data, request, response);
	}
	
	// http://localhost:8080/data/role/remove?id=1&id=2
	@RequestMapping("/{module}/remove")
	@ResponseBody
	public ResponseMessage remove(@PathVariable("module") String moduleName, String[] id,
			HttpServletRequest request, HttpServletResponse response) {
		return super.remove(moduleName, id, request, response);
	}
	
	@RequestMapping("/{module}/preview")
	@ResponseBody
	public ResponseMessage preview(@PathVariable("module") String moduleName, String _props, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return super.preview(moduleName, _props, _data, request, response);
	}
	
	@RequestMapping("/{module}/saveImport")
	@ResponseBody
	public ResponseMessage saveImport(@PathVariable("module") String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return super.saveImport(moduleName, _data, request, response);
	}
	
//	@RequestMapping("/data/action/{action}")
//	@ResponseBody
//	public ResponseMessage action(@PathVariable("action") String _action, String _data,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			if(StringUtils.hasText(_action)) {
//				Action action = contextService.getModuleContext().getAction(_action, true);
//				List<ActionData> actionData = 
//						JsonUtils.fromJSON(_data, new TypeReference<List<ActionData>>() {}, om);
//				dataService.execute(action, actionData);
//				return "ok";
//			}
//			return "found no action";
//		});
//	}
	
}
