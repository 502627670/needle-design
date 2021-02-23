package org.needleframe.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.needleframe.context.ModuleFactory.ActionFactory;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.ActionType;
import org.needleframe.core.model.Module;
import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.Setter;

public class ModuleActionBuilder {
	
	private ActionFactory af;
	
	public ModuleActionBuilder(ActionFactory af) {
		this.af = af;
	}
	
	public void buildDefaultActions(Collection<Module> modules) {
		modules.forEach(module -> {
			this.buildDefaultAction(module);
		});
	}
	
	protected void buildDefaultAction(Module module) {
		if(module.isCreatable()) {
			af.addAction(new CreateAction(module));
		}
		if(module.isUpdateable()) {
			af.addAction(new UpdateAction(module));
		}
		if(module.isRemoveable()) {
			af.addAction(new DeleteAction(module));
		}
		if(module.isImportable()) {
			af.addAction(new ImportAction(module));
		}
		if(module.isExportable()) {
			af.addAction(new ExportAction(module));
		}
	}
	
	@Getter
	@Setter
	protected static class CreateAction extends Action {
		private Act act = Act.CREATE;
		private CreateAction() {}
		private CreateAction(Module module) {
			this.setIdentity(String.join(".", module.getName(), "create"));
			this.setName("create");
			this.setUri("/" + module.getName() + "/create");
			this.setBatchable(false);
			this.addAct(module, Act.CREATE);
			this.setModule(module.getName());
			module.getActions().add(this);
		}
		
		public CreateAction clone() {
			CreateAction copy = new CreateAction();
			BeanUtils.copyProperties(this, copy);
			
			List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
			getActionTypes().forEach(at -> {
				copyOfActionTypes.add(at);
			});
			copy.setActionTypes(copyOfActionTypes);
			
			List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
			getProps().forEach(prop -> {
				copyOfProps.add(prop.clone());
			});
			copy.setProps(copyOfProps);
			
			return copy;
		}
	}
	
	@Getter
	@Setter
	protected static class UpdateAction extends Action {
		private Act act = Act.UPDATE;
		private UpdateAction() {}
		private UpdateAction(Module module) {
			this.setIdentity(String.join(".", module.getName(), "update"));
			this.setName("update");
			this.setUri("/" + module.getName() + "/update");
			this.setBatchable(false);
			this.addAct(module, Act.UPDATE);
			this.setModule(module.getName());
			module.getActions().add(this);
		}
		
		public UpdateAction clone() {
			UpdateAction copy = new UpdateAction();
			BeanUtils.copyProperties(this, copy);
			
			List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
			getActionTypes().forEach(at -> {
				copyOfActionTypes.add(at);
			});
			copy.setActionTypes(copyOfActionTypes);
			
			List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
			getProps().forEach(prop -> {
				copyOfProps.add(prop.clone());
			});
			copy.setProps(copyOfProps);
			
			return copy;
		}
	}
	
	@Getter
	@Setter
	protected static class DeleteAction extends Action {
		private Act act = Act.DELETE;
		private DeleteAction() {}
		private DeleteAction(Module module) {
			this.setIdentity(String.join(".", module.getName(), "delete"));
			this.setName("delete");
			this.setUri("/" + module.getName() + "/delete");
			this.setBatchable(true);
			this.addAct(module, Act.DELETE);
			this.setModule(module.getName());
			module.getActions().add(this);
		}
		public DeleteAction clone() {
			DeleteAction copy = new DeleteAction();
			BeanUtils.copyProperties(this, copy);
			
			List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
			getActionTypes().forEach(at -> {
				copyOfActionTypes.add(at);
			});
			copy.setActionTypes(copyOfActionTypes);
			
			List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
			getProps().forEach(prop -> {
				copyOfProps.add(prop.clone());
			});
			copy.setProps(copyOfProps);
			
			return copy;
		}
	}
	
	@Getter
	@Setter
	protected static class ImportAction extends Action {
		private Act act = Act.IMPORT;
		private ImportAction() {}
		private ImportAction(Module module) {
			this.setIdentity(String.join(".", module.getName(), "import"));
			this.setName("import");
			this.setUri("/" + module.getName() + "/saveImport");
			this.setBatchable(true);
			this.addAct(module, Act.IMPORT);
			this.setModule(module.getName());
			module.getActions().add(this);
		}
		
		public ImportAction clone() {
			ImportAction copy = new ImportAction();
			BeanUtils.copyProperties(this, copy);
			
			List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
			getActionTypes().forEach(at -> {
				copyOfActionTypes.add(at);
			});
			copy.setActionTypes(copyOfActionTypes);
			
			List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
			getProps().forEach(prop -> {
				copyOfProps.add(prop.clone());
			});
			copy.setProps(copyOfProps);
			
			return copy;
		}
	}
	
	@Getter
	@Setter
	protected static class ExportAction extends Action {
		private Act act = Act.EXPORT;
		private ExportAction() {}
		private ExportAction(Module module) {
			this.setIdentity(String.join(".", module.getName(), "export"));
			this.setName("export");
			this.setUri("/" + module.getName() + "/export");
			this.setBatchable(true);
			this.addAct(module, Act.EXPORT);
			this.setModule(module.getName());
			module.getActions().add(this);
		}
		
		public ExportAction clone() {
			ExportAction copy = new ExportAction();
			BeanUtils.copyProperties(this, copy);
			
			List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
			getActionTypes().forEach(at -> {
				copyOfActionTypes.add(at);
			});
			copy.setActionTypes(copyOfActionTypes);
			
			List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
			getProps().forEach(prop -> {
				copyOfProps.add(prop.clone());
			});
			copy.setProps(copyOfProps);
			
			return copy;
		}
	}
}
