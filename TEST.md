# 测试方式

## 注入Filter

站点启动成功之后，访问`http://localhost:8080/addFilter.jsp`

显示`success`表示注入成功：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704165802.png)

否则，代表失败。

## 测试利用

在任意`URL`后添加`GET`参数`cmd=hostname`，有结果表示成功：

![](https://cdn.jsdelivr.net/gh/cnsimo/pic_bed/20200704170044.png)

## 测试StandardContext

借用`com.reinject.MyServlet.HelloWorldServlet`中实例化`com.reinject.test.TestAddFilter`，通过访问`/hello`结合断点调试进行测试。

## 测试ApplicationContext

借用`com.reinject.MyServlet.HelloWorldServlet`中实例化`com.reinject.test.TestApplicationContextAddFilter`，通过访问`/hello`结合断点调试进行测试。

## 测试MBean获取Context

配合`jconsole`。

借用`com.reinject.MyServlet.HelloWorldServlet`中实例化`com.reinject.test.TestMBeanGetContext`，通过访问`/hello`结合断点调试进行测试。

## 自定义Webshell

修改`com.reinject.MyFilter.TomcatShellFilter`中`doFilter`内容。

用冰蝎可能需要改一些东西，还没看，可参考[冰蝎改造之适配基于tomcat Filter的无文件webshell](https://www.cnblogs.com/potatsoSec/p/13098595.html)