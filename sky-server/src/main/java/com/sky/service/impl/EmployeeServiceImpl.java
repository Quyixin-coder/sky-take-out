package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service     //请 Spring 创建这个业务对象
public class EmployeeServiceImpl implements EmployeeService {
                           //这个类负责实现员工业务接口。
    @Autowired  //请 Spring 把 Mapper 自动交给它使用。
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!md5.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (Objects.equals(employee.getStatus(), StatusConstant.DISABLE)) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();// 创建一个 Employee 对象

        // 对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);//固定工具 把 DTO 中同名字段复制到 Entity。

        // 设置账号状态
        employee.setStatus(StatusConstant.ENABLE);//新员工默认启用

        // 设置密码  新员工的默认密码是 123456，数据库保存 MD5 指纹
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 设置创建时间和修改时间
        LocalDateTime now = LocalDateTime.now();
        employee.setCreateTime(now);
        employee.setUpdateTime(now);

        // 记录创建人和修改人ID
        Long id = BaseContext.getCurrentId();
        employee.setCreateUser(id);
        employee.setUpdateUser(id);

        employeeMapper.insert(employee);
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> result = page.getResult();
//total：一共有多少名员工。
//result：当前这一页的员工列表。
        return new PageResult(total, result);
    }

    @Override            //传过来的id=2 status = 0
    public void startOrStop(Integer status, Long id) {  //updateTime = 当前时间
        Employee employee = Employee.builder()
                .updateTime(LocalDateTime.now())
                .id(id)
                .status(status)
                .build();
        employeeMapper.update(employee);  //然后交给 Mapper 修改
    }

    @Override
    public Employee getById(Long id) {
        return employeeMapper.getById(id);
    }

    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();// 创建一个 Employee 对象
        BeanUtils.copyProperties(employeeDTO, employee);//把网页传来的字段复制进去
        employee.setUpdateTime(LocalDateTime.now());//记录修改时间
        employee.setUpdateUser(BaseContext.getCurrentId());//记录是谁修改的
        employeeMapper.update(employee);//让 Mapper 执行数据库修改
    }

}
