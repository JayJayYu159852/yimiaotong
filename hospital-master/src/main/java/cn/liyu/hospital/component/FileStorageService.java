package cn.liyu.hospital.component;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口（策略模式）
 *
 * @author 医秒通
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问 URL，失败返回 null
     */
    String uploadFile(MultipartFile file);
}
