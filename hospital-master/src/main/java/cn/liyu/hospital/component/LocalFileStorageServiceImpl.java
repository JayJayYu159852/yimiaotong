package cn.liyu.hospital.component;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储服务实现（MinIO 不可用时的降级方案）
 * <p>
 * 使用 @ConditionalOnMissingBean 注解，仅当没有 MinioFileStorageServiceImpl 时生效。
 * 如果想强制使用本地存储，请注释掉 MinIO 的 @Component 注解。
 *
 * @author 医秒通
 */
@Component
@ConditionalOnMissingBean(MinioFileStorageServiceImpl.class)
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    @Value("${local.storage.path:./uploads}")
    private String storagePath;

    @Value("${local.storage.url:http://localhost:8080/hospital/files/}")
    private String urlPrefix;

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            // 确保存储目录存在
            Path dir = Paths.get(storagePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (StrUtil.isNotEmpty(originalFilename) && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

            // 保存到本地磁盘
            Path targetPath = dir.resolve(filename);
            file.transferTo(targetPath.toFile());

            String fileUrl = urlPrefix + filename;
            LOGGER.info("本地存储上传成功: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            LOGGER.error("本地存储上传失败: {}", e.getMessage(), e);
        }

        return null;
    }
}
