package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
public class UserLoginDTO implements Serializable {

    private String code;
//Spring MVC + Jackson  JSON 请求体
                        //→ UserLoginDTO 对象
                        //→ 读取 code
}
