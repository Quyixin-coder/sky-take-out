package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Value("${sky.upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl;

    @PostMapping("upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {    //MultipartFile 是 Spring 用来表示上传文件的类型
        log.info("文件上传：{}", file);

        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("文件格式不正确");
            }

            String extension = originalFilename                      //substring：截取一部分文字
                    .substring(originalFilename.lastIndexOf("."))//lastIndexOf：查找最后一次出现的位置
                    .toLowerCase(Locale.ROOT);                       //toLowerCase：转成小写

            if (!extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {     //matches 用来判断扩展名是否符合规则
                return Result.error("只支持 jpg、jpeg、png、webp、gif 图片");   //如果扩展名不在允许列表中，就返回错误
            }

            String filename = UUID.randomUUID() + extension;  //UUID.randomUUID() 生成一个随机的 UUID 这样每个文件都有唯一名字
            Path uploadDirectory = Paths.get("uploads")  //Paths.get("uploads") 获取上传目录的路径
                    .toAbsolutePath() //转换成完整绝对路径
                    .normalize();  //清理路径中的 .. 等冗余部分。
            Files.createDirectories(uploadDirectory);  //创建上传目录

            Path target = uploadDirectory.resolve(filename).normalize();  //resolve：将 filename 添加到 uploadDirectory 路径下，并规范化得到目标文件的路径
            file.transferTo(target.toFile());  //把浏览器上传的文件写入目标磁盘路径
            log.info("文件已保存到：{}", target);

            return Result.success(
                    uploadBaseUrl + "/images/" + filename
            );
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }
    }

}
