package cn.liyu.hospital.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Objects;

/**
 * 通用结果返回对象
 *
 * @author 医秒通
 */
public class CommonResult<T> implements Serializable {
    /**
     * 结果码
     */
    @Schema(description = "结果码")
    private Long code;
    /**
     * 提示信息
     */
    @Schema(description = "提示信息")
    private String message;
    /**
     * 返回数据
     */
    @Schema(description = "返回数据")
    private T data;

    protected CommonResult() {
    }

    private CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 返回成功结果
     */
    public static <T> CommonResult<T> success() {
        return new CommonResult<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    /**
     * 返回成功结果
     *
     * @param data 返回数据
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 返回失败结果
     */
    public static <T> CommonResult<T> failed() {
        return failed(ResultCodeEnum.FAILED.getCode(), ResultCodeEnum.FAILED.getMessage());
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     */
    public static <T> CommonResult<T> failed(String message) {
        return failed(ResultCodeEnum.FAILED.getCode(), message);
    }

    /**
     * 返回失败结果
     */
    public static <T> CommonResult<T> failed(long code, String message) {
        return new CommonResult<>(code, message, null);
    }

    /**
     * 返回校验失败结果
     */
    public static <T> CommonResult<T> validateFailed() {
        return failed(ResultCodeEnum.VALIDATE_FAILED.getCode(), ResultCodeEnum.VALIDATE_FAILED.getMessage());
    }

    /**
     * 返回校验失败结果
     *
     * @param message 错误信息
     */
    public static <T> CommonResult<T> validateFailed(String message) {
        return failed(ResultCodeEnum.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * 未授权返回结果
     */
    public static <T> CommonResult<T> unauthorized(T data) {
        return new CommonResult<>(ResultCodeEnum.UNAUTHORIZED.getCode(), ResultCodeEnum.UNAUTHORIZED.getMessage(), data);
    }

    /**
     * 未登录返回结果
     */
    public static <T> CommonResult<T> forbidden(T data) {
        return new CommonResult<>(ResultCodeEnum.FORBIDDEN.getCode(), ResultCodeEnum.FORBIDDEN.getMessage(), data);
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommonResult<?> that = (CommonResult<?>) o;
        return Objects.equals(code, that.code) && Objects.equals(message, that.message) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data);
    }
}
