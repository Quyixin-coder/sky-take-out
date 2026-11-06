package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDTO
     */
    @Override
    @Transactional  //Spring事务 setmeal 和 setmeal_dish 必须一起成功 若关系保存失败则setmeal 的插入也回滚
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();//创建套餐对象  Setmeal 是 Entity
        BeanUtils.copyProperties(setmealDTO, setmeal);//复制基本字段 但是没有setmealDishes
        setmealMapper.insert(setmeal);//插入套餐主表 去看mapper.xml
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();//获取前端传来的套餐菜品关系 这里SetmealDish 完整路径entity\SetmealDish.java
        setmealDishes.forEach(setmealDish -> {                          //上面这一行如果套餐 id 生成结果是 5 则所有套餐菜品关系的 setmealId 都设置为 5
            setmealDish.setSetmealId(setmeal.getId()); //设置完套餐关系将其批量插入关系表
        });
        setmealDishMapper.insertBatch(setmealDishes);//批量插入关系表
        log.info("套餐和菜品的关联关系保存成功");
        log.info("套餐保存成功");
    }

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize()); //调用 PageHelper 假设PageHelper.startPage(1, 10);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);//调用mapper.xml中的pageQuery方法
        log.info("分页查询结果：{}", page);                             //Page<SetmealVO>:带分页信息的套餐展示对象列表
        return new PageResult(page.getTotal(), page.getResult());//返回第1页的10个套餐包装成 PageResult
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Override
    @Transactional  //Spring 事务管理
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {//第一段循环检查套餐状态
            Setmeal setmeal = setmealMapper.getById(id);

            if (setmeal != null        //先确保查到了套餐，避免空指针
                    && Objects.equals(
                            setmeal.getStatus(),//读取套餐状态：0：停售 1：起售
                            StatusConstant.ENABLE  //值为1
                    )) {
                throw new DeletionNotAllowedException(
                        MessageConstant.SETMEAL_ON_SALE//这里也有路径 在commom中全局异常处理器会把异常转换成json格式 进而浏览器显示
                );
            }
        }

        for (Long id : ids) {//第二段循环删除套餐及关联关系 删除两张表
            setmealDishMapper.deleteBySetmealId(id);//先删除 setmeal_dish 关系 再删除 setmeal 套餐
            setmealMapper.deleteById(id);          //先清掉套餐从属数据，再删除主数据。
        }
    }

    /**
     * 根据id查询套餐和关联的菜品数据
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        return null;
    }//直接返回空数据，但 Controller 没有调用它。

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional//Spring事务，确保修改套餐和关联的菜品数据时的一致性
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();  //创建 Setmeal对象 Setmeal是套餐Entity
        BeanUtils.copyProperties(setmealDTO, setmeal);//复制 DTO（基本） 字段
        //括号里面第一个参数是源对象，第二个参数是目标对象 源对象是有路径的可以去看看
        setmealMapper.update(setmeal);//更新套餐信息这个是修改主表（套餐主表）有路径
       //上下这两行都是与mapper相关了 执行了具体的SQL
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());//删除套餐关联的菜品 删除旧套餐菜品关系

        List<SetmealDish> setmealDishes =//取得新的套餐菜品列表
                setmealDTO.getSetmealDishes();

        if (setmealDishes != null && !setmealDishes.isEmpty()) {//只有选择了菜品，才插入套餐菜品关系
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());//补充新的套餐编号
            });
            setmealDishMapper.insertBatch(setmealDishes);//批量插入新关系
        }
    }

    /**
     * 套餐起售、停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {      //但是启售时要确保：套餐里的每一道菜都可以出售
        if (Objects.equals(status, StatusConstant.ENABLE)) {//只需要检察启售状态 因为停售了不需要检查 里面即使有停售商品也不影响关闭
            List<Dish> dishes = dishMapper.getBySetmealId(id);//取得套餐里的所有菜

            for (Dish dish : dishes) { //逐个遍历套餐里的菜品
                if (Objects.equals(
                        dish.getStatus(),//这是找到那个菜品的状态 若为0不行二者相等抛出异常
                        StatusConstant.DISABLE//这个是个常量0
                )) {
                    throw new SetmealEnableFailedException(
                            MessageConstant.SETMEAL_ENABLE_FAILED
                    );
                }
            }
        }

        setmealMapper.startOrStop(status, id);//所有菜品检查通过后：进入mapper执行SQL
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {

        return setmealMapper.getDishItemBySetmealId(id);
    }

    @Override
    public SetmealVO getById(Long id) {

        return setmealMapper.getByIdWithDish(id);
    } //这里是实际调用的位置
}
