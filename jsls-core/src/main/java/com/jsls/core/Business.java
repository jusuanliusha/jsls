package com.jsls.core;

import java.util.Collection;
import java.util.function.Function;

import com.jsls.util.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Business<M> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private M model;
    private ActionInfo actionInfo;
    private final Business<?> parent;

    public Business(M model, ActionInfo actionInfo) {
        this(model, actionInfo, null);
    }

    public Business(M model, ActionInfo actionInfo, Business<?> parent) {
        this.model = model;
        this.actionInfo = actionInfo;
        this.parent = parent;
    }

    /**
     * 用于对业务进行横向扩展的观察者分业务
     * <p>
     * 在实际业务开发中经常遇到，业务的处理流程基本一致，但对不同类型有区分处理
     * <p>
     * 比如发估值/分红需求中，不同的系列产品业务流程一样（先导出文件，再发邮件），当不同系列，要导出的文件名称，内容模板可能不同
     * 发邮件的收件人、邮件标题、邮件内容可能不尽相同，这时就需要对不同类型定义PartBusiness（建议通过actionInfo.part区分），
     * 通过主线{@value Business#publish} 选择对应的PartBusiness处理
     */
    public static abstract class PartBusiness<M> extends Business<M> {
        private Business<M> bus;

        public PartBusiness() {
            super(null, null);
            logger.info("new PartBusiness of {}", getClass());
        }

        /**
         * 设置观察者的上级（因为观察者是横向扩展的处理节点，它的上级也可以形象叫做集合了多个观察者的总线）
         * 
         * @param bus
         */
        protected final void setBus(Business<M> bus) {
            if (bus != null) {
                super.model = bus.model;
                super.actionInfo = bus.actionInfo;
            }
            this.bus = bus;
        }

        /**
         * 获取主线Business
         * 
         * @return
         */
        public Business<M> getBus() {
            return this.bus;
        }

        /**
         * 判断当前观察者是否支持此业务
         * 
         * @param bus
         * @return
         */
        public abstract boolean isSupport(Business<M> bus);
    }

    /**
     * 保存业务
     * 
     * @param <M>
     * @param model
     * @return
     */
    public static <M, B> Business<M> useSaveBusiness(M model) {
        ActionInfo actionInfo = ActionInfo.useSaveAction();
        return new Business<>(model, actionInfo);
    }

    /**
     * 保存业务(指定模式)
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M, B> Business<M> useSaveBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_SAVE, mode);
        return new Business<>(model, actionInfo);

    }

    /**
     * 查询
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useQueryBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_QUERY, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 删除
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useDeleteBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_DELETE, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 审核
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useAuditBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_AUDIT, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 业务处理
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useHandleBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_HANDLE, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 上传文件
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useUploadBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_UPLOAD, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 下载文件
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useDownloadBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_DNOWLOAD, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 发送邮件/短信、推送消息
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useSendBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_SEND, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 状态切换 比如：上架/下架、启用/禁用
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useSwitchBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_SWITCH, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 工作流流转
     * 
     * @param <M>
     * @param model
     * @param mode
     * @return
     */
    public static <M> Business<M> useFlowBusiness(M model, String mode) {
        ActionInfo actionInfo = ActionInfo.useAction(ActionInfo.TYPE_FLOW, mode);
        return new Business<>(model, actionInfo);
    }

    /**
     * 通过观察者模式处理
     * <p>
     * 当一个业务的处理流程大体一致，只是在不同类型(一般推荐用{@link ActionInfo.part}区分)上取值/访问资源有不同处理时；建议使用观察着模式进行横向扩展
     * <p>
     * 使用短路模式，当匹配到一个观察着后执行并立即返回
     * @param <R>
     * @param <B>
     * @param partBusinessClass 用于从spring获取满足条件的业务观察者
     * @param service
     * @return
     */
    public <R, B extends PartBusiness<M>> R publish(Class<B> partBusinessClass, Function<B, R> service) {
        Collection<B> partBusinessList = SpringContextHolder.getBeansOfType(partBusinessClass).values();
        return publish(partBusinessList, service);
    }

    /**
     * 通过观察者模式处理
     * <p>
     * 当一个业务的处理流程大体一致，只是在不同类型(一般推荐用{@link ActionInfo.part}区分)上取值/访问资源有不同处理时；建议使用观察着模式进行横向扩展
     * <p>
     * 使用短路模式，当匹配到一个观察着后执行并立即返回
     * @param <R>
     * @param <B>
     * @param partBusinessList
     * @param service
     * @return
     */
    public <R, B extends PartBusiness<M>> R publish(Collection<B> partBusinessList, Function<B, R> service) {
        for (B partBusiness : partBusinessList) {
            Business<M> tempBus = partBusiness.getBus();
            if (partBusiness.isSupport(this)) {
                partBusiness.setBus(this);
                R ret = service.apply(partBusiness);
                partBusiness.setBus(tempBus);
                return ret;
            }
        }
        throw new RuntimeException("没有可处理的业务观察者");
    }

    /**
     * 生成子业务上下文
     * <p>
     * 当业务很复杂是，我们考虑将业务在概念上拆分出即有层级结构的业务处理树，相应的在实现上就需要有生成子业务上下文
     * 
     * @param <M2>
     * @param model
     * @param actionInfo
     * @return
     */
    public <M2> Business<M2> sub(M2 model, ActionInfo actionInfo) {
        return new Business<>(model, actionInfo, this);
    }

    public Business<?> getParent() {
        return this.parent;
    }

    public M getModel() {
        return this.model;
    }

    public ActionInfo getActionInfo() {
        return this.actionInfo;
    }

    /**
     * 是否新增
     */
    public boolean isAdd() {
        return getActionInfo().isAdd();
    }

    /**
     * 是否提交
     * 
     * @return
     */
    public boolean isSubmit() {
        return getActionInfo().isSubmit();
    }

    /**
     * 是否导入
     * 
     * @return
     */
    public boolean isImport() {
        return getActionInfo().isImport();
    }

    /**
     * 是否为指定分区
     * 
     * @return
     */
    public boolean isPart(String part) {
        return getActionInfo().isPart(part);
    }

    /**
     * 是否为指定分区列表parts中任意一个
     * 
     * @return
     */
    public boolean anyPart(String... parts) {
        return getActionInfo().anyPart(parts);
    }

    /**
     * 是否有指定模式
     * 
     * @return
     */
    public boolean hasMode(String mode) {
        return getActionInfo().hasMode(mode);
    }

    /**
     * 是否指定模式列表中的任意一个
     * 
     * @param modes
     * @return
     */
    public boolean anyMode(String... modes) {
        return getActionInfo().anyMode(modes);
    }

    /**
     * 是否是指定渠道
     * 
     * @param channel
     * @return
     */
    public boolean isChannel(String channel) {
        return getActionInfo().isChannel(channel);
    }

    /**
     * 是否是指定action类型
     * 
     * @param type
     * @return
     */
    public boolean isAction(String type) {
        return getActionInfo().isAction(type);
    }

    /**
     * 是否是批量操作
     * 
     * @return
     */
    public boolean isMul() {
        return getActionInfo().isMul();
    }
}
