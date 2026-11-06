package com.sky.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {

        List<LocalDate> dates = buildDateList(begin, end);// 生成日期列表，包含开始日期和结束日期的完整日期区间

        List<Double> turnoverList = new ArrayList<>();//创建营业额列表
        for (LocalDate date : dates) {//循环每一天
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//生成当天开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//生成当天结束时间
            Map<String, Object> map = new HashMap<>();//创建参数列表Map 存放查询条件
            map.put("status", Orders.COMPLETED);//设置订单状态为已完成 Orders.COMPLETED 的值是 5。
            map.put("begin", beginTime);//设置开始时间
            map.put("end", endTime);//设置结束时间
            Double turnover = orderMapper.sumByMap(map);//Java中只能看到这一行，但MyBatis会根据方法名找到OrderMapper
            turnover = turnover == null ? 0.0 : turnover;//Java 中不能直接把 null 显示成金额，所以：
            turnoverList.add(turnover);//将营业额添加到营业额列表
        }

        return TurnoverReportVO.builder()//@Builder 由 Lombok 生成，所以可以连续调用 .dateList() 和 .turnoverList()。
                .dateList(StringUtils.join(dates, ","))//将日期列表转换为字符串，用逗号分隔
                .turnoverList(StringUtils.join(turnoverList, ","))//将营业额列表转换为字符串，用逗号分隔
                .build();
    }

    /**
     * 根据时间区间统计用户数量
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateList(begin, end);
        List<Integer> newUserList = new ArrayList<>(); // 新增用户数
        List<Integer> totalUserList = new ArrayList<>(); // 总用户数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 新增用户数量 select count(id) from user where create_time > ? and create_time < ?
            Integer newUser = getUserCount(beginTime, endTime);
            // 总用户数量 select count(id) from user where create_time < ?
            Integer totalUser = getUserCount(null, endTime);//getUserCount 方法189行
//null不限制开始时间
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 根据时间区间统计订单数量
     *
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateList(begin, end);
        // 每天订单总数集合
        List<Integer> orderCountList = new ArrayList<>();
        // 每天有效订单数集合
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 查询每天的总订单数 select count(id) from orders where order_time > ? and order_time <
            // ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            // 查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time <
            // ? and status = ?    只统计状态 5 的订单。
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
//getOrderCount 方法 第204行
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 时间区间内的总订单数
        Integer totalOrderCount = orderCountList.stream().reduce((a, b) -> a + b).get();
        // 时间区间内的总有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce((a, b) -> a + b).get();
        // 订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    /**
     * 查询指定时间区间内的销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils//StringUtils 由 Apache Commons Lang 库提供，用于字符串操作
                .join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()), ",");//将名称列表转换为字符串，用逗号分隔
        String numberList = StringUtils
                .join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()), ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 根据时间区间统计用户数量
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        return userMapper.countByMap(map);
    }

    /**
     * 根据时间区间统计指定状态的订单数量
     *
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("begin", beginTime);
        map.put("end", endTime);
        return orderMapper.countByMap(map);
    }

    /**
     * 生成包含开始日期和结束日期的完整日期区间。 营业额、用户和订单统计都需要逐天处理。
     */
    private List<LocalDate> buildDateList(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("统计开始日期和结束日期不能为空");
        }
        if (begin.isAfter(end)) {//开始日期是否晚于结束日期？
            throw new IllegalArgumentException("统计开始日期不能晚于结束日期");
        }

        List<LocalDate> dateList = new ArrayList<>();// 日期列表
        LocalDate date = begin;// 当前日期。因为 begin 是方法参数，重新赋值会改变原始参数。使用新的 date 更方便理解
        while (!date.isAfter(end)) {// 当前日期在结束日期之后则退出循环
            dateList.add(date);// 添加当前日期到日期列表
            date = date.plusDays(1);// 将当前日期加一天
        }
        return dateList;// 返回日期列表
    }

    /**
     * 导出excel
     *
     * @param resp
     */
    @Override
    public void exportExcel(HttpServletResponse resp) {
        LocalDate begin = LocalDate.now().minusDays(30);// 开始日期
        LocalDate end = LocalDate.now().minusDays(1);// 结束日期
        // BusinessDataVO里面包含：营业额、订单完成率、新用户数、有效订单数、客单价
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(//获取业务数据
                LocalDateTime.of(begin, LocalTime.MIN),// 开始日期
                LocalDateTime.of(end, LocalTime.MAX)// 结束日期
        );
//告诉浏览器 这是一个Excel OpenXML文件
        resp.setContentType(// 设置内容类型为 Excel 文件
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        resp.setHeader(// 设置响应头，指定文件名和内容类型
                "Content-Disposition",
                "attachment; filename=\"business-report.xlsx\""
        );//attachment表示这是一个下载附件  filename表示文件名，前端目前自己指定了运营数据统计报表.xlsx

        String template = "templates/运营数据报表模板.xlsx";// 模板文件路径
        try (
                InputStream in = this.getClass()//输入流：把文件网络的文件读进程序
                        .getClassLoader()
                        .getResourceAsStream(template);//读取 resources 中的文件
                //运行时文件在JAR内部，不是普通磁盘路径。getResourceAsStream可以从classpath中读取它
                ServletOutputStream out = resp.getOutputStream();// 获取输出流（写出）
        ) {
            if (in == null) {
                throw new IOException("运营数据报表模板不存在：" + template);
            }

            try (XSSFWorkbook excel = new XSSFWorkbook(in)) {// 创建工作簿
                XSSFSheet sheet = excel.getSheetAt(0);// 获取第一个工作表
                sheet.getRow(1).getCell(1).setCellValue("时间: " + begin + " ~ " + end);

                sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());// 设置营业额
                sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());// 设置订单完成率
                sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());// 设置新用户数
                sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());// 设置有效订单数
                sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());// 设置客单价

                for (int i = 0; i < 30; i++) { //循环写入 30 天明细
                    LocalDate date = begin.plusDays(i);// 计算当前日期
                    BusinessDataVO businessData = workspaceService.getBusinessData(// 获取业务数据
                            LocalDateTime.of(date, LocalTime.MIN),// 开始日期
                            LocalDateTime.of(date, LocalTime.MAX)// 结束日期
                    );
                    XSSFRow row = sheet.getRow(i + 7);// 前七行是标题头：获取当前行
                    row.getCell(1).setCellValue(date.toString());// 设置日期
                    row.getCell(2).setCellValue(businessData.getTurnover());// 设置营业额
                    row.getCell(3).setCellValue(businessData.getValidOrderCount());// 设置有效订单数
                    row.getCell(4).setCellValue(businessData.getOrderCompletionRate());// 设置订单完成率
                    row.getCell(5).setCellValue(businessData.getUnitPrice());// 设置客单价
                    row.getCell(6).setCellValue(businessData.getNewUsers());// 设置新用户数
                }

                excel.write(out);// 将工作簿写入输出流，把整个工作簿转换成 .xlsx 二进制字节，并写入 HTTP 响应输出流。
            }
            out.flush();// 刷新输出流，把缓冲区中的数据立即推送给浏览器。
        } catch (IOException e) {// 捕获输入输出异常
            throw new IllegalStateException("导出运营数据报表失败", e);// 抛出异常
        }
    }

}
//Workbook写入输出流out，返回浏览器下载