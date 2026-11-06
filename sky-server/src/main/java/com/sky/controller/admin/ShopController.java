package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 修改店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status) {// 修改店铺营业状态
        log.info("设置店铺营业状态:{}", status == 1 ? "营业" : "打烊");
        redisTemplate.opsForValue().set("SHOP_STATUS", status);// 修改成功后数据库不会发生变化，因为营业状态本来就不在 MySQL里面
        return Result.success();//用户端和管理员端读取同一个 Redis Key：SHOP_STATUS


    }

    @GetMapping("/status")//管理员查询营业状态
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus() {// 把 Redis 取出的 Object 转成 Integer
        Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
        log.info("获取店铺营业状态:{}", status == 1 ? "营业" : "打烊");
        return Result.success(status);
    }
}
