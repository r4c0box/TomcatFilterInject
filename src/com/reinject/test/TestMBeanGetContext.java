package com.reinject.test;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.modeler.BaseModelMBean;
import org.apache.tomcat.util.modeler.Registry;
import com.sun.jmx.interceptor.DefaultMBeanServerInterceptor;

import javax.management.MBeanServer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TestMBeanGetContext {

    public void getContext() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
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
        HashMap<String,Map<String, NamedObject>> domainTb = (HashMap<String,Map<String,NamedObject>>)field.get(repository);
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

        System.out.println("tset");
        //StandardContext standardContext =
        //repository.domainTb.get("Catalina").get("context=/samples_web_war,host=localhost,name=NonLoginAuthenticator,type=Valve").object.resource.context;
    }
}
