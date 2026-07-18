package cn.liyu.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性（对标苍穹外卖 AliOssProperties）
 *
 * @author 医秒通
 */
@Component
@ConfigurationProperties(prefix = "hospital.alioss")
@Data
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
