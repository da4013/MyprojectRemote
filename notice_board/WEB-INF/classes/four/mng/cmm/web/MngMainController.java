/** ===============================================
 * Copyright(c) 4DEPTH 2017
 * @fileName MngMainController.java
 * @author Chang-il Jeon
 * @since 2017. 2. 1.
 * @version 1.0
 * 
 *=================================================
 */
package four.mng.cmm.web;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UrlPathHelper;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.rte.fdl.security.userdetails.util.EgovUserDetailsHelper;
import four.common.util.security.tem4UserDetails;

@Controller
public class MngMainController {
	
	/** EgovMessageSource */
	@Resource(name = "egovMessageSource")
	EgovMessageSource egovMessageSource;

	
	@RequestMapping(value = {"/index.do"})
	public String frontMain(Model model){
		System.out.println("사용자 메인");
		return "front/index";
	}

	@RequestMapping(value = {"/mng/main.do"})
	public String MngMain(Model model){
		System.out.println("관리자 메인");
		return "mng/main";
	}
	
		
	   @RequestMapping(value = "/mng/index.do")
	    public String root() {
	        return "mng/index";
	    }
	   
	   @RequestMapping("/mng/left.do")
	    public String mngLeft(ModelMap model, HttpServletRequest request)throws Exception{
	       
	        LoginVO loginVO = new LoginVO();

	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	        if(auth.getDetails() instanceof tem4UserDetails) {
	            tem4UserDetails detail = (tem4UserDetails) auth.getDetails();
	            loginVO = detail.getLoginInfo();
	        }

	        String url = "";
	        String queryString = "";
	        UrlPathHelper urls = new UrlPathHelper();
	        url = urls.getOriginatingServletPath(request);
	        queryString =  urls.getOriginatingQueryString(request);

	        if(queryString != "" && queryString != null){
	        url+= "?"+queryString;
	        }
	        System.out.println("##############################");
	        System.out.println(url);
	        System.out.println("##############################");
	       
	        model.addAttribute("LoginVO",loginVO);
	        model.addAttribute("reqUrl",url);
	        return "mng/include/left";
	    }   
	   
	   
}

