package com.jsls.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpringContextHolder implements ApplicationContextAware, DisposableBean {

    private static ApplicationContext applicationContext = null;
    private static BeanExpressionResolver resolver;
    private static BeanExpressionContext expressionContext;
    private static ConfigurableListableBeanFactory configurableListableBeanFactory;

    /**
     * 解析 SPEL
     *
     * @param value
     * @return
     */
    public static Object resolveExpression(String value) {
        String resolvedValue = resolve(value);
        if (!(resolvedValue.startsWith("#{") && value.endsWith("}"))) {
            return resolvedValue;
        }
        return SpringContextHolder.resolver.evaluate(resolvedValue, SpringContextHolder.expressionContext);
    }

    /**
     * 解析 ${}
     * 
     * @param value
     * @return
     */
    private static String resolve(String value) {
        if (SpringContextHolder.configurableListableBeanFactory != null) {
            return configurableListableBeanFactory.resolveEmbeddedValue(value);
        }
        return value;
    }

    public static void publishEvent(ApplicationEvent event) {
        SpringContextHolder.applicationContext.publishEvent(event);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
        assertContextInjected();
        return applicationContext.getBeansOfType(requiredType);
    }

    /**
     * 从静态变量applicationContext中取得Bean, 自动转型为所赋值对象的类型.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        assertContextInjected();
        return (T) applicationContext.getBean(name);
    }

    /**
     * 从静态变量applicationContext中取得Bean, 自动转型为所赋值对象的类型.
     */
    public static <T> T getBean(Class<T> requiredType) {
        assertContextInjected();
        return applicationContext.getBean(requiredType);
    }

    /**
     * 获取SpringBoot 配置信息
     *
     * @param property     属性key
     * @param defaultValue 默认值
     * @param requiredType 返回类型
     * @return /
     */
    public static <T> T getProperties(String property, T defaultValue, Class<T> requiredType) {
        T result = defaultValue;
        try {
            result = getBean(Environment.class).getProperty(property, requiredType);
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 获取SpringBoot 配置信息
     *
     * @param property 属性key
     * @return /
     */
    public static String getProperties(String property) {
        return getProperties(property, null, String.class);
    }

    /**
     * 获取SpringBoot 配置信息
     *
     * @param property     属性key
     * @param requiredType 返回类型
     * @return /
     */
    public static <T> T getProperties(String property, Class<T> requiredType) {
        return getProperties(property, null, requiredType);
    }

    /**
     * 检查ApplicationContext不为空.
     */
    private static void assertContextInjected() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "applicaitonContext属性未注入, 请在applicationContext.xml中定义SpringContextHolder或在SpringBoot启动类中注册SpringContextHolder.");
        }
    }

    /**
     * 清除SpringContextHolder中的ApplicationContext为Null.
     */
    private static void clearHolder() {
        log.debug("清除SpringContextHolder中的ApplicationContext:" + applicationContext);
        applicationContext = null;
        resolver = null;
        expressionContext = null;
    }

    @Override
    public void destroy() {
        SpringContextHolder.clearHolder();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringContextHolder.applicationContext != null) {
            log.warn("SpringContextHolder中的ApplicationContext被覆盖, 原有ApplicationContext为:"
                    + SpringContextHolder.applicationContext);
        }
        SpringContextHolder.applicationContext = applicationContext;
        BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
            SpringContextHolder.resolver = configurableListableBeanFactory.getBeanExpressionResolver();
            SpringContextHolder.expressionContext = new BeanExpressionContext(configurableListableBeanFactory, null);
        }
    }

    /**
     * 加载配置
     * 
     * @param file
     * @return
     */
    public static Properties loadProperties(File file) throws IOException {
        BufferedReader reader = IOUtils.useTextReader(file, "UTF-8");
        Properties properties = new Properties();
        properties.load(reader);
        IOUtils.closeQuietly(reader);
        return properties;
    }
}