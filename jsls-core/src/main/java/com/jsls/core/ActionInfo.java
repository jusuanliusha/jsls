package com.jsls.core;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import com.jsls.util.WebUtils;

import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
public class ActionInfo {
    /**
     * 操作类型-查询
     */
    public static String TYPE_QUERY = "QUERY";
    /**
     * 操作类型-保存
     */
    public static String TYPE_SAVE = "SAVE";
    /**
     * 操作类型-删除
     */
    public static String TYPE_DELETE = "DELETE";
    /**
     * 操作类型-审核
     */
    public static String TYPE_AUDIT = "AUDIT";
    /**
     * 操作类型-状态切换
     */
    public static String TYPE_SWITCH = "SWITCH";
    /**
     * 操作类型-工作流转
     */
    public static String TYPE_FLOW = "FLOW";
    /**
     * 操作类型-发送
     */
    public static String TYPE_SEND = "SEND";
    /**
     * 操作类型-处理
     */
    public static String TYPE_HANDLE = "HANDLE";
    /**
     * 操作类型-上传
     */
    public static String TYPE_UPLOAD = "UPLOAD";
    /**
     * 操作类型-下载
     */
    public static String TYPE_DNOWLOAD = "DNOWLOAD";

    public static String MODE_DEFAULT = "DEFAULT";
    public static String PART_DEFAULT = "DEFAULT";
    /**
     * 渠道-系统（本系统）
     */
    public static String CHANNEL_SYSTEM = "SYSTEM";
    /**
     * 渠道-来宾（无渠道标识的第三方）
     */
    public static String CHANNEL_GUEST = "GUEST";

    /******************* 保存 ********************/
    /**
     * 模式-添加
     */
    public static String MODE_ADD = "ADD";
    /**
     * 模式-修改
     */
    public static String MODE_EDIT = "EDIT";
    /**
     * 模式-提交
     */
    public static String MODE_SUBMIT = "SUBMIT";
    /**
     * 模式-导入
     */
    public static String MODE_IMPORT = "IMPORT";
    /******************* 查询 ********************/
    /**
     * 模式-查询一条
     */
    public static String MODE_ONE = "ONE";
    /**
     * 模式-查询列表
     */
    public static String MODE_LIST = "LIST";
    /**
     * 模式-分页查询
     */
    public static String MODE_PAGE = "PAGE";
    /**
     * 模式-查询For导出
     */
    public static String MODE_EXPORT = "EXPORT";
    /******************* 开关切换 ********************/
    /**
     * 模式-有效
     */
    public static String MODE_EFFECT = "EFFECT";

    /******************* 发送 ********************/
    /**
     * 模式-邮件
     */
    public static String MODE_EMAIL = "EMAIL";
    /**
     * 模式-短信
     */
    public static String MODE_SMS = "SMS";
    /**
     * 模式-通知
     */
    public static String MODE_NOTICE = "NOTICE";

    /******************* 处理 ********************/
    /**
     * 模式-任务
     */
    public static String MODE_TASK = "TASK";
    /**
     * 模式-事件
     */
    public static String MODE_EVENT = "EVENT";
    /**
     * 模式-消息
     */
    public static String MODE_MESSAGE = "MESSAGE";

    private Date time = new Date();
    private String userId;
    private String userNickName;
    /**
     * 用来区分请求来源
     */
    private String channel = CHANNEL_SYSTEM;
    /**
     * 操作类型 用来区分功能类型
     */
    private String type;
    /**
     * 用来区分相同功能在不同模式下有特殊处理
     */
    private Mark mode = Mark.of(MODE_DEFAULT);
    /**
     * 用来区分相同功能操作在不同页面，不同区域的数据范围，数据模板，权限等不同
     */
    private String part = PART_DEFAULT;

    /**
     * 用来区分是否批量操作
     */
    private boolean mul;

    /**
     * 上级ActionInfo
     * ActionInfo 可以有层级关系，比如审核操作可能有其他派生出子action（发送邮件或短信）
     */
    private ActionInfo parent;
    private static final boolean shiroPresent;
    static {
        shiroPresent = ClassUtils.isPresent("org.apache.shiro.SecurityUtils", ClassUtils.getDefaultClassLoader());
    }

    /**
     * 更新是否批量
     * 
     * @param mul
     */
    public void applyMul(boolean mul) {
        this.mul = mul;
    }

    /**
     * 更新是修改(true)还是新增(false)
     * 
     * @param edit
     */
    public void applySave(boolean edit) {
        if (edit) {
            this.mode.rmSymbol(MODE_ADD);
            this.mode.addSymbol(MODE_EDIT);
        } else {
            this.mode.rmSymbol(MODE_EDIT);
            this.mode.addSymbol(MODE_ADD);
        }
    }

    /**
     * 添加模式
     * 
     * @param modes
     */
    public void addMode(String... modes) {
        this.mode.addSymbol(modes);
    }

    /**
     * 更新mode
     * 
     * @param mark
     */
    public void applyMode(Mark mark) {
        this.mode = mark;
    }

    /**
     * 更新分区
     * 
     * @param part
     */
    public void applyPart(String part) {
        if (StringUtils.hasText(part)) {
            this.part = part;
        }
    }

    /**
     * 更新模式和分区
     * 
     * @param mode
     * @param part
     */
    public void apply(String mode, String part) {
        applyMode(Mark.of(mode));
        applyPart(part);
    }

    /**
     * 更新模式和分区和批量
     * 
     * @param mode
     * @param part
     * @param mul
     */
    public void apply(String mode, String part, boolean mul) {
        apply(mode, part);
        this.mul = mul;
    }

    /**
     * 是否添加
     * 
     * @return
     */
    public boolean isAdd() {
        return hasMode(MODE_ADD);
    }

    /**
     * 是否编辑
     * 
     * @return
     */
    public boolean isEdit() {
        return hasMode(MODE_EDIT);
    }

    /**
     * 是否是提交
     * 
     * @return
     */
    public boolean isSubmit() {
        return hasMode(MODE_SUBMIT);
    }

    /**
     * 是否是导入
     * 
     * @return
     */
    public boolean isImport() {
        return hasMode(MODE_IMPORT);
    }

    /**
     * 是否是指定分区
     * 
     * @param part
     * @return
     */
    public boolean isPart(String part) {
        return part.equals(this.part);
    }

    /**
     * 至少有指定分区列表parts中其中一个
     * 
     * @param parts
     * @return
     */
    public boolean anyPart(String... parts) {
        for (String part : parts) {
            if (isPart(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否有指定的mode
     * 
     * @param mode
     * @return
     */
    public boolean hasMode(String mode) {
        return this.mode.hasSymbol(mode);
    }

    /**
     * 至少有指定modes的其中一个
     */
    public boolean anyMode(String... modes) {
        return this.mode.anySymbol(modes);
    }

    /**
     * 是否是指定的渠道
     * 
     * @param channel
     * @return
     */
    public boolean isChannel(String channel) {
        return channel.equals(this.channel);
    }

    /**
     * 是否是保存
     * 
     * @return
     */
    public boolean isSave() {
        return isAction(TYPE_SAVE);
    }

    /**
     * 是否是审核
     * 
     * @return
     */
    public boolean isAudit() {
        return isAction(TYPE_AUDIT);
    }

    /**
     * 是否是指定的类型
     * 
     * @param type
     * @return
     */
    public boolean isAction(String type) {
        return type.equals(this.type);
    }

    /**
     * 当前是从批量action派生的单项action;
     * 
     * @return
     */
    public boolean isSingleInMul() {
        return !this.mul && this.parent != null && this.parent.mul;
    }

    /**
     * 是否是根actionInfo
     * 
     * @return
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * 创建批量action的单项action
     * 
     * @param type
     * @param mode
     * @return
     */
    public ActionInfo subItem(String type, Mark mode) {
        return subItem(type, mode, this.part);
    }

    /**
     * 创建批量action的单项action
     * 
     * @param type
     * @param mode
     * @param part
     * @return
     */
    public ActionInfo subItem(String type, Mark mode, String part) {
        ActionInfo ninfo = sub(type, mode, this.part);
        ninfo.mul = false;
        return ninfo;
    }

    /**
     * 创建子action
     * 
     * @param type
     * @param mode
     * @return
     */
    public ActionInfo sub(String type, Mark mode) {
        return sub(type, mode, this.part);
    }

    /**
     * 创建子action
     * 
     * @param type
     * @param mode
     * @param part
     * @return
     */
    public ActionInfo sub(String type, Mark mode, String part) {
        ActionInfo ninfo = new ActionInfo();
        ninfo.time = this.time;
        ninfo.userId = this.userId;
        ninfo.userNickName = this.userNickName;
        ninfo.channel = this.channel;
        ninfo.mul = this.mul;
        ninfo.type = type;
        ninfo.mode = mode;
        ninfo.part = part;
        ninfo.parent = this;
        return ninfo;
    }

    /**
     * 分批导入数据，防止数据量太大导入失败
     * 
     * @param <D>
     * @param dataList
     * @param recorder
     * @param action
     * @return
     */
    public <D> Result<Long> processBatchImport(List<D> dataList, Progress progress,
            BiFunction<? super Collection<D>, ActionInfo, Result<Long>> action) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Result.fail("导入数据为空！");
        }
        Result<Void> ivrr = Result.SUCCESS.copy();
        Progress.Recorder recorder = progress.getRecorder();
        progress.processBatch(dataList, batch -> {
            Result<Long> ivr = action.apply(batch, this);
            if (ivr.isSuccess()) {
                recorder.applyBatch(batch.size(), 0);
            } else {
                int failCount = batch.size() - ivr.getData().intValue();
                recorder.applyBatch(batch.size(), failCount);
                ivrr.setCode(ivr.getCode());
                ivrr.setMessage(ivr.getMessage());
            }
        });
        long success = recorder.getSuccessCount();
        if (success == 0) {
            return ivrr.copy();
        }
        return Result.success(success);
    }

    public static ActionInfo useSendAction(String mode) {
        ActionInfo actionInfo = useAction(ActionInfo.TYPE_SEND, mode);
        return actionInfo;
    }

    public static ActionInfo useSwitchAction(String mode) {
        ActionInfo actionInfo = useAction(ActionInfo.TYPE_SWITCH, mode);
        return actionInfo;
    }

    public static ActionInfo useFlowAction(String mode) {
        ActionInfo actionInfo = useAction(ActionInfo.TYPE_FLOW, mode);
        return actionInfo;
    }

    public static ActionInfo useSaveAction() {
        return useSaveAction(MODE_DEFAULT);
    }

    public static ActionInfo useSaveAction(String mode) {
        return useAction(ActionInfo.TYPE_SAVE, mode);
    }

    public static ActionInfo useHandleAction(String mode) {
        ActionInfo actionInfo = new ActionInfo();
        actionInfo.type = TYPE_HANDLE;
        actionInfo.mode = Mark.of(mode);
        actionInfo.userId = "system";
        actionInfo.userNickName = "系统";
        return actionInfo;
    }

    public static ActionInfo useAction(String type) {
        return useAction(type, MODE_DEFAULT);
    }

    public static ActionInfo useAction(String type, String mode) {
        ActionInfo actionInfo = new ActionInfo();
        actionInfo.type = type;
        actionInfo.mode = Mark.of(mode);
        processAction(actionInfo);
        return actionInfo;
    }

    public static void processAction(ActionInfo actionInfo) {
        UserInfo userInfo = currUserInfo();
        actionInfo.userId = userInfo.getUserId() + "";
        actionInfo.userNickName = userInfo.getNickName();
    }

    public static String currUsername() {
        return currUserInfo().getUsername();
    }

    public static String currUserId() {
        return currUserInfo().getUserId() + "";
    }

    public static UserInfo currUserInfo() {
        return shiroPresent ? currShiroUser()
                : currSessionUser(WebUtils.getRequest());
    }

    public static UserInfo currShiroUser() {
        return (UserInfo) SecurityUtils.getSubject().getPrincipal();
    }

    public static UserInfo currSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute(UserInfo.SESSION_USER_ID);
        String username = (String) session.getAttribute(UserInfo.SESSION_USERNAME);
        String nickName = (String) session.getAttribute(UserInfo.SESSION_NICK_NAME);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setNickName(nickName);
        return userInfo;
    }
}
