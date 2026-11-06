package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component//表示这是一个 Spring 组件,请创建 OrderTask 对象并交给 Spring 管理//这个类是一个任务类//处理订单超时和派送中的订单
@Slf4j//日志写上之后自动生成log对象 直接使用log.info
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ")   // 每分钟触发一次
    public void processTimeoutOrders() {
        log.info("处理超时订单");
        // 获取超时订单 当前时间 - 15
        List<Orders> orderList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (orderList != null && !orderList.isEmpty()) {
            orderList.forEach(order -> {
                order.setStatus(Orders.CANCELLED);//Orders.CANCELLED 定义在：com.sky.entity.Orders.java
                order.setCancelReason("订单超时，自动取消");//设置取消原因
                order.setCancelTime(LocalDateTime.now());//设置取消时间
                orderMapper.update(order);//真正的更新订单在 OrderMapper.xml
            });
        }
    }

    /**
     * 处理一直处于派送中的
     */
    @Scheduled(cron = "0 0 1 * * ? ")   // 每天凌晨一点触发
    public void processDeliveryOrders() {
        log.info("处理派送中的订单");
        // 获取派送中的订单   查找状态 4“派送中”且下单超过 60 分钟的订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);//获取当前时间减去 60 分钟的时间
        List<Orders> orderList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (orderList != null && !orderList.isEmpty()) {//判断订单列表是否为空
            orderList.forEach(order -> {//遍历订单列表
                order.setStatus(Orders.COMPLETED);//Orders.COMPLETED 定义在：com.sky.entity.Orders.java
                order.setDeliveryTime(LocalDateTime.now());//设置派送时间 //LocalDateTime.now() 获取当前时间
                orderMapper.update(order);//真正的更新订单在 OrderMapper.xml
            });
        }
    }
}
