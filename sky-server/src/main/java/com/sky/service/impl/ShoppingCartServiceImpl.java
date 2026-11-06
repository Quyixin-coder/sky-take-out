package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @Override      //这个Service判断购物车已有商品
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();//创建ShoppingCart对象 完整路径：com.sky.entity.ShoppingCart.Java
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);      //将shoppingCartDTO中的属性复制到shoppingCart中

        Long userId = BaseContext.getCurrentId();//获取当前用户 如果没有userId可能查到其他用户购物车中的同款菜品
        shoppingCart.setUserId(userId);      //设置当前用户id

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);//查询是否已有记录

        // 如果已经存在，将数量+1
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);//mapper写SQL 所以再次添加时不会创建重复行，只会增加 number
        } else {
            // 如果不存在，存入购物车中
            // 判断是菜品
            Long dishId = shoppingCartDTO.getDishId();
            shoppingCart.setNumber(1);
            if (dishId != null) {
                // 添加菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
                shoppingCart.setCreateTime(LocalDateTime.now());
            }
            // 还是套餐
            else {
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setCreateTime(LocalDateTime.now());
            }
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     *
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        return shoppingCartMapper.list(ShoppingCart.builder().userId(BaseContext.getCurrentId()).build());
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    /**
     * 删除购物车中一个商品
     *
     * @param shoppingCartDTO
     */
    @Override// 减少一份的购物车Service 代码
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shop = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shop);// DTO 转 Entity
        shop.setUserId(BaseContext.getCurrentId());//从 JWT 获取当前用户id
        List<ShoppingCart> list = shoppingCartMapper.list(shop); //查询购物车记录
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);//取出对应购物车记录
            if (cart.getNumber() == 1) {// 数量为1时删除整行
                shoppingCartMapper.deleteById(cart.getId());
            } else {
                cart.setNumber(cart.getNumber() - 1);//数量大于1时减1
                shoppingCartMapper.updateNumberById(cart);
            }
        }
    }
}
