package simplemvc.controller;

import simplemvc.annotation.Controller;
import simplemvc.annotation.RequestMapping;
import simplemvc.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by zhengb on 2018-03-05.
 */
@Controller
@RequestMapping(value = "/test/")
public class TestController {

    @RequestMapping(value = "test1")
    public void test1(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam("param") String param){
            System.out.println("请求参数：" + param);
        try {
            response.getWriter().println("测试返回：" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value =  "test2")
    public String test2(){
        return "test2返回内容";
    }
}
