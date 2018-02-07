package com.fkedu.mvcframework.servlet;

import com.fkedu.mvcframework.annotation.*;
import com.sun.corba.se.spi.ior.ObjectKey;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**SpringMvc核心路由，初始化容器，处理就请求转发
 * Created by fk on 2018/1/22.
 */
public class FKDispatherServlet extends HttpServlet {

    private Properties p = new Properties();

    //包扫描到的所有类容器
    private List<String> classNames = new ArrayList<String>();

    //key        beanName Spring中所描述的beanid，value 实例，对象
    private Map<String, Object> ioc = new HashMap<>();

    //private Map<String, Method> handlerMapping = new HashMap<>();
    //保存所有的url和方法和对象的映射关系，所以key value不够，需要使用Handler内部类对象保存
    private List<Handler> handlerMapping = new ArrayList<>();

    public FKDispatherServlet(){ super(); }

    //ServletConfig 可以获取到web.xml为servlet的init-param的信息
    // 重写servlet的init方法，用于初始化
    @Override
    public void init(ServletConfig config) throws ServletException {

        //相当于获取application.xml这个文件的路径
        String application = config.getInitParameter("contextConfigLocation");
        System.out.println("application = " + application);
        //通过完善DispatherServlet的功能
        //实现像Spring一样的IOC容器初始化，DIz注入，MVC中的url对应Method(解析Controller)
        //1。加载配置文件，application.xml,这里用application.properties来替代
        doLoadConfig(application);
        //2. 扫描配置文件中描述的相关所有的类
        doScanner(p.getProperty("scanPackage"));
        //3.初始化所有相关的类的实例，并且保存在ioc容器之中（自己写的IOC容器（Map））
        doInstance();
        //4.实现依赖注入,DI从IOC容器中找到加上了@FKAutowred注解的所有字段全部赋值给在IOC
        //容器中找到的对应实例的，
        //利用反射机制进行动态赋值

        doAutowired();

        //============Spring的基本功功能实现了，暂时还不支持aop=====================//
        //5。把在@Controller中加了@RequestMapping这个注解的方法和URl构造成一个对应关系
        // 可以理解为就是一个Map(HandlerMapping)结构，key,url,值就是Method
        // 这个操作是SPringMvc中最关键的点
        initHandlerMapping();

        System.out.println("Fk Edu Mvc is init.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    //6.初始化完毕，等待请求，根据用户请求的url在HandlerMapping中找到具体的method
    //  调用doGet/doPost方法
    // 通过反射机制动态调用该方法并且执行
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //根据用户请求的url，去找到其对应的method
        //通过反射机制去动态调用该方法
        //将返回结果通过response输出到浏览器
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Excepiton,Details:\r\n " + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]",""));
        }

    }
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        if(handlerMapping.isEmpty()){
            resp.getWriter().write("404 ntofound");
            return;
        }

        try{
            Handler handler = getHandler(req);
            if(handler == null){
                //如果没有匹配上。404
                resp.getWriter().write("404 ntofound");
                return;
            }

            //获取方法的参数列表
            Class<?> [] paramTypes = handler.method.getParameterTypes();

            //保存所有需要自动赋值的参数值
            Object [] paramValues = new Object[paramTypes.length];

            //获取请求的参数map（只读map） 因为有checkbox这种可能一个那么对应多个value所以value是数组
            Map<String,String[]> params = req.getParameterMap();
            for (Map.Entry<String,String[]> param : params.entrySet()){
                //遍历每个请求参数   \\[|\\] 为[或]    \\s 为空白字符,一下
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","").replaceAll(",\\s",",");

                //如果找到匹配的对象，则开始填充参数值
                //注意这里 ，提前使用    卫语句。避免多层if嵌套
                if(!handler.paramIndexMapping.containsKey(param.getKey())){ continue; }
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index],value);
            }
            //设置方法中的req和resp对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;

            handler.method.invoke(handler.controller,paramValues);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将请求中的String参数转化成我们需要的实际参数类型，如integer
     * @param type
     * @param value
     * @return
     */
    private Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
     * 判断请求的url能否匹配上需要的映射方法
     * @param req
     * @return
     * @throws Exception
     */
    private Handler getHandler(HttpServletRequest req) throws Exception{
        if(handlerMapping.isEmpty()){ return null;}
        //1.拿到请求的url
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        //2.便利handlermapping 查找
        for(Handler handler : handlerMapping){
            try{
                Matcher matcher = handler.pattern.matcher(url);
                //如果匹配上，返回
                if(matcher.matches()){
                    return handler;
                }
            } catch (Exception e){
                throw e;
            }
        }
        return null;
    }


    /**
     * 加载配置文件
     * @param location
     */
    private void doLoadConfig(String location) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(is!=null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 扫描配置文件中所有的类,递归扫描
     * @param packegeName
     */
    private void doScanner(String packegeName) {
        //将所有包路径转换成文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packegeName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹，继续递归
            if(file.isDirectory()){
                doScanner(packegeName+"."+file.getName());
            }else{
                classNames.add(packegeName + "." + file.getName().replace(".class",""));
            }
        }
    }

    /**
     * 将扫描到的所有类实例化到ioc容器
     */
    private void doInstance() {
        if(classNames.isEmpty()){
            return;
        }
        for (String className : classNames) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
                //虽然类都扫描到了，不是所有的类都要初始化
                if(clazz.isAnnotationPresent(FKController.class)){
                    //只要加了controller就能初始化
                    //保存到IOC容器之中

                    //beanName默认使用类名 的首字母小写作为beanName
                    //自定义beanName

                    String beanName = lowerFirst(clazz.getSimpleName());
                    ioc.put(beanName,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(FKService.class)){
                    //1.自定义beanName,优先自定
                    FKService service = clazz.getAnnotation(FKService.class);
                    String beanName = service.value();
                    //2.默认beanName,首字母小写
                    if("".equals(beanName.trim())){
                        beanName = lowerFirst(clazz.getSimpleName());
                    }
                    //注意，forName()这个静态方法调用了启动类加载器,实现1，类加载，2.类链接
                    //newInstance() 3.实例化，，，以上相当于将new 分为两步
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);

                    //3.根据类型去判断，吧所有的接口也要找出来（因为所有被该类实现的接口autowired时，需要使用该类进行注入）
                    //利用接口本身全程作为Key,把其对应的实现类的实例作为值
                    //TODO 判断重复， map主键重复。一个接口可能多个实现类
                    //getInterfaces() 获得该class 实现的额所有接口（数组）
                    Class<?> [] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces){
                        ioc.put(i.getSimpleName(),instance);
                    }

                }else {
                    //否则，不加入容器管理
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 依赖注入，将实例化在ioc容器中的实例注入值。
     *  即，为他们拥有的@Autowired注解的属性，从
     */
    private void doAutowired() {
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String ,Object> entry :ioc.entrySet()){
            Field [] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
               //不是所有的额牛奶都叫特仑苏 .
                if(!field.isAnnotationPresent(FKAutowired.class)){
                    continue;
                }
                //2.FKAutowired使用默认bytype的beanname
                String beanName = field.getType().getSimpleName();
                //如果是，哪怕你是private的成员变量，也要强制赋值
                try{
                    //jdk源码中对于private的属性，默认isAccessible是false。
                    // 当访问时会拒绝抛出IllegalAccessException异常，必须先修改权限setAccessible为true
                    field.setAccessible(true);
                    //开始赋值
                    /* @param obj the object whose field should be modified
                       @param value the new value for the field of {@code obj}*/
                    //如果ioc容器中存在则注入，
                    if(ioc.get(beanName)!= null){
                        field.set(entry.getValue(),ioc.get(beanName));
                    }
                    //否则，如果必须则报错，非必需则跳过
                    if(field.getAnnotation(FKAutowired.class).required()){
                        throw new Exception("NoSuchBeanDefinitionException: No qualifying bean of type");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }


    /**
     * 初始化HandlerMapping，
     */
    private void initHandlerMapping() {

        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            //不是所有的牛奶都叫特仑苏
            if(!clazz.isAnnotationPresent(FKController.class)){
                continue;
            }
            String baseUrl = "";
            //获取controller的url配置
            if(clazz.isAnnotationPresent(FKRequestMapping.class)){
                FKRequestMapping requestMapping = clazz.getAnnotation(FKRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            //获取Methid的url配置
            Method [] methods = clazz.getMethods();
            for(Method method :methods) {
                //跳过没有加FKRequestMapping注解的
                if(!method.isAnnotationPresent(FKRequestMapping.class)){
                    continue;
                }
                //映射Url
                FKRequestMapping requestMapping = method.getAnnotation(FKRequestMapping.class);
                String regex = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+","/");
                Pattern patten = Pattern.compile(regex);
                //这里有一个问题，。光方法和url的对应不够，method.invoke需要对象名，
                //所以这里需要把对象beanname也放入handlermapping中
                Handler handler = new Handler(entry.getValue(),method,patten);
                handler.putParamIndexMapping(method);
                handlerMapping.add(handler);
                System.out.println("Mapping :" + regex + "," + method);
            }
        }
    }

    private String lowerFirst(String str) {
        //tolowercase（）源码
        char [] chars = str.toCharArray();
        //大小写ascii码差值为32
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * Handler内部类记录Controller 中的Request Mapping和Method的对应关系
     */
    private class Handler{
        protected Object controller;    //保存方法对应的实例
        protected Method method;    //保存映射的方法
        protected Pattern pattern;  //支持正则的Url映射
        protected  Map<String,Integer> paramIndexMapping;    //参数顺序

        /**
         * 构造一个Handler基本的参数
         * @param controller
         * @param method
         * @param pattern
         */
        protected Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            this.paramIndexMapping = new HashMap<>();
        }

        //提取记录该方法上所有参数的位置和，他们的参数值从哪里来
        private void putParamIndexMapping(Method method){
            //1.提取方法中所有参数上的所有注解
            Annotation [] [] pa = method.getParameterAnnotations();
            for (int i=0;i <pa.length ; i++){
                //每个参数上可能有多个注解
                for(Annotation a : pa[i]){
                    if(a instanceof FKRequestParam){
                        String paramName = ((FKRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            //记录映射参数值和位置
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            //提取方法中的request和response参数，记录位置
            Class<?> [] paramsTypes = method.getParameterTypes();
            for(int i = 0; i < paramsTypes.length ; i++){
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i );
                }

            }
        }

    }
}

