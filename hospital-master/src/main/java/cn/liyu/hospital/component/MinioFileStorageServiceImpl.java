package cn.liyu.hospital.component;

import cn.hutool.core.util.StrUtil;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MinIO 文件存储服务实现
 *
 * @author 医秒通
 */

@Component
public class MinioFileStorageServiceImpl implements FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioFileStorageServiceImpl.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url}")
    private String url;

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (StrUtil.isNotEmpty(originalFilename) && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = UUID.randomUUID().toString().replace("-", "") + suffix;

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String fileUrl = url + bucket + "/" + objectName;
            LOGGER.info("MinIO 上传成功: {}", fileUrl);
            return fileUrl;

        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            LOGGER.error("MinIO 上传失败: {}", e.getMessage(), e);
        }

        return null;
    }
}
