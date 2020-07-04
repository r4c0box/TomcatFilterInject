# TomcatFilterInject

@desc1: 测试tomcat中如何动态添加filter

@desc2: Tomcat Filter动态注入

@page: [https://www.cnblogs.com/lxmwb/p/13235572.html](https://www.cnblogs.com/lxmwb/p/13235572.html)

@test：[TEST.md](https://github.com/cnsimo/TomcatFilterInject/blob/master/TEST.md)

## 前言

最近，看到好多不错的关于“无文件Webshell”的文章，对其中利用上下文动态的注入`Filter`的技术做了一下简单验证，写一下测试过程总结，不依赖任何框架，仅想学习一下tomcat的filter。

先放几篇大佬的文章：

- [Tomcat中一种半通用回显方法](https://xz.aliyun.com/t/7348)
- [tomcat结合shiro无文件webshell的技术研究以及检测方法](https://www.cnblogs.com/potatsoSec/p/13060261.html)
- [Tomcat通用回显学习](https://lucifaer.com/2020/05/12/Tomcat%E9%80%9A%E7%94%A8%E5%9B%9E%E6%98%BE%E5%AD%A6%E4%B9%A0/)
- [基于全局储存的新思路 | Tomcat的一种通用回显方法研究](https://mp.weixin.qq.com/s?__biz=MzIwNDA2NDk5OQ==&mid=2651374294&idx=3&sn=82d050ca7268bdb7bcf7ff7ff293d7b3)
- [threedr3am/ysoserial](https://github.com/threedr3am/ysoserial/blob/master/src/main/java/ysoserial/payloads/TomcatShellInject.java)

## Filter介绍

详细介绍略，简单记录一下我的理解：

- 过滤器（Filter）：用来对指定的URL进行过滤处理，类似`.net core`里的中间件，例如登录验证过滤器可以用来限制资源的未授权访问；
- 过滤链（FilterChain）：通过URL匹配动态将所有符合URL规则的过滤器共同组成一个过滤链，顺序有先后，类似`.net core`的管道，不过区别在于过滤链是单向的，管道是双向；

同Servlet，一般Filter的配置方式：

 - web.xml
 - @WebFilter修饰

## Filter注册调用流程

新建一个登录验证的Filter: SessionFilter.java

```java
package com.reinject.MyFilter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *    判断用户是否登录,未登录则退出系统
 */
@WebFilter(filterName = "SessionFilter", urlPatterns = "/*",
        initParams = {@WebInitParam(name = "logonStrings", value = "index.jsp;addFilter.jsp"),
                @WebInitParam(name = "includeStrings", value = ".jsp"),
                @WebInitParam(name = "redirectPath", value = "/index.jsp"),
                @WebInitParam(name = "disabletestfilter", value = "N")})
public class SessionFilter implements Filter {

    public FilterConfig config;

    public void destroy() {
        this.config = null;
    }

    public static boolean isContains(String container, String[] regx) {
        boolean result = false;

        for (int i = 0; i < regx.length; i++) {
            if (container.indexOf(regx[i]) != -1) {
                return true;
            }
        }
        return result;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hrequest = (HttpServletRequest)request;
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse) response);

        String logonStrings = config.getInitParameter("logonStrings");        // 登录登陆页面
        String includeStrings = config.getInitParameter("includeStrings");    // 过滤资源后缀参数
        String redirectPath = hrequest.getContextPath() + config.getInitParameter("redirectPath");// 没有登陆转向页面
        String disabletestfilter = config.getInitParameter("disabletestfilter");// 过滤器是否有效

        if (disabletestfilter.toUpperCase().equals("Y")) {    // 过滤无效
            chain.doFilter(request, response);
            return;
        }
        String[] logonList = logonStrings.split(";");
        String[] includeList = includeStrings.split(";");

        if (!this.isContains(hrequest.getRequestURI(), includeList)) {// 只对指定过滤参数后缀进行过滤
            chain.doFilter(request, response);
            return;
        }

        if (this.isContains(hrequest.getRequestURI(), logonList)) {// 对登录页面不进行过滤
            chain.doFilter(request, response);
            return;
        }

        String user = ( String ) hrequest.getSession().getAttribute("useronly");//判断用户是否登录
        if (user == null) {
            wrapper.sendRedirect(redirectPath);
            return;
        }else {
            chain.doFilter(request, response);
            return;
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
    }
}
```

观察一个正常请求的函数栈：

```
_jspService:14, index_jsp (org.apache.jsp)
service:70, HttpJspBase (org.apache.jasper.runtime)
service:731, HttpServlet (javax.servlet.http)
service:439, JspServletWrapper (org.apache.jasper.servlet)
serviceJspFile:395, JspServlet (org.apache.jasper.servlet)
service:339, JspServlet (org.apache.jasper.servlet)
service:731, HttpServlet (javax.servlet.http)
internalDoFilter:303, ApplicationFilterChain (org.apache.catalina.core)
doFilter:208, ApplicationFilterChain (org.apache.catalina.core)
doFilter:52, WsFilter (org.apache.tomcat.websocket.server)
internalDoFilter:241, ApplicationFilterChain (org.apache.catalina.core)
doFilter:208, ApplicationFilterChain (org.apache.catalina.core)
doFilter:66, SessionFilter (com.reinject.MyFilter)
internalDoFilter:241, ApplicationFilterChain (org.apache.catalina.core)
doFilter:208, ApplicationFilterChain (org.apache.catalina.core)
invoke:218, StandardWrapperValve (org.apache.catalina.core)
invoke:122, StandardContextValve (org.apache.catalina.core)
invoke:505, AuthenticatorBase (org.apache.catalina.authenticator)
invoke:169, StandardHostValve (org.apache.catalina.core)
invoke:103, ErrorReportValve (org.apache.catalina.valves)
invoke:956, AccessLogValve (org.apache.catalina.valves)
invoke:116, StandardEngineValve (org.apache.catalina.core)
service:442, CoyoteAdapter (org.apache.catalina.connector)
process:1082, AbstractHttp11Processor (org.apache.coyote.http11)
process:623, AbstractProtocol$AbstractConnectionHandler (org.apache.coyote)
run:316, JIoEndpoint$SocketProcessor (org.apache.tomcat.util.net)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:61, TaskThread$WrappingRunnable (org.apache.tomcat.util.threads)
run:748, Thread (java.lang)
```

找到最开始的`ApplicationFilterChain`位置，调用者是`StandardWrapperValve`的`invoke`，再观察`invoke`代码不难看出是用`ApplicationFilterFactory`动态生成的`ApplicationFilterChain`：

```java
// Create the filter chain for this request
ApplicationFilterFactory factory =
    ApplicationFilterFactory.getInstance();
ApplicationFilterChain filterChain =
    factory.createFilterChain(request, wrapper, servlet);
```

`createFilterChain`根据xml配置动态生成一个过滤链，部分代码如下：

```java
// Acquire the filter mappings for this Context
StandardContext context = (StandardContext) wrapper.getParent();
FilterMap filterMaps[] = context.findFilterMaps();

// If there are no filter mappings, we are done
if ((filterMaps == null) || (filterMaps.length == 0))
    return (filterChain);

// Acquire the information we will need to match filter mappings
String servletName = wrapper.getName();

// Add the relevant path-mapped filters to this filter chain
for (int i = 0; i < filterMaps.length; i++) {
    if (!matchDispatcher(filterMaps[i] ,dispatcher)) {
        continue;
    }
    if (!matchFiltersURL(filterMaps[i], requestPath))
        continue;
    ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
        context.findFilterConfig(filterMaps[i].getFilterName());
    if (filterConfig == null) {
        // FIXME - log configuration problem
        continue;
    }
    boolean isCometFilter = false;
    if (comet) {
        try {
            isCometFilter = filterConfig.getFilter() instanceof CometFilter;
        } catch (Exception e) {
            // Note: The try catch is there because getFilter has a lot of 
            // declared exceptions. However, the filter is allocated much
            // earlier
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(t);
        }
        if (isCometFilter) {
            filterChain.addFilter(filterConfig);
        }
    } else {
        filterChain.addFilter(filterConfig);
    }
}
```

所有的`filter`可以通过`context.findFilterMaps()`方法获取，FilterMap结构如下：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704104945.png)

`FilterMap`中存放了所有`filter`相关的信息包括`filterName`和`urlPattern`。

有了这些之后，使用`matchFiltersURL`函数将每个`filter`和当前`URL`进行匹配，匹配成功的通过`context.findFilterConfig`获取`filterConfig`，`filterConfig`结构如下：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704111009.png)

之后将`filterConfig`添加到`filterChain`中，最后回到`StandardWrapperValve`中调用`doFilter`进入过滤阶段。

这个图（@宽字节安全）能够很清晰的看到整个filter流程：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704113125.png)

通过上面的流程，可知所有的`filter`信息都是从`context（StandardContext）`获取到的，所以假如可以获取到这个`context`就可以通过反射的方式修改`filterMap`和`filterConfig`从而达到动态注册filter的目的。

## 获取context

打开`jconsole`，获取`tomcat`的`Mbean`:

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704120332.png)

感觉其中好多地方都可以获取到`context`，比如`RequestProcessor`、`Resource`、`ProtocolHandler`、`WebappClassLoader`、`Value`。

### Value获取

代码：

```java
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
```

反射弧：`mBeanServer->mbsInterceptor->repository->domainTb->nonLoginAuthenticator->resource->context`。

## 通过StandardContext注册filter

通过filter流程分析可知，注册filter需要两步：

- 修改`filterConfigs`；
- 将filter插到`filterMaps`的`0`位置；

在此之前，先看一下我们比较关心的context中三个成员变量：
 
 - filterConfigs：filterConfig的数组
 - filterRefs：filterRef的数组
 - filterMaps：filterMap的数组

 `filterConfig`的结构之前看过，`filterConfig.filterRef`实际和`context.filterRef`指向的地址一样：
 
 Expression: `((StandardContext) context).filterConfigs.get("SessionFilter").filterDef == ((StandardContext) context).filterDefs.get("SessionFilter");`

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704131645.png)

从`StandardContext`类的方法看，可以调用`StandardContext.addFilterDef()`修改`filterRefs`，然后调用`StandardContext.filterStart()`函数会自动根据`filterDefs`重新生成`filterConfigs`：

```java
filterConfigs.clear();
for (Entry<String, FilterDef> entry : filterDefs.entrySet()) {
    String name = entry.getKey();
    if (getLogger().isDebugEnabled())
        getLogger().debug(" Starting filter '" + name + "'");
    ApplicationFilterConfig filterConfig = null;
    try {
        filterConfig =
            new ApplicationFilterConfig(this, entry.getValue());
        filterConfigs.put(name, filterConfig);
    } catch (Throwable t) {
        t = ExceptionUtils.unwrapInvocationTargetException(t);
        ExceptionUtils.handleThrowable(t);
        getLogger().error
            (sm.getString("standardContext.filterStart", name), t);
        ok = false;
    }
}
```

综上，修改`filterRefs`和`filterConfigs`的代码如下：

```java
// Gen filterDef
filterDef = new FilterDef();
filterDef.setFilterName(filterName);
filterDef.setFilterClass(filter.getClass().getName());
filterDef.setFilter(filter);
// Add filterDef
context.addFilterDef(filterDef);
// Refresh filterConfigs
context.filterStart();
```

改`filterMaps`就简单了，添加上去改一下顺序加到`0`位置：

```java
// filterMap
filterMap.setFilterName(filterName);
filterMap.setDispatcher(String.valueOf(DispatcherType.REQUEST));
filterMap.addURLPattern(filterUrlPatern);
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
```

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704143054.png)

## 通过ApplicationContext注册filter

多次调试发现有多处context，上面一直用的都是`StandardContext`，观察该结构发现还有一个私有变量`context`，类型为`ApplicationContext`，通过他的定义发现其实就是一个`ServletContext`：

```java
public class ApplicationContext implements ServletContext {
}
```

该结构中也有一些`filter`操作的方法：

```java
public Map<String, ? extends FilterRegistration> getFilterRegistrations() {}
public FilterRegistration getFilterRegistration(String filterName) {}
public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {} 
```

这三个函数返回值都是`FilterRegistration`，看一下结构：

```java
public class ApplicationFilterRegistration implements FilterRegistration.Dynamic {
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {}
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {}
    public Collection<String> getServletNameMappings() {}
    public Collection<String> getUrlPatternMappings() {}
    public String getClassName() {}
    public String getInitParameter(String name) {}
    public Map<String, String> getInitParameters() {}
    public String getName() {}
    public boolean setInitParameter(String name, String value) {}
    public Set<String> setInitParameters(Map<String, String> initParameters) {}
    public void setAsyncSupported(boolean asyncSupported) {}
}
```

很明显打包了一些常用的注册`Filter`的函数，所以可以使用`ApplicationContext`和`FilterRegistration`进行注册，测试代码如下：

```java
// Define
ApplicationContext applicationContext = new ApplicationContext(standardContext);
Filter filter = new TestApplicationContextAddFilter();
// Registe Filter
FilterRegistration.Dynamic filterRegistration = applicationContext.addFilter(filterName, filter);
// Create Map for urlPattern
filterRegistration.addMappingForUrlPatterns(EnumSet.of(javax.servlet.DispatcherType.REQUEST), false, new String[]{urlPatern});
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
```

很不幸，有`IllegalStateException`异常：

```
严重: Servlet.service() for servlet [HelloWorldServlet] in context with path [] threw exception [Servlet execution threw an exception] with root cause
java.lang.IllegalStateException: Filters can not be added to context  as the context has been initialised
	at org.apache.catalina.core.ApplicationContext.addFilter(ApplicationContext.java:1005)
	at org.apache.catalina.core.ApplicationContext.addFilter(ApplicationContext.java:970)
	at com.reinject.test.TestApplicationContextAddFilter.<clinit>(TestApplicationContextAddFilter.java:61)
	at com.reinject.MyServlet.HelloWorldServlet.doGet(HelloWorldServlet.java:50)
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:624)
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:731)
```

通过观察`AddFilter`报错的位置，发现是对`standardContext`的`state`校验的时候不达标抛出的异常：

```java
if (!context.getState().equals(LifecycleState.STARTING_PREP)) {
    //TODO Spec breaking enhancement to ignore this restriction
    throw new IllegalStateException(
            sm.getString("applicationContext.addFilter.ise",
                    getContextPath()));
}
```

那么可以先修改一下`state`为`LifecycleState.STARTING_PREP`:

```java
java.lang.reflect.Field stateField = org.apache.catalina.util.LifecycleBase.class.getDeclaredField("state");
stateField.setAccessible(true);
stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTING_PREP);
```

再运行正常：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704155408.png)

不过测试发现如果`state`不改回来，之后访问所有页面都会`503`：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704155611.png)

综上：

```java
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
```

## 实验过程中的代码

[获取](https://github.com/cnsimo/TomcatFilterInject.git) 方式，`git clone https://github.com/cnsimo/TomcatFilterInject.git`

部署方式，`idea + tomcat7.0.70`。

添加`tomcat7.0.70/lib`为依赖。

