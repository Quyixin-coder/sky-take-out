package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);//方法名是：saveWithFlavor 同时保存菜品基本信息和菜品口味
        return Result.success();           //为什么不叫简单的 save？因为这个功能要写两张表，需要提醒程序员它不是普通新增。
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        dishPageQueryDTO.setPageSize(100000);
        return Result.success(dishService.pageQuery(dishPageQueryDTO));
    }


    /**
     * 启用或停用菜品
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")  //@PathVariable Integer status从 URL 路径读取
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        // 1. 启用 0. 停用
        log.info("启用或停用菜品：{}", id);//读取 id
        dishService.startOrStop(status, id);//调用 Service 修改数据库
        clearRedis("dish_*");  // 清理所有菜品缓存数据 看第113行
        return Result.success();//返回成功

    }

    @DeleteMapping//Spring MVC 固定模板 删除接口，根据id删除菜品，批量删除 ids
    @ApiOperation("删除菜品")                         // @RequestParam Long[] idsSpring MVC固定模板参数绑定
    public Result delete(@RequestParam Long[] ids) {  //读取网址中的 ids 参数，并转换成 Long 数组：Long[] ids = {51L, 52L};
        dishService.deleteBatch(ids);                 //Controller 自己不做业务判断，只把菜品编号数组交给 Service。

        // 将所有菜品缓存数据清理，所有以dish_的key 防止用户端继续看到已经删除的菜品
        Set keys = redisTemplate.keys("dish_*");//Redis，请找出所有以 dish_ 开头的键。
        if (keys != null) {
            redisTemplate.delete(keys);
        }

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询指定菜品")           //@PathVariable Long id：Spring MVC 路径参数绑定 从 URL 路径中读取 id
    public Result<DishVO> getByIdWithFlavor(@PathVariable Long id) { //Result统一返回盒子    DishVO菜品展示对象

        log.info("根据ID查询指定菜品：{}", id);
        return Result.success(dishService.getByIdWithFlavor(id));//根据菜品 id 查询菜品，并把口味一起查出来
    }

    @PutMapping    //这是一个修改接口
    @ApiOperation("更新菜品信息")                         // @RequestBody DishDTO dishDTO：把请求体中的 JSON 数据转换成 DishDTO 对象
    public Result update(@RequestBody DishDTO dishDTO) { //Jackson 是 Spring Boot 用来处理 JSON 的组件。
        log.info("更新菜品信息：{}", dishDTO);            //DishDTO 中保存修改后的菜品信息

        // 更新缓存数据
        String key = "dish_" + dishDTO.getCategoryId();  // 构建缓存键
        redisTemplate.delete(key);  // 删除缓存数据 修改菜品后，必须清除该分类的旧缓存
                    //service修改菜品以及菜品口味
        dishService.updateWithFlavor(dishDTO);  //Controller 把修改工作交给 Service
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }

    private void clearRedis(String keys) {//第62行调用了把字符串diah_*传给方法参数String keys = "dish_*";
        Set<String> cacheKeys = redisTemplate.keys(keys);//Redis，请找出所有以 dish_ 开头的键。
        redisTemplate.delete(cacheKeys);//把找到的这三个缓存键全部删除
    }
}
