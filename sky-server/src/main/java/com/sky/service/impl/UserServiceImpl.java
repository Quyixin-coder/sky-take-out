package com.sky.service.impl;  //这里的 impl 表示“实现类”

import com.alibaba.fastjson.JSON;  // FastJSON 是一个用于 Java 对象和 JSON 数据之间进行转换的库
import com.alibaba.fastjson.JSONObject;  // JSONObject 是 FastJSON 中用于表示 JSON 对象的类
import com.sky.constant.MessageConstant;  // MessageConstant 类中定义了一些常量字符串，用于表示业务消息
import com.sky.dto.UserLoginDTO;  // UserLoginDTO 类用于接收微信登录的参数
import com.sky.entity.User;  // User 类表示用户信息
import com.sky.exception.LoginFailedException;  // LoginFailedException 类用于表示登录失败的业务异常
import com.sky.mapper.UserMapper;  // UserMapper 接口用于数据库操作
import com.sky.properties.WeChatProperties;  // WeChatProperties 类中定义了微信接口的参数
import com.sky.service.UserService;  // UserService 接口定义了用户服务
import com.sky.utils.HttpClientUtil;  // HttpClientUtil 类用于发送 HTTP 请求
import lombok.extern.slf4j.Slf4j;  // log4j 日志框架
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;  // 用于自动装配依赖
import org.springframework.beans.factory.annotation.Value;  // 用于从配置文件中获取属性值
import org.springframework.stereotype.Service;  // 用于标识服务层组件

import java.time.LocalDateTime;//表示日期和时间。
import java.util.HashMap;
import java.util.Map;//保存请求微信接口的参数。

@Service  //告诉 Spring，这是业务层对象，请创建它并交给其他类使用
@Slf4j   //用于日志记录
public class UserServiceImpl implements UserService {
    // 微信接口地址  真实微信登录时，后端会向这个地址发送 code，换取 openid
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired  //请 Spring 把微信配置对象交给当前类。
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Value("${sky.wechat.mock-enabled:false}")
    private boolean mockLoginEnabled;//运行时：mockLoginEnabled = true表示开启模拟登录

    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override//目的是告诉编译器，下面这个方法是在实现接口中的方法  表示这个方法覆盖了父类的方法
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 判断openid是否为空，为空登录失败，抛出异常
        String openid = getOpenId(userLoginDTO.getCode());//根据code获取openid 真实环境下会请求微信服务器
        log.info("openid: {}", openid);//记录openid。打印openid日志。{}是日志占位符
        log.info("UserLoginDto {}", userLoginDTO);//记录用户登录信息。打印用户登录信息日志
        if (openid == null) {//openid 为空 登录失败
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断用户是否为新用户  根据 openid 查询用户
        User user = userMapper.getByOpenid(openid);  //查询 user 表
        if (user == null) {
            // 新用户自动完成注册
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();//真正创建 User 对象，并重新赋值给变量 user
            userMapper.insert(user);//将新用户插入数据库
        }

        // 返回用户对象
        return user;
    }

    private String getOpenId(String code) {//定义获取 openid 的辅助方法
        if (mockLoginEnabled //本地模拟登录 表示本地模拟登录开关已开启
                && code != null
                && code.startsWith("local:")) {
            return code.substring("local:".length());
        }

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("appid", weChatProperties.getAppid());
        reqParams.put("secret", weChatProperties.getSecret());
        reqParams.put("js_code", code);
        reqParams.put("grant_type", "authorization_code");//grant_type 是微信接口要求的固定参数
        String json = HttpClientUtil.doGet(WX_LOGIN, reqParams);//请求微信服务器
        log.info("微信接口返回的json: {}", json);

        JSONObject parseJson = JSON.parseObject(json);
        return parseJson.getString("openid");
    }
}
