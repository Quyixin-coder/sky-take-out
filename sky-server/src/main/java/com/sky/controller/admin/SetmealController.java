package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     *
     * @param setmealDTO
     * @return
     */
    @PostMapping  //Spring Cache：新增套餐后，清除对应分类的套餐缓存
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")// 清空setmealCache缓存
    public Result<String> save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);   // 打印日志
        setmealService.saveWithDish(setmealDTO);// 新增套餐，同时需要保存套餐和菜品的关联关系
        return Result.success();                 //Controller 只接收数据，不直接写数据库
    }// 新增套餐后，清除对应分类的套餐缓存，但后台修改、删除、启售停售使用了@CacheEvict(cacheNames = "setmealCache", allEntries = true)
     //因为在修改时可能会改变分类图片价格啥的 直接清理全部套餐缓存更简单、更可靠
    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO  分页查询参数
     * @return
     */
    @GetMapping("/page")      //Spring MVC 会自动把 URL 参数放入 DTO 对象中
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) { //这里没有 @RequestBody，因为参数在 URL 的查询字符串中，不是 JSON 请求体。
        log.info("分页查询套餐列表，请求参数：{}", setmealPageQueryDTO);//
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除套餐
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")  //删除套餐成功后，清除所有全部套餐列表缓存
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<String> delete(@RequestParam List<Long> ids) { //Long[]     Java 数组
                                                                 // List<Long>   java 集合列表
        log.info("删除套餐，ids：{}", ids);
        setmealService.deleteBatch(ids);//调用service
        return Result.success();//最后通过这里提交页面删除成功
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")  //@PathVariable Long id是Spring MVC 路径参数绑定 作用：从 URL 路径中读取 32
    public Result<SetmealVO> getById(@PathVariable Long id) {//外层统一返回 Result data 中保存 SetmealVO 这个VO是有路径的
        log.info("根据id查询套餐，id：{}", id);//
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     * @return
     */
    @PutMapping    //修改时，主表更新，关系表也重新生成
    @ApiOperation("修改套餐")  //套餐修改成功后，清除全部套餐列表缓存 allEntries = true表示清除setmealCache 中所有分类的缓存
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<String> update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐，请求参数：{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 启用或停用套餐
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用或停用套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用或停用套餐，status：{}，id：{}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();//OK最后返回成功 在状态修改后清除套餐缓存
    }
}