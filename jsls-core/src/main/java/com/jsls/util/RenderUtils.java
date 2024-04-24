package com.jsls.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public class RenderUtils {
    private static final Logger logger = LoggerFactory.getLogger(RenderUtils.class);

    public static String thymeleafRender(Map<String, Object> variables, String templateName) {
        SpringResourceTemplateResolver springResourceTemplateResolver = new SpringResourceTemplateResolver();
        springResourceTemplateResolver.setPrefix("/");
        springResourceTemplateResolver.setSuffix(".html");
        springResourceTemplateResolver.setTemplateMode("HTML");
        springResourceTemplateResolver.setCacheable(false);
        springResourceTemplateResolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
        springTemplateEngine.setTemplateResolver(springResourceTemplateResolver);
        Context context = new Context();
        context.setVariables(variables);
        return springTemplateEngine.process(templateName, context);
    }

    public static String freeMarkerRender(Object model, String templateName) {
        StringWriter sw = new StringWriter();
        freeMarkerRender(model, templateName, sw);
        return sw.toString();
    }

    public static void freeMarkerRender(Object model, String templateName, OutputStream os) {
        Writer out = new OutputStreamWriter(os, Charset.forName("utf-8"));
        freeMarkerRender(model, templateName, out);
    }

    private static void freeMarkerRender(Object model, String templateName, Writer out) {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setDefaultEncoding("utf-8");
        configuration.setClassLoaderForTemplateLoading(ClassUtils.getDefaultClassLoader(), "/templates");
        Template template;
        try {
            template = configuration.getTemplate(templateName);
            template.process(model, out);
        } catch (TemplateNotFoundException e) {
            logger.error(templateName + " 模板不存在", e);
            throw new RuntimeException(templateName + " 模板不存在", e);
        } catch (MalformedTemplateNameException e) {
            logger.error(templateName + " 模板不存在", e);
            throw new RuntimeException(templateName + " 模板不存在", e);
        } catch (ParseException e) {
            logger.error("模板解析异常：" + e.getMessage(), e);
            throw new RuntimeException("模板解析异常：" + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("渲染模板失败：" + e.getMessage(), e);
            throw new RuntimeException("渲染模板失败：" + e.getMessage(), e);
        } catch (TemplateException e) {
            logger.error("渲染模板失败：" + e.getMessage(), e);
            throw new RuntimeException("渲染模板失败：" + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }

    }
}
