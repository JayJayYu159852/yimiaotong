package cn.liyu.hospital.component;

import cn.hutool.core.util.StrUtil;
import cn.liyu.hospital.config.AliOssProperties;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 文件存储服务实现（对标苍穹外卖 AliOssUtil + CommonController.upload）
 * <p>
 * 标注 @Primary 作为默认存储策略，优先于 MinIO/本地存储生效。
 *
 * @author 医秒通
 */
@Primary
@Component
public class AliOssFileStorageServiceImpl implements FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliOssFileStorageServiceImpl.class);

    @Resource
    private AliOssProperties aliOssProperties;

    @Override
    public String uploadFile(MultipartFile file) {
        // 生成唯一文件名：UUID + 原始扩展名
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotEmpty(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + suffix;

        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());

        try {
            // 创建PutObject请求
            ossClient.putObject(aliOssProperties.getBucketName(), objectName,
                    new ByteArrayInputStream(file.getBytes()));
        } catch (OSSException oe) {
            LOGGER.error("OSS上传被拒绝: errorCode={}, errorMessage={}, requestId={}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            return null;
        } catch (ClientException ce) {
            LOGGER.error("OSS客户端异常（网络不通或配置错误）: {}", ce.getMessage(), ce);
            return null;
        } catch (Exception e) {
            LOGGER.error("OSS上传失败: {}", e.getMessage(), e);
            return null;
        } finally {
            ossClient.shutdown();
        }

        // 文件访问路径规则 https://BucketName.Endpoint/ObjectName
        String fileUrl = "https://" + aliOssProperties.getBucketName() + "."
                + aliOssProperties.getEndpoint() + "/" + objectName;
        LOGGER.info("文件上传到OSS: {}", fileUrl);
        return fileUrl;
    }
}
