package org.needleframe.security.web;

import org.springframework.stereotype.Controller;

@Controller
public class GroupController {
	
//	@Autowired
//	private AppContextService moduleContextService;
//	
//	@Autowired
//	private DataService dataService;
//	
//	@Autowired
//	private GroupService groupService;
//	
//	@Autowired
//	private DataFormatter dataFormatter;
	
//	@RequestMapping("/data/group/create")
//	@ResponseBody
//	public ResponseMessage create(String _data,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = moduleContextService.getModuleContext();
//			Module module = moduleContext.getModule(Group.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<ActionData> _dataList = dataHandler.getCreateData(_data, request);
//			ActionData actionData = _dataList.stream()
//				.filter(ad -> module.getName().equals(ad.getModule()))
//				.findFirst().orElseThrow(() -> new ServiceException("新建用户组必须传入用户组数据"));
//			AfterGroupCreate after = new AfterGroupCreate(module, actionData);
//			List<ActionData> dataList = dataService.create(module, _dataList, null, after);
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule);
//			return ResponseModule.success(dataModule, dataList);
//		});
//	}
	
//	@RequestMapping("/data/group/update")
//	@ResponseBody
//	public ResponseMessage update(String _data,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = moduleContextService.getModuleContext();
//			Module module = moduleContext.getModule(Group.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<Map<String,Object>> dataList = dataHandler.getUpdateData(_data, request);
//			dataService.update(module, dataList, null, null);
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule, dataList);
//			return ResponseModule.success(dataModule, dataList);
//		});
//	}
	
//	@RequestMapping("/data/group/remove")
//	@ResponseBody
//	public ResponseMessage remove(String[] id,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = moduleContextService.getModuleContext();
//			Module module = moduleContext.getModule(Group.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<Serializable> ids = dataHandler.getDeleteData(id);
//			BeforeGroupDelete before = new BeforeGroupDelete(ids);
//			dataService.delete(module, ids.toArray(new Serializable[ids.size()]), before, null);
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule);
//			return ResponseModule.success(dataModule, "ok");
//		});
//	}
	
//	private class BeforeGroupDelete implements ServiceBefore {
//		List<Serializable> ids;
//		BeforeGroupDelete(List<Serializable> ids) {
//			this.ids = ids;
//		}
//		public void doBefore() {
//			this.ids.forEach(id -> {
//				Group group = groupService.getById(Long.valueOf(id.toString())).get();
//				Group parent = group.getParent();
//				while(parent != null) {
//					parent.setOffspringNumber(parent.getOffspringNumber() - 1);
//					groupService.saveOrUpdate(parent);
//					parent = parent.getParent();
//				}
//			});
//		}
//	}
//	
//	private class AfterGroupCreate implements ServiceAfter {
//		protected  Module module;
//		protected  ActionData actionData;
//		protected AfterGroupCreate(Module module, ActionData actionData) {
//			this.module = module;
//			this.actionData = actionData;
//		}
//		@Override
//		public void doAfter() {
//			
//		}
//	}
}
