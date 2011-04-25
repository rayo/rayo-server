package com.tropo.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.XmppServlet;

public class SpringXmppServlet extends XmppServlet {

    private Loggerf log = Loggerf.getLogger(SpringXmppServlet.class);

    static String INIT_PARAMETER = "xmpp-servlet";

    private XmppServlet target;

    public void init() throws ServletException {

        String wrappedServletName = getServletName();

        // Parent Context loaded by ContextLoaderListener
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        XmlWebApplicationContext xmppContext = new XmlWebApplicationContext();
        xmppContext.setServletContext(getServletContext());
        xmppContext.setParent(wac);
        xmppContext.setConfigLocation("/WEB-INF/" + getServletName() + "-xmpp.xml");
        xmppContext.refresh();

        Object bean = xmppContext.getBean(wrappedServletName);

        if (!(bean instanceof XmppServlet)) {
            throw new IllegalArgumentException("Servlet has to be an instance of XMPPServlet");
        }

        target = (XmppServlet) bean;

        target.init(getServletConfig());
    }

    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        try {
            this.target.service(request, response);
        } catch (HttpRequestMethodNotSupportedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void destroy() {
        if (target != null) {
            target.destroy();
        }
    }
}
