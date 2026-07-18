package cn.liyu.hospital;

import cn.liyu.hospital.common.security.JwtTokenUtil;
import cn.liyu.hospital.dto.param.UserRegisterParam;
import cn.liyu.hospital.service.IPowerAccountService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 压测数据生成工具（仅测试环境使用，不参与打包）
 * <p>
 * 用途：
 * 1. 批量创建压测用户（走真实注册链路：账号 + 钱包 + 默认就诊卡 + 基础信息，数据与正常注册完全一致）
 * 2. 为每个用户签发 JWT，写入 tokens.csv 供 JMeter 的 CSV Data Set Config 使用
 * <p>
 * 使用方式：直接运行 generateUsersAndTokens 测试方法（已存在的用户跳过创建，只重签 token，可重复执行）
 *
 * @author 医秒通
 */
// WebSocket 的 ServerEndpointExporter 需要真实 Servlet 容器，Mock 环境会启动失败
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadTestUserGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadTestUserGenerator.class);

    /**
     * 生成用户数量
     */
    private static final int USER_COUNT = 100;

    /**
     * 压测用户手机号前缀（19900000001 ~ 19900000100，与真实用户隔离便于清理）
     */
    private static final String PHONE_PREFIX = "1990000";

    /**
     * 压测用户统一密码
     */
    private static final String DEFAULT_PASSWORD = "test123456";

    /**
     * token 输出路径（JMeter CSV Data Set Config 指向该文件）
     */
    private static final String TOKEN_OUTPUT_PATH = "d:/tokens.csv";

    @Resource
    private IPowerAccountService powerAccountService;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private UserDetailsService userDetailsService;

    @Test
    void generateUsersAndTokens() throws IOException {
        List<String> tokens = new ArrayList<>();
        int created = 0;

        for (int i = 1; i <= USER_COUNT; i++) {
            String phone = PHONE_PREFIX + String.format("%04d", i);

            // 已存在则跳过创建，只重签 token（工具可重复执行）
            if (!powerAccountService.count(phone)) {
                UserRegisterParam param = new UserRegisterParam();
                param.setName("压测用户" + i);
                param.setPhone(phone);
                param.setPassword(DEFAULT_PASSWORD);
                // avatar_url 列 NOT NULL 且无默认值，占位空串
                param.setAvatarUrl("");

                if (!powerAccountService.registerUser(param)) {
                    LOGGER.error("压测用户创建失败: phone={}", phone);
                    continue;
                }
                created++;
            }

            tokens.add(jwtTokenUtil.generateToken(userDetailsService.loadUserByUsername(phone)));
        }

        Files.write(Paths.get(TOKEN_OUTPUT_PATH), tokens);
        LOGGER.info("压测数据就绪: 新建用户={}, token总数={}, 输出文件={}", created, tokens.size(), TOKEN_OUTPUT_PATH);
    }
}
