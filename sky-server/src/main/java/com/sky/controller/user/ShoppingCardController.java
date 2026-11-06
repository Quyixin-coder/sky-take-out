package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j//日志
@Api(tags = "C端-购物车接口")
public class ShoppingCardController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车:{}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("list") //Service 会从 BaseContext 获取当前用户
    @ApiOperation("查看购物车")
    public Result list() {
        return Result.success(shoppingCartService.showShoppingCart());
    }

    @DeleteMapping("/delete")
    @ApiOperation("清空购物车")
    public Result delete() {
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    @PostMapping("sub")   //前端不需要传 userId 因为如果让前端传 userId，用户就能伪造别人的编号
    @ApiOperation("购物车-删除商品")  //正确方式是后端从 JWT 中取得当前 userId
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("购物车-删除商品:{}", shoppingCartDTO);
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 清空购物车
     *
     * @return
     */
    @DeleteMapping("/clean")
    public Result<String> clean() {
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
}
