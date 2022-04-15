package io.yapix.parse.parser;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import io.yapix.config.YapixConfig;
import io.yapix.model.DataTypes;
import io.yapix.parse.util.PropertiesLoader;
import io.yapix.parse.util.PsiTypeUtils;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

/**
 * 字段类型工具类.
 */
public final class DataTypeParser {

    private static final String FILE = "yapix/types.properties";
    private final Project project;
    private final Module module;
    private final YapixConfig settings;

    public DataTypeParser(Project project, Module module, YapixConfig settings) {
        this.project = project;
        this.module = module;
        this.settings = settings;
    }

    /**
     * 获取字段类型
     *    1 数组、集合 -> array
     *    2 枚举 -> string
     *    3 其他：根据配置文件 types.properties 中的类型映射，来获得目标类型
     */
    public String parseType(PsiType type) {
        // 数组类型处理
        if (PsiTypeUtils.isArray(type) || PsiTypeUtils.isCollection(type, this.project, this.module))
            return DataTypes.ARRAY;

        boolean isEnum = PsiTypeUtils.isEnum(type);
        if (isEnum)
            return DataTypes.STRING;

        String dataType = getTypeInProperties(type);
        return StringUtils.isEmpty(dataType) ? DataTypes.OBJECT : dataType;
    }

    /**
     * 根据配置文件 types.properties 中的类型映射，来获得目标类型
     *   如 byte=integer / short=integer
     * @param type
     * @return
     */
    public static String getTypeInProperties(PsiType type) {
        Properties typeProperties = PropertiesLoader.getProperties(FILE);
        return typeProperties.getProperty(type.getCanonicalText());
    }

}
