package io.yapix.process.yapi;

import static io.yapix.base.util.NotificationUtils.notifyError;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import io.yapix.action.AbstractAction;
import io.yapix.base.sdk.yapi.YapiClient;
import io.yapix.base.sdk.yapi.model.YapiInterface;
import io.yapix.base.sdk.yapi.response.YapiTestResult.Code;
import io.yapix.config.YapixConfig;
import io.yapix.model.Api;
import io.yapix.process.yapi.config.YapiSettings;
import io.yapix.process.yapi.config.YapiSettingsDialog;
import io.yapix.process.yapi.process.YapiUploader;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 处理Yapi上传入口动作.
 */
public class YapiUploadAction extends AbstractAction {

    public static final String ACTION_TEXT = "Upload To YApi";

    @Override
    public boolean before(AnActionEvent event, YapixConfig config) {
        String projectId = config.getYapiProjectId();
        if (StringUtils.isEmpty(projectId)) {
            notifyError("Config file error", "yapiProjectId must not be empty.");
            return false;
        }
        if (StringUtils.isNotEmpty(config.getYapiProjectToken()) && StringUtils.isEmpty(config.getYapiUrl())) {
            notifyError("Config file error", "yapiUrl must not be empty, when you config yapiProjectToken.");
            return false;
        }
        if (StringUtils.isNotEmpty(config.getYapiProjectToken())) {
            return true;
        }

        Project project = event.getData(CommonDataKeys.PROJECT);
        // 检查yapi的配置
        YapiSettings settings = YapiSettings.getInstance();
        // 如果没有配置，则弹窗让用户填写配置
        if (!settings.isValidate() || Code.OK != settings.testSettings().getCode()) {
            YapiSettingsDialog dialog = YapiSettingsDialog.show(project, event.getPresentation().getText());
            return !dialog.isCanceled();
        }
        return true;
    }

    /**
     * 处理上传
     * @param event
     * @param config
     * @param apis 父类 AbstractAction.actionPerformed() 中解析好的api信息
     */
    @Override
    public void handle(AnActionEvent event, YapixConfig config, List<Api> apis) {
        Integer projectId = Integer.valueOf(config.getYapiProjectId());
        Project project = event.getData(CommonDataKeys.PROJECT);

        // 配置
        YapiSettings settings = YapiSettings.getInstance();
        // 连接yapi server的client
        YapiClient client = createClient(config, settings);
        // 上传器
        YapiUploader uploader = new YapiUploader(client);

        // 异步上传api信息
        super.handleUploadAsync(project, apis,
                api -> {
                    // 上传
                    YapiInterface yapi = uploader.upload(projectId, api);

                    ApiUploadResult result = new ApiUploadResult();
                    result.setCategoryUrl(client.calculateCatUrl(projectId, yapi.getCatid()));
                    if (yapi.getId() != null) {
                        result.setApiUrl(client.calculateInterfaceUrl(projectId, yapi.getId()));
                    } else {
                        result.setApiUrl(result.getCategoryUrl());
                    }
                    return result;
                }, () -> {
                    client.close();
                    return null;
                });
    }

    private YapiClient createClient(YapixConfig config, YapiSettings settings) {
        if (StringUtils.isNotEmpty(config.getYapiProjectToken())) {
            return new YapiClient(config.getYapiUrl(), config.getYapiProjectToken());
        }
        return new YapiClient(settings.getUrl(), settings.getAccount(), settings.getPassword(),
                settings.getLoginWay(), settings.getCookies(), settings.getCookiesTtl());
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setText(ACTION_TEXT);
    }
}
