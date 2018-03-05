package simplemvc.servlet;

import simplemvc.annotation.Controller;
import simplemvc.annotation.RequestMapping;
import simplemvc.annotation.RequestParam;
import simplemvc.annotation.ResponseBody;
import simplemvc.common.HttpResponse;
import simplemvc.utils.ClasspathPackageScanner;
import simplemvc.utils.CollectionUtils;
import simplemvc.utils.PackageScanner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class DispatcherServlert extends HttpServlet {


	private Properties properties = new Properties();

	/**
	 * 配置文件扫描包下的所以类名
	 */
	private List<String> classNames = Collections.synchronizedList(new ArrayList<>());

	/**
	 * handlerMapping 用于存放请求映射url和其对应的具体方法
	 */
	private Map<String, Method> handlerMapping = Collections.synchronizedMap(new HashMap<String, Method>());

	/**
	 *controllerMap 用于存放请求url和其对应的具体的controller类
	 */
	private Map<String, Object> controllerMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * controllerInstances 用于存放具体类名和其对应的controller类的实例
	 */
	private Map<String, Object> controllerInstances = Collections.synchronizedMap(new HashMap<>());

	private void loadConfig(String location){
		try {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
			if(inputStream != null) {
				properties.load(inputStream);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void doSacanner(String packageName) {
		PackageScanner scanner = new ClasspathPackageScanner(packageName);
		try {
			classNames = scanner.getFullyQualifiedClassNameList();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initializeInstances() {
		if(CollectionUtils.isEmptyList(classNames)){
			return;
		}

		for (String className : classNames) {
			try {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(Controller.class)) {
					controllerInstances.put(className, clazz.newInstance());
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void initHadlerMapping() {
		if (controllerInstances.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : controllerInstances.entrySet()) {
			Class<? extends Object> clazz = entry.getValue().getClass();
			String baseUrl = "";

			if (clazz.isAnnotationPresent(RequestMapping.class)) {
				String value = clazz.getAnnotation(RequestMapping.class).value();
				baseUrl = value;
			}

			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(RequestMapping.class)) {
					continue;
				}

				String value = method.getAnnotation(RequestMapping.class).value();

				String methodUrl = "/" + value;

				StringBuilder fullUrl = new StringBuilder();
				fullUrl.append(baseUrl).append(methodUrl);

				//把//替换成/
				String uri = fullUrl.toString().replaceAll("/+","/");

				handlerMapping.put(uri, method);
				try {
					controllerMap.put(uri, clazz.newInstance());
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
		String requestUrl = req.getRequestURI();
		String contextPath = req.getContextPath();

		requestUrl = requestUrl.replace(contextPath, "").replaceAll("/+", "/");
		if (!handlerMapping.containsKey(requestUrl)) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Method invokeMethd = handlerMapping.get(requestUrl);

		Parameter[] parameters = invokeMethd.getParameters();
		Object[] paramValues = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			System.out.println(param.getName());
			Class<?> paramType = param.getType();
			String typeSimpleName = paramType.getSimpleName();

			if(paramType == HttpServletRequest.class){
				paramValues[i] = req;
				continue;
			}
			if (paramType == HttpServletResponse.class) {
				paramValues[i] = resp;
				continue;
			}

			Map<String,String[]> parameterMap = req.getParameterMap();
			
			if (paramType.isAnnotationPresent(RequestParam.class)) {
				String paramKey = paramType.getAnnotation(RequestParam.class).value();

				String[] values = req.getParameterValues(paramKey);
				paramValues[i] = Arrays.toString(values).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
			}

			if (typeSimpleName.equals("String")) {
				for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
					String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					paramValues[i] = value;
				}
			}
		}

		try {
			resp.setCharacterEncoding(HttpResponse.DEFAULT_CHARSET);
			resp.setContentType(HttpResponse.DEFUALT_CONTENTTYPE);
			resp.setStatus(HttpServletResponse.SC_OK);
			Object resturnValue = invokeMethd.invoke(controllerMap.get(requestUrl), paramValues);
			if(invokeMethd.isAnnotationPresent(ResponseBody.class)){
				byte[] responseBodyByte = ((String)resturnValue).getBytes(HttpResponse.DEFAULT_CHARSET);
				resp.setContentLength(responseBodyByte.length);
				resp.getOutputStream().write(responseBodyByte);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doDispatch(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doDispatch(req, resp);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		//读取配置文件路径
		String contextLocation = config.getInitParameter("contextConfigLocation");
		//加载配置文件
		loadConfig(contextLocation);
		//包扫描路径
		String scanPackage = properties.getProperty("componentscan");;
		//执行包扫描
		doSacanner(scanPackage);
		
		initializeInstances();
		
		initHadlerMapping();
	}

}
