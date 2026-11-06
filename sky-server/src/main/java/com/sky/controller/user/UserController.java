package com.sky.controller.user;
//import表示当前文件需要使用这些类
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Api(tags = "C端用户相关接口")//在接口文档中把这一组接口归类为“C端用户相关接口”。
@RequestMapping("/user/user")//第一层/user表示用户端接口   第二层/user表示 UserController
@Slf4j//Lombok 的固定注解 它自动为类生成一个日志对象 名字是：log
public class UserController {

    @Autowired  //请 Spring 给我一个 UserService 对象  Spring 会自动创建 UserServiceImpl，再交给 Controller
    private UserService userService;   //Controller 是前台，Spring 自动给它安排一个用户业务经理。

    @Autowired  //让 Spring 把这些配置交给 jwtProperties 对象。
    private JwtProperties jwtProperties;

    @PostMapping("/login")   //最终地址：/user/user/login
    @ApiOperation("微信登录") //Swagger注解，在接口文档中为这个登录接口显示“微信登录” 不参与业务判断
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {//@RequestBody UserLoginDTO：把 JSON 变成 Java 对象
        // 微信登陆  Result统一返回盒子 <UserLoginVO>  盒子中的 data 是 UserLoginVO
        User user = userService.wxLogin(userLoginDTO);//调用service
     //把登录DTO交给用户业务Service让它完成登录，把查到的用户对象放进user变量
//user 表中的一行用户数据
        // 为微信用户生成jwt
        Map<String, Object> cliams = new HashMap<>();//理解成一个空表格
        cliams.put(JwtClaimsConstant.USER_ID, user.getId());//把用户id放进JWT   将来放进 JWT 的用户身份
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), cliams);
        //String token ：准备一个文本变量，用来保存生成的 JWT      调用项目中的 JWT 工具类生成通行证。
//参数 1：签名密钥 参数 2：签名过期时间 参数 3：载荷生成的一堆字符串它会被保存到 token 变量中
        UserLoginVO build = UserLoginVO.builder()//这是返回给前端的展示对象
                .id(user.getId())//用户id
                .openid(user.getOpenid())//用户openid
                .token(token)//用户token
                .build();//真正创建 UserLoginVO 对象

        return Result.success(build);//包装成功结果并返回
    }
}
