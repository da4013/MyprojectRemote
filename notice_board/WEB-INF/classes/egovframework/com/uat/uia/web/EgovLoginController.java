package egovframework.com.uat.uia.web;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UrlPathHelper;

import com.google.gson.Gson;
import com.sun.mail.handlers.message_rfc822;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.EgovComponentChecker;
import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.annotation.IncludedInfo;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.cmm.service.Globals;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.com.uat.uia.service.EgovLoginService;
import four.common.util.security.tem4UserDetails;

/*
import com.gpki.gpkiapi.cert.X509Certificate;
import com.gpki.servlet.GPKIHttpServletRequest;
import com.gpki.servlet.GPKIHttpServletResponse;
*/

/**
 * 일반 로그인, 인증서 로그인을 처리하는 컨트롤러 클래스
 * @author 공통서비스 개발팀 박지욱
 * @since 2009.03.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자          수정내용
 *  -------    --------    ---------------------------
 *  2009.03.06  박지욱          최초 생성
 *  2011.8.26	정진오			IncludedInfo annotation 추가
 *  2011.09.07  서준식          스프링 시큐리티 로그인 및 SSO 인증 로직을 필터로 분리
 *  2011.09.25  서준식          사용자 관리 컴포넌트 미포함에 대한 점검 로직 추가
 *  2011.09.27  서준식          인증서 로그인시 스프링 시큐리티 사용에 대한 체크 로직 추가
 *  2011.10.27  서준식          아이디 찾기 기능에서 사용자 리름 공백 제거 기능 추가
 *  </pre>
 */
@Controller
public class EgovLoginController {

	/** EgovLoginService */
	@Resource(name = "loginService")
	private EgovLoginService loginService;

	/** EgovCmmUseService */
	@Resource(name = "EgovCmmUseService")
	private EgovCmmUseService cmmUseService;

	/** EgovMessageSource */
	@Resource(name = "egovMessageSource")
	EgovMessageSource egovMessageSource;

	/** log */
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovLoginController.class);

	/**
	 * 로그인 화면으로 들어간다
	 * @param vo - 로그인후 이동할 URL이 담긴 LoginVO
	 * @return 로그인 페이지
	 * @exception Exception
	 */
	@IncludedInfo(name = "로그인", listUrl = "/uat/uia/egovLoginUsr.do", order = 10, gid = 10)
	@RequestMapping(value = "/uat/uia/egovLoginUsr.do")
	public String loginUsrView(@ModelAttribute("loginVO") LoginVO loginVO, HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Exception {
		if (EgovComponentChecker.hasComponent("mberManageService")) {
			model.addAttribute("useMemberManage", "true");
		}
		/*
		GPKIHttpServletResponse gpkiresponse = null;
		GPKIHttpServletRequest gpkirequest = null;

		try{

			gpkiresponse=new GPKIHttpServletResponse(response);
		    gpkirequest= new GPKIHttpServletRequest(request);
		    gpkiresponse.setRequest(gpkirequest);
		    model.addAttribute("challenge", gpkiresponse.getChallenge());
		    return "egovframework/com/uat/uia/EgovLoginUsr";

		}catch(Exception e){
		    return "egovframework/com/cmm/egovError";
		}
		*/

		return "egovframework/com/uat/uia/EgovLoginUsr";
	}

	/**
	 * 일반(세션) 로그인을 처리한다
	 * @param vo - 아이디, 비밀번호가 담긴 LoginVO
	 * @param request - 세션처리를 위한 HttpServletRequest
	 * @return result - 로그인결과(세션정보)
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/actionLogin.do")
	public String actionLogin(@ModelAttribute("loginVO") LoginVO loginVO, HttpServletRequest request, ModelMap model) throws Exception {

		// 1. 일반 로그인 처리
		LoginVO resultVO = loginService.actionLogin(loginVO);

		if (resultVO != null && resultVO.getId() != null && !resultVO.getId().equals("")) {

			// 2-1. 로그인 정보를 세션에 저장
			request.getSession().setAttribute("loginVO", resultVO);

			return "redirect:/uat/uia/actionMain.do";

		} else {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "egovframework/com/uat/uia/EgovLoginUsr";
		}
	}

	/**
	 * 로그인 후 메인화면으로 들어간다
	 * @param
	 * @return 로그인 페이지
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/actionMain.do")
	public String actionMain(ModelMap model) throws Exception {

		// 1. Spring Security 사용자권한 처리
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		if (!isAuthenticated) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "egovframework/com/uat/uia/EgovLoginUsr";
		}
		LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		
		LOGGER.debug("User Id : {}", user.getId());

		/*
		// 2. 메뉴조회
		MenuManageVO menuManageVO = new MenuManageVO();
		menuManageVO.setTmp_Id(user.getId());
		menuManageVO.setTmp_UserSe(user.getUserSe());
		menuManageVO.setTmp_Name(user.getName());
		menuManageVO.setTmp_Email(user.getEmail());
		menuManageVO.setTmp_OrgnztId(user.getOrgnztId());
		menuManageVO.setTmp_UniqId(user.getUniqId());
		List list_headmenu = menuManageService.selectMainMenuHead(menuManageVO);
		model.addAttribute("list_headmenu", list_headmenu);
		*/

		// 3. 메인 페이지 이동
		String main_page = Globals.MAIN_PAGE;

		LOGGER.debug("Globals.MAIN_PAGE > " + Globals.MAIN_PAGE);
		LOGGER.debug("main_page > {}", main_page);

		if (main_page.startsWith("/")) {
			return "forward:" + main_page;
		} else {
			return main_page;
		}

		/*
		if (main_page != null && !main_page.equals("")) {

			// 3-1. 설정된 메인화면이 있는 경우
			return main_page;

		} else {

			// 3-2. 설정된 메인화면이 없는 경우
			if (user.getUserSe().equals("USR")) {
				return "egovframework/com/EgovMainView";
			} else {
				return "egovframework/com/EgovMainViewG";
			}
		}
		*/
	}

	/**
	 * 로그아웃한다.
	 * @return String
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/actionLogout.do")
	public String actionLogout(HttpServletRequest request, ModelMap model) throws Exception {

		/*String userIp = EgovClntInfo.getClntIP(request);

		// 1. Security 연동
		return "redirect:/j_spring_security_logout";*/

		request.getSession().setAttribute("loginVO", null);

		//return "redirect:/egovDevIndex.jsp";
		return "redirect:/uat/uia/egovLoginUsr.do";
	}

	/**
	 * 아이디/비밀번호 찾기 화면으로 들어간다
	 * @param
	 * @return 아이디/비밀번호 찾기 페이지
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/egovIdPasswordSearch.do")
	public String idPasswordSearchView(ModelMap model) throws Exception {

		// 1. 비밀번호 힌트 공통코드 조회
		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCode("COM022");
		List<?> code = cmmUseService.selectCmmCodeDetail(vo);
		model.addAttribute("pwhtCdList", code);

		return "egovframework/com/uat/uia/EgovIdPasswordSearch";
	}

	/**
	 * 아이디를 찾는다.
	 * @param vo - 이름, 이메일주소, 사용자구분이 담긴 LoginVO
	 * @return result - 아이디
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/searchId.do")
	public String searchId(@ModelAttribute("loginVO") LoginVO loginVO, ModelMap model) throws Exception {

		if (loginVO == null || loginVO.getName() == null || loginVO.getName().equals("") && loginVO.getEmail() == null || loginVO.getEmail().equals("")
				&& loginVO.getUserSe() == null || loginVO.getUserSe().equals("")) {
			return "egovframework/com/cmm/egovError";
		}

		// 1. 아이디 찾기
		loginVO.setName(loginVO.getName().replaceAll(" ", ""));
		LoginVO resultVO = loginService.searchId(loginVO);

		if (resultVO != null && resultVO.getId() != null && !resultVO.getId().equals("")) {

			model.addAttribute("resultInfo", "아이디는 " + resultVO.getId() + " 입니다.");
			return "egovframework/com/uat/uia/EgovIdPasswordResult";
		} else {
			model.addAttribute("resultInfo", egovMessageSource.getMessage("fail.common.idsearch"));
			return "egovframework/com/uat/uia/EgovIdPasswordResult";
		}
	}

	/**
	 * 비밀번호를 찾는다.
	 * @param vo - 아이디, 이름, 이메일주소, 비밀번호 힌트, 비밀번호 정답, 사용자구분이 담긴 LoginVO
	 * @return result - 임시비밀번호전송결과
	 * @exception Exception
	 */
	@RequestMapping(value = "/uat/uia/searchPassword.do")
	public String searchPassword(@ModelAttribute("loginVO") LoginVO loginVO, ModelMap model) throws Exception {

		if (loginVO == null || loginVO.getId() == null || loginVO.getId().equals("") && loginVO.getName() == null || loginVO.getName().equals("") && loginVO.getEmail() == null
				|| loginVO.getEmail().equals("") && loginVO.getPasswordHint() == null || loginVO.getPasswordHint().equals("") && loginVO.getPasswordCnsr() == null
				|| loginVO.getPasswordCnsr().equals("") && loginVO.getUserSe() == null || loginVO.getUserSe().equals("")) {
			return "egovframework/com/cmm/egovError";
		}

		// 1. 비밀번호 찾기
		boolean result = loginService.searchPassword(loginVO);

		// 2. 결과 리턴
		if (result) {
			model.addAttribute("resultInfo", "임시 비밀번호를 발송하였습니다.");
			return "egovframework/com/uat/uia/EgovIdPasswordResult";
		} else {
			model.addAttribute("resultInfo", egovMessageSource.getMessage("fail.common.pwsearch"));
			return "egovframework/com/uat/uia/EgovIdPasswordResult";
		}
	}
	
	@RequestMapping(value = "/com/loginConfirm.do")
	@ResponseBody
    public String loginConfirm(
			HttpServletResponse response) throws Exception {
		// 결과 상태값 셋팅
		HashMap result = new HashMap();
		LoginVO user = new LoginVO();
		try {
			
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	        if(auth.getDetails() instanceof tem4UserDetails) {
	        	tem4UserDetails detail = (tem4UserDetails) auth.getDetails();
	        	user = detail.getLoginInfo();
	        }	
			
	    	if ( user.getRole() == null ) {
	    		throw new Exception("회원전용 서비스입니다.\n로그인후 진행주세요.");
			}
	    	result.put("result_cd", "200");
	    	result.put("result_msg", "Success");
		} catch (Exception e) {
			result.put("result_cd", "500");
			result.put("result_msg", e.getMessage());
		} finally {
			return new Gson().toJson(result);
        }
    }

}