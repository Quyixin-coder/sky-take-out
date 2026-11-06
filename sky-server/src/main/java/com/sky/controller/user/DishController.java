package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")  //这是网页的接口 why括号里面写了名字因为项目里还有一个后台菜品 Controller 避免冲突
@RequestMapping("/user/dish")  //声明公共路径
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;//Spring，请把菜品 Service 交给我

    @Autowired
    private RedisTemplate redisTemplate;//Spring，请把 Redis 工具注入给我

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")     //最终接口为GET /user/dish/list
    @ApiOperation("根据分类id查询菜品")    //Result统一返回盒子   List<DishVO>  菜品展示对象列表
    public Result<List<DishVO>> list(Long categoryId) { //categoryId 来自请求参数：?categoryId=20
        // 查询redis中是否有菜品数据
        String key = "dish_" + categoryId;//缓存的key   第一步组成Redis键 eg：key = "dish_20"
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);//第二步从 Redis 查询
        //通俗的理解：Redis 拿来一个没有明确类型标签的箱子，程序把它认定为“菜品列表”。
        if (list != null && !list.isEmpty()) {  //RedisTemplate：这是 Spring 提供的 Redis 操作工具
            //  Redis 取出来的数据被当成普通 Object，程序知道它保存的是一个菜品列表，所以转换成List<DishVO>
            // Redis，请把编号为 dish_20 的柜子打开，把里面的数据给我
            return Result.success(list);   //缓存命中     //opsForValue()：表示操作普通的键值数据
        }

        // redis中不存在，先从数据库中查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品 用户只能看到起售菜品

        list = dishService.listWithFlavor(dish);//查询菜品列表，同时查询每个菜品的口味
            //好的 至此我门进入到service层去看这一步的具体实现

        // 将查询的数据放入缓存中
        redisTemplate.opsForValue().set(key, list);//把刚查到的菜品列表写入 Redis。
                                                     //缓存未命中，查询数据库并回填缓存
        return Result.success(list);                   //把查询到的完整列表保存到 Redis
        // 下一次再查询这个分类dish_20已存在课直接返回不需要查询Mysql 这就是缓存加速
    }//小程序显示菜品和口味
}
