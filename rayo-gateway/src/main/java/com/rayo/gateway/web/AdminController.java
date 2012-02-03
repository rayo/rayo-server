package com.rayo.gateway.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AdminController {

    @RequestMapping(value="/index")
    public String dashboard() {

    	return "admin";
    }
        
	@RequestMapping("/platforms/{platform}")
	public ModelAndView platformHandler(@PathVariable("platform") String platform) {
		ModelAndView mav = new ModelAndView("platforms");
		mav.addObject("platform", platform);
		return mav;
	}

	@RequestMapping("/nodes/{node:.+}")
	public ModelAndView nodeHandler(@PathVariable("node") String node) {
		ModelAndView mav = new ModelAndView("nodes");
		mav.addObject("node", node);
		return mav;
	}

    @RequestMapping(value="/applications/{application}")
    public String application(@PathVariable("application") String application, ModelMap model) {

    	model.addAttribute("application", application);
    	return "applications";
    }
}
