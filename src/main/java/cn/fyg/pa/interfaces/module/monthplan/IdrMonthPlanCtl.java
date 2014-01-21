package cn.fyg.pa.interfaces.module.monthplan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import cn.fyg.pa.application.IdrMonthPlanBillService;
import cn.fyg.pa.domain.model.department.Department;
import cn.fyg.pa.domain.model.department.DepartmentRepository;
import cn.fyg.pa.domain.model.deptmonthplan.IdrMonthPlanBill;
import cn.fyg.pa.domain.model.deptmonthplan.IdrMonthPlanBillRepository;
import cn.fyg.pa.domain.model.deptmonthplan.IdrMonthPlanEnum;
import cn.fyg.pa.domain.model.deptmonthplan.IdrTask;
import cn.fyg.pa.domain.model.person.Person;
import cn.fyg.pa.domain.model.person.PersonRepository;
import cn.fyg.pa.domain.shared.state.StateChangeException;
import cn.fyg.pa.interfaces.module.shared.message.impl.SessionMPR;
import cn.fyg.pa.interfaces.module.shared.personin.annotation.PersonIn;
import cn.fyg.pa.interfaces.module.shared.tool.DateTool;

@Controller
@RequestMapping("/monthplan")
public class IdrMonthPlanCtl {
	
	private static final Logger logger=LoggerFactory.getLogger(IdrMonthPlanCtl.class);
	
	private static final String PATH="idrmonthplan/";
	private interface Page {
		String EDIT     = PATH + "edit";
		String VIEW = PATH + "view";
		String SUMMARY  = PATH + "summary";
	}
	
	public static Map<IdrMonthPlanEnum,String> PAGEMAP=new HashMap<IdrMonthPlanEnum,String>();
	static{
		PAGEMAP.put(IdrMonthPlanEnum.NEW, Page.EDIT);
		PAGEMAP.put(IdrMonthPlanEnum.SAVED, Page.EDIT);
		PAGEMAP.put(IdrMonthPlanEnum.SUBMITTED, Page.VIEW);
		PAGEMAP.put(IdrMonthPlanEnum.EXECUTE, Page.SUMMARY);
		PAGEMAP.put(IdrMonthPlanEnum.FINISHED, Page.VIEW);
	}
	
	@Resource
	PersonRepository personRepository;
	@Resource
	IdrMonthPlanBillRepository idrMonthPlanBillRepository;
	@Resource
	IdrMonthPlanBillService idrMonthPlanBillService; 
	@Resource
	DepartmentRepository departmentRepository;
	
	@RequestMapping(value="",method=RequestMethod.GET)
	@PersonIn(0)
	public String toEdit(Person person,Map<String,Object> map,HttpSession session){
		String departmentName=person.getDepartment();
		Department department=departmentRepository.findDepartmentByName(departmentName);
		IdrMonthPlanBill idrMonthPlanBill=idrMonthPlanBillService.getLastIdrMonthPlanBill(department);
		map.put("person", person);
		map.put("idrMonthPlanBill", idrMonthPlanBill);
		map.put("message", new SessionMPR(session).getMessage());
		return PAGEMAP.get(idrMonthPlanBill.getState());
	}
	
	@RequestMapping(value="/save",method=RequestMethod.POST)
	public String save(IdrMonthPlanBill idrMonthPlanBill,HttpSession session){
		idrMonthPlanBill.setState(IdrMonthPlanEnum.SAVED);
		idrMonthPlanBill=idrMonthPlanBillService.save(idrMonthPlanBill);
		new SessionMPR(session).setMessage("保存成功！");
		return "redirect:/monthplan";
	}
	
	@RequestMapping(value="/commit",method=RequestMethod.POST)
	public String commit(IdrMonthPlanBill idrMonthPlanBill,HttpSession session){
		idrMonthPlanBill.setState(IdrMonthPlanEnum.SAVED);
		idrMonthPlanBill=idrMonthPlanBillService.save(idrMonthPlanBill);
		String message="提交成功！";
		try {
			idrMonthPlanBillService.next(idrMonthPlanBill.getId());
		} catch (StateChangeException e) {
			message=String.format("提交失败，原因：%s", e.getMessage());
		}
		new SessionMPR(session).setMessage(message);
		return "redirect:/monthplan";
	}
	
	@RequestMapping(value="/{idrmonthplanId}/summary",method=RequestMethod.POST)
	public String summary(@PathVariable("idrmonthplanId")Long idrmonthplanId,HttpServletRequest request,HttpSession session){
		String message="保存成功！";
		IdrMonthPlanBill idrMonthPlanBill=idrMonthPlanBillRepository.find(idrmonthplanId);
		//TODO 此处有待进一步修改
		Iterator<IdrTask> iterator=idrMonthPlanBill.getIdrTasks().iterator();
		while(iterator.hasNext()){
			IdrTask idrTask=iterator.next();
			if(idrTask.getContext()==null){
				iterator.remove();
			}
		}
        ServletRequestDataBinder binder = new ServletRequestDataBinder(idrMonthPlanBill);  
        binder.bind(request);  
        idrMonthPlanBillService.save(idrMonthPlanBill);
		new SessionMPR(session).setMessage(message);
		return "redirect:/monthplan";
	}
	
	@RequestMapping(value="/{idrmonthplanId}/finish",method=RequestMethod.POST)
	public String finish(@PathVariable("idrmonthplanId")Long idrmonthplanId,HttpServletRequest request,HttpSession session){
		String message="工作计划完成！";
		IdrMonthPlanBill idrMonthPlanBill=idrMonthPlanBillRepository.find(idrmonthplanId);
		//TODO 此处有待进一步修改
		Iterator<IdrTask> iterator=idrMonthPlanBill.getIdrTasks().iterator();
		while(iterator.hasNext()){
			IdrTask idrTask=iterator.next();
			if(idrTask.getContext()==null){
				iterator.remove();
			}
		}
		ServletRequestDataBinder binder = new ServletRequestDataBinder(idrMonthPlanBill);  
        binder.bind(request);  
        idrMonthPlanBill=idrMonthPlanBillService.save(idrMonthPlanBill);
        try {
			idrMonthPlanBillService.next(idrMonthPlanBill.getId());
		} catch (StateChangeException e) {
			logger.error("", e);
			message=String.format("工作计划完成失败，原因：%s", e.getMessage());
		}
        new SessionMPR(session).setMessage(message);
		return "redirect:/monthplan";
	}
	
	@RequestMapping(value="/history",method=RequestMethod.GET)
	@PersonIn(1)
	public String history(YearQueryBean queryBean,Person person,Map<String,Object> map,HttpSession session){
		String departmentName=person.getDepartment();
		Department department=departmentRepository.findDepartmentByName(departmentName);
		List<IdrMonthPlanBill> idrMonthPlanBills=idrMonthPlanBillRepository.findIdrMonthPlanBillByPeriodAndDepartmentAndState(queryBean.getYear(),null,department,IdrMonthPlanEnum.FINISHED);
		map.put("dateTool", new DateTool());
		map.put("queryBean", queryBean);
		map.put("person", person);
		map.put("idrMonthPlanBills", idrMonthPlanBills);
		return "idrmonthplan/histroy";
	}

}