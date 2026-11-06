package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品以及保存口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);//拷贝属性 名称、分类、价格、图片、描述等信息从 DTO 复制到 Dish
                                               //DishDTO 中的 flavors 不会复制到 Dish
                                                //因为 Dish 没有 flavors 字段
        // 向菜品表插入一条记录
        dishMapper.insert(dish);//先写 dish 表，因为口味必须通过菜品编号才能关联

        // 获取insert生成的主键id  取得自动生成的菜品编号
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();//取出 DTO 中的口味
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {      //遍历口味给每个口味设置 dishId
                // 向口味表插入n个数据
                flavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(flavors);//批量插入口味  我们INSERT菜品 1.取得菜品id 2.insert多条口味
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
//PageHelper 设置分页 Mapper 执行查询 得到 DishVO 列表 放进 PageResult
        List<DishVO> dishVOList = dishMapper.pageQuery(dishPageQueryDTO);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(dishVOList.size());//设置总条数
        pageResult.setRecords(dishVOList);//设置当前页数据

        return pageResult;
    }

    /**
     * 分页查询菜品
     *
     * @param dishPageQueryDTO
     */
    // @Override
    // public PageResult<DishVO> queryPage(DishPageQueryDTO dishPageQueryDTO) {
    //     return null;
    // }

    /**
     * 删除菜品
     *
     * @param ids
     */
    @Override
    // 事务一致性
    @Transactional    //Spring事务管理：一组数据要不全部成功 要不全部失败 自动回滚 失败了我就全部撤销此前的全部数据库操作
    public void deleteBatch(Long[] ids) {
        // 判断菜品是否被套餐关联
        for (Long id: ids) { //循环对选中的每一个菜品都执行一遍检查
            Dish dish = dishMapper.getById(id);//调用mapper 第50-51行
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {//安全比较两个值是否相等
                // 正在销售的菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断菜品是否被套餐关联                                            //原来的参数是数组 Arrays.asList(ids)这个把数组转换成List
        List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(Arrays.asList(ids));
        if (setmealIdsByDishIds != null && !setmealIdsByDishIds.isEmpty()) {//表示列表对象存在 表示列表里面确实有数据
            // 菜品被套餐关联，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
//再次遍历每个菜品编号。
        // 删除菜品表的菜品数据
        for (Long id: ids) {
            dishMapper.deleteById(id);
        //顺序不可以变
            // 删除菜品关联的口味数据 如果下面的失败了上面的也会回滚数据库依然会保持完整
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);//去mapper查询菜品基本数据 第一次执行SQL 去查询Dish 基本信息
        DishVO dishVO = new DishVO();      //DishVO 是返回给浏览器的数据对象

        if (dish != null) {//只有查到菜品，才继续查询口味
            BeanUtils.copyProperties(dish, dishVO);
            // 查询菜品对应的口味数据，从dish_flavor表查询
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);  //第二此执行SQL去查询List<DishFlavor> 口味
            dishVO.setFlavors(flavors);//把口味列表保存到 DishVO 的 flavors 属性
        }
   //先拿菜品档案 再拿菜品口味表 最后合成一份完整编辑资料
        return dishVO;
    }

    /**
     * 更新菜品以及口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional //让修改菜品、删除旧口味、插入新口味处于同一个事务中。保证数据的一致性，要么全部成功，要么全部失败
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish); //把 DTO 中与 Dish 同名的属性复制到 Dish 不复制flavors因为 Dish 类中没有 flavors 字段

        // 修改菜品表基本信息
        dishMapper.update(dish);

        // 删除原有口味数据
        dishFlavorMapper.deleteByDishId(dish.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();  //List表示列表，里面保存多个 DishFlavor
        if (flavors != null && !flavors.isEmpty()) {      //有口味列表，并且里面确实有口味，才执行保存
            // 批量插入n条数据
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());//给新口味设置 dishId
            });
            dishFlavorMapper.insertBatch(flavors);//批量插入新口味
        }
    }

    /**
     * 启用或禁用菜品
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()//这里使用的是 builder，它创建了一个只包含部分字段的对象：id 和 status
                .id(id)           //为什么不创建一个完整菜品对象 因为我们只想修改状态，不想修改名称、价格、分类等字段。
                .status(status)
                .build();
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        return dishMapper.getListById(categoryId);
    }

    /**
     * 条件查询菜品和口味    这里我们是作为用户来实现这个逻辑的
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);//我们可以去mapper接口以及Mapper XML中看一下

        List<DishVO> dishVOList = new ArrayList<>();//ArrayList 这是 Java 中常用的列表实现

        for (Dish d : dishList) {//这里简写为了d 表示遍历 dishList 中的每一个菜品
            DishVO dishVO = new DishVO();//创建一个空的展示对象，用来组合：菜品的基本信息+菜品口味
            BeanUtils.copyProperties(d,dishVO);//拷贝属性 名称、分类、价格、图片、描述等信息从 Dish 复制到 DishVO
                                               //此时 dishVO 有基本信息，但口味列表为空
            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());//可以去mapper看一下

            dishVO.setFlavors(flavors);//把口味放进展示对象     循环每个菜品 查询 dish_flavor 组合DishVo
            dishVOList.add(dishVO);//把完整对象加入最终列表  第一轮：dishVOList = [本地测试菜品+口味]
        }                                            //第二轮：dishVOList = [本地测试菜品+口味, 红烧肉+口味]

        return dishVOList;//循环结束 接下来返回controller里面写入 Redis 缓存
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    // @Override
    // public List<Dish> list(Long categoryId) {
    //     // return List.of();
    // }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    // @Override
    // public List<DishVO> listWithFlavor(Dish dish) {
    //     // return List.of();
    // }
}
