package cn.liyu.hospital.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器。
 * <p>
 * Token 读取优先级：
 * 1. Authorization 请求头（Bearer xxx）—— 标准方式，Apifox / 前端使用
 * 2. URL 参数 ?token=xxx —— Knife4j 调试兜底（Knife4j 4.x 全局参数不支持 header 注入）
 * 3. 自定义请求头 token —— 备用
 *
 * @author 医秒通
 */

public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.tokenHeader}")
    private String tokenHeader;

    @Value("${jwt.tokenHead}")
    private String tokenHead;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            String authToken = extractToken(request);

            if (authToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                String username = jwtTokenUtil.getUserNameFromToken(authToken);

                LOGGER.info("JWT过滤器: 解析用户名={}", username);

                if (username != null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication
                                = new UsernamePasswordAuthenticationToken(userDetails,
                                null, userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        LOGGER.info("JWT过滤器: 认证成功 user={}", username);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 存入 ThreadLocal，方便业务代码直接获取
                        if (userDetails instanceof AccountDetails) {
                            AccountDetails details = (AccountDetails) userDetails;
                            cn.liyu.hospital.dto.UserDTO userDTO = new cn.liyu.hospital.dto.UserDTO();
                            userDTO.setId(details.getAccount().getId());
                            userDTO.setPhone(details.getUsername());
                            UserHolder.saveUser(userDTO);
                        }
                    } else {
                        LOGGER.warn("JWT过滤器: Token校验失败 user={}", username);
                    }
                } else {
                    LOGGER.warn("JWT过滤器: Token解析失败");
                }
            }

            chain.doFilter(request, response);
        } finally {
            // 请求结束必须清理 ThreadLocal，防止内存泄漏
            UserHolder.removeUser();
        }
    }

    /**
     * 从请求中提取 JWT Token（支持多种来源）
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 标准 Authorization 请求头: "Bearer xxx"
        String authHeader = request.getHeader(this.tokenHeader);
        if (StringUtils.hasText(authHeader)) {
            String token = stripBearerPrefix(authHeader);
            LOGGER.info("JWT过滤器: 从Authorization头提取Token → {}", maskToken(token));
            return token;
        }

        // 2. URL 参数 ?token=xxx（Knife4j 全局参数兜底）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            // 兼容用户填写 "Bearer xxx" 或直接填 token
            String token = stripBearerPrefix(tokenParam);
            LOGGER.info("JWT过滤器: 从URL参数提取Token → {}", maskToken(token));
            return token;
        }

        // 3. 自定义 token 请求头
        String tokenHeader = request.getHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            String token = stripBearerPrefix(tokenHeader);
            LOGGER.info("JWT过滤器: 从token头提取Token → {}", maskToken(token));
            return token;
        }

        LOGGER.debug("JWT过滤器: 未检测到任何Token来源");
        return null;
    }

    /**
     * 循环剥离所有 "Bearer " 前缀，防止前端或后端重复拼接导致多层前缀
     */
    private String stripBearerPrefix(String value) {
        String result = value;
        while (result.toLowerCase().startsWith("bearer ")) {
            result = result.substring(7);
        }
        if (result.toLowerCase().startsWith("bearer")) {
            result = result.substring(6);
        }
        return result.trim();
    }

    /**
     * 日志脱敏：只显示 Token 前 10 位
     */
    private String maskToken(String token) {
        if (token == null) return "null";
        return token.length() > 10 ? token.substring(0, 10) + "..." : token;
    }
}
