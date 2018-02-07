# mySpringMvc
> 简单时模拟实现springmvc前端控制器，ioc容器管理注入功能

### springmvc运行大致分为以下阶段 

 - #### 配置阶段
    - web.xml
    - DispatcherServlet
    - application.xml
 - #### 运行阶段（初始化）
    - 读取application.xml，实现ioc
    - 九大组件初始化init方法
    - DI/AOP
 - #### 运行阶段（用户请求）
    - 浏览器输入url地址请求
    - doGet/doPost方法
    - 找到URL对应的方法 @Controller；@Request Mapping方法
    - 利用反射机制动态执行该方法，并将结果通过response对象输到客户端。