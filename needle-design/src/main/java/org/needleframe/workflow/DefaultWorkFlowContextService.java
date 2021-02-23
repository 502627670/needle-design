package org.needleframe.workflow;

import org.needleframe.AbstractContextService;
import org.needleframe.context.ModuleFactory;
import org.needleframe.context.ModuleFactory.ActionFactory;
import org.needleframe.context.ModuleFactory.MenuFactory;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.workflow.domain.Message;
import org.needleframe.workflow.domain.Step;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.domain.WorkFlow;
import org.needleframe.workflow.domain.WorkNode;

public class DefaultWorkFlowContextService extends AbstractContextService implements WorkFlowContextService {

	@Override
	protected void defModules(ModuleFactory mf) {
		mf.build(WorkFlow.class).showName("流程定义").addCRUD()
//			.addUnique("module")
			.fk("reporterUser").map("fullname", "reporterName").show("username").name("报告人")
			.prop("reporterName").name("报告人姓名")
			.prop("title").name("标题").inRow()
			.prop("module").name("业务项").hide().showForm().showEdit()
			.prop("moduleName").name("业务项").show().hideForm().hideEdit()
			.end()
			
			.addChild(WorkNode.class).showName("流程节点").addCRUD()
			.fk("workFlow").show("title").name("工作流程").hideList().end()
			.fk("assigneeUser").map("fullname", "assigneeName").show("username").name("接收人").required().end()
			.prop("name").name("节点名称").required()
			.prop("prop").name("触发属性").hide().showForm().showEdit().required()
			.prop("propName").name("触发属性名").show().hideForm().hideEdit()
			.prop("fromValue").name("触发属性值").required()
			.prop("nextValue").name("完成后属性值")
			.prop("assigneeName").name("接收人姓名").show().hideForm().hideEdit()
			.prop("sortOrder").name("节点序列").required();
			
		mf.build(Task.class).showName("工作任务").addCRUD()
			.showList("subtitle", "assigneeUser", "beginDate", "completeDate", "taskStatus")
			.filters("subtitle", "taskStatus").eq()
			.filters("assignDate", "beginDate", "completeDate").ops(Op.EQUAL, Op.LARGE_EQUAL, Op.LARGE, Op.LESS_EQUAL, Op.LESS)
			.prop("subtitle").name("任务标题").inRow().required()
			.prop("workFlow").name("流程定义").fk().show("title").hide()
			.prop("workNode").name("流程节点").fk().show("name").hide()
			.prop("assignDate").name("分配日期").dateTime()
			
			.prop("assigneeUser").hide().showForm().showEdit().fk().map("fullname", "assignee").show("username").required()
			.prop("reporterUser").hide().showForm().showEdit().fk().map("fullname", "reporter").show("username").rule().noUpdate().required()
			.prop("reporter").name("报告用户名").show().hideForm().hideEdit().rule().noUpdate()
			.prop("assignee").name("接收用户名").show().hideForm().hideEdit()
			.prop("beginDate").name("开始日期").date()
			.prop("completeDate").name("完成日期").date()
			.prop("taskStatus").name("任务状态")
			
			.prop("module").name("业务项目").hide()
			.prop("instanceId").name("业务实例").hide()
			.prop("instancePk").name("实例主键").hide()
			.prop("instanceTitle").name("关联业务").hide().end()
			.prop("nextTaskId").name("下一步").hide()
			.prop("prevTaskId").name("上一步").hide().end()
			
			.addChild(Step.class).addAct(Act.CREATE).showName("处理")
			.prop("task").name("任务").fk().cascadeDelete().show("subtitle").hide()
			.prop("content").name("备注").text().showList()
			.prop("stepStatus").name("处理状态").required()
			.prop("submitter").name("提交人").hide().showList()
			.prop("submitDate").name("提交日期").hide().showList()
			.prop("module").name("业务项目").hide()
			.prop("instanceId").name("业务实例").hide()
			.prop("instancePk").name("实例主键").hide()
			.prop("instanceTitle").name("关联业务").hide().showInfo()
			.and();
		
		mf.build(Message.class).showName("消息").addCRUD()
			.filters("title", "assignDate", "messageStatus").eq()
			.prop("title").name("标题")
			.prop("link").name("链接")
			.prop("assignDate").name("分配日期").date()
			.prop("messageStatus").name("消息状态").end()
			.prop("assigneeUser").name("接收用户").fk().show("username")
			.prop("assignee").name("接收人");
	}
	
	protected void defActions(ActionFactory af) {
		
	}
	
	protected void defMenus(MenuFactory mf) {
//		mf.build("工作流程", -1).uri("/work").icon("el-icon-s-operation")
//			.addItem(WorkFlow.class)
//				.name("流程定义")
//				.uri("/workFlow")
//				.icon("el-icon-s-unfold")
//				
//			.addItem(Task.class)
//				.name("发出任务")
//				.uri("/sendTask")
//				.icon("el-icon-date")
//			
//			.addItem(Task.class)
//				.uri("/myTask")
//				.name("我的任务")
//				.icon("el-icon-date");
	}
	
}
