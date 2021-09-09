package io.yapix.model;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * 参数
 */
public class Property {

    /** 名称 */
    private String name;

    /** 类型 */
    private String type;

    /** 时间格式 */
    private String dateFormat;

    /** 描述 */
    private String description;

    /** 参数位置 */
    private ParameterIn in;

    /** 是否必须 */
    private Boolean required;

    /** 是否标记过期 */
    private Boolean deprecated;

    /** 请求示例 */
    private String example;

    /** 响应mock */
    private String mock;

    /** 默认值 */
    private String defaultValue;

    /** 当type为array */
    private Property items;

    /** 当type为object */
    private Map<String, Property> properties;

    public boolean isArrayType() {
        return DataTypes.ARRAY.equals(type);
    }

    public boolean isObjectType() {
        return DataTypes.OBJECT.equals(type);
    }

    /**
     * 合并自定义配置, 自定义优先
     */
    public void mergeCustom(Property custom) {
        if (StringUtils.isNotEmpty(custom.getName())) {
            this.name = custom.getName();
        }
        if (StringUtils.isNotEmpty(custom.getType())) {
            this.type = custom.getType();
        }
        if (StringUtils.isNotEmpty(custom.getDescription())) {
            this.description = custom.getDescription();
        }
        if (custom.getRequired() != null) {
            this.required = custom.getRequired();
        }
        if (custom.getDeprecated() != null) {
            this.deprecated = custom.getDeprecated();
        }
        if (StringUtils.isNotEmpty(custom.getDefaultValue())) {
            this.defaultValue = custom.getDefaultValue();
        }
        if (StringUtils.isNotEmpty(custom.getExample())) {
            this.example = custom.getExample();
        }
        if (StringUtils.isNotEmpty(custom.getMock())) {
            this.mock = custom.getMock();
        }
    }

    //---------------------------generated-------------------------------//


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ParameterIn getIn() {
        return in;
    }

    public void setIn(ParameterIn in) {
        this.in = in;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
    }

    public Property getItems() {
        return items;
    }

    public void setItems(Property items) {
        this.items = items;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
