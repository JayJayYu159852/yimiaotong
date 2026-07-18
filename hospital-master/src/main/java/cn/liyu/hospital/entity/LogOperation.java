package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class LogOperation implements Serializable {
    /**
     * 编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "编号")
    private Long id;
    /**
     * 账号名称
     */
    @Schema(description = "账号名称")
    private String accountName;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private Long startTime;
    /**
     * 消耗时间
     */
    @Schema(description = "消耗时间")
    private Integer spendTime;
    /**
     * 操作描述
     */
    @Schema(description = "操作描述")
    private String description;
    /**
     * 根路径
     */
    @Schema(description = "根路径")
    private String basePath;
    /**
     * uri
     */
    @Schema(description = "uri")
    private String uri;
    /**
     * url
     */
    @Schema(description = "url")
    private String url;
    /**
     * 请求方法
     */
    @Schema(description = "请求方法")
    private String method;
    /**
     * ip地址
     */
    @Schema(description = "ip地址")
    private String ipAddress;
    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    private String parameter;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date gmtCreate;
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date gmtModified;
    /**
     * 请求结果
     */
    @Schema(description = "请求结果")
    private String result;
    private static final long serialVersionUID = 1L;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName == null ? null : accountName.trim();
    }
    public Long getStartTime() {
        return startTime;
    }
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    public Integer getSpendTime() {
        return spendTime;
    }
    public void setSpendTime(Integer spendTime) {
        this.spendTime = spendTime;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
    public String getBasePath() {
        return basePath;
    }
    public void setBasePath(String basePath) {
        this.basePath = basePath == null ? null : basePath.trim();
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri == null ? null : uri.trim();
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method == null ? null : method.trim();
    }
    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress == null ? null : ipAddress.trim();
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter == null ? null : parameter.trim();
    }
    public Date getGmtCreate() {
        return gmtCreate;
    }
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
    public Date getGmtModified() {
        return gmtModified;
    }
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result == null ? null : result.trim();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", accountName=").append(accountName);
        sb.append(", startTime=").append(startTime);
        sb.append(", spendTime=").append(spendTime);
        sb.append(", description=").append(description);
        sb.append(", basePath=").append(basePath);
        sb.append(", uri=").append(uri);
        sb.append(", url=").append(url);
        sb.append(", method=").append(method);
        sb.append(", ipAddress=").append(ipAddress);
        sb.append(", parameter=").append(parameter);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", result=").append(result);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
