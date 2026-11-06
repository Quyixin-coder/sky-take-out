package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController                           //这个类负责接收网页请求，并返回 JSON
@RequestMapping("/admin/employee")          //- 这个类的统一部门地址是 /admin/employee。类似一家公司的部门地址：
@Api(tags = "员工管理")
@Slf4j
public class EmployeeController {

    @Autowired   //请 Spring 把 Service 自动交给我
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")            //   告诉spring表示在部门地址后面追加 /login
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {//@RequestBody：读取请求中的 JSON。
        log.info("员工登录：{}", employeeLoginDTO);                                        //EmployeeLoginDTO：告诉 Spring 按什么结构转换
                                                                                          //employeeLoginDTO：转换后得到的 Java 对象。
        Employee employee = employeeService.login(employeeLoginDTO);     //Controller 把查询工作交给 Service。
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }                            //查询成功，把结果放进统一的成功返回盒子。

    @PostMapping//   固定写法（新增接口）@RequestMapping("/admin/employee")  所以完整地址是POST /admin/employee
    @ApiOperation(value = "新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO) {//括号里面 固定写法把网页提交的 JSON 变成 Java 对象
        log.info("新增员工{}", employeeDTO);
        employeeService.save(employeeDTO);//业务部分：Controller 把保存工作交给 Service。   Controller 自己不处理，把 DTO 交给 Service。
        return Result.success();//固定写法 返回成功 告诉网页：保存成功
    }

    @GetMapping("/page")  //这是查询接口，地址后面是 /page
    @ApiOperation(value = "员工分页查询")        //这里没有 @RequestBody，因为这不是 JSON 表单，而是网址中的普通查询参数
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {
                                               //业务部分：分页查询 EmployeePageQueryDTO这个对象接收 page、pageSize 和 name 三个查询参数
        log.info("分页查询{}", employeePageQueryDTO);
        PageResult result = employeeService.pageQuery(employeePageQueryDTO);
                                               //业务部分：Controller 把查询工作交给 Service。
                                               //查询成功，把结果放进统一的成功返回盒子。
        return Result.success(result);
    }

    @PostMapping("/status/{status}")
    @ApiOperation(value = "员工状态禁用启用")
    public Result startOrStop(@PathVariable("status") Integer status, Long id) {
        log.info("启用禁用员工状态{}", status);        //状态改为0也就是禁用，员工编号是2
        employeeService.startOrStop(status, id);//下一步就是打开EmployeeServiceImpl.java
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询员工信息")
    public Result<Employee> getById(@PathVariable("id") Long id) {
        log.info("根据ID查询员工信息{}", id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    @PutMapping//  固定写法（修改接口）@RequestMapping("/admin/employee")  所以完整地址是PUT /admin/employee
    @ApiOperation(value = "修改员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO) {//括号里面 固定写法接受网页提交的JSON，并转换成 EmployeeDTO
        log.info("修改员工信息{}", employeeDTO);
        employeeService.update(employeeDTO);//业务部分：Controller 把修改工作交给 Service。
        return Result.success();//固定写法 返回成功 告诉网页：修改成功
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation(value = "员工退出")
    public Result<String> logout() {
        return Result.success();
    }

}
