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
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
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
        try {
            freeMarkerRender(model, templateName, sw);
            return sw.toString();
        } catch (Exception e) {
            logger.error("渲染模板失败：" + e.getMessage(), e);
            throw new RuntimeException("渲染模板失败：" + e.getMessage(), e);
        }

    }

    public static void freeMarkerRender(Object model, String templateName, OutputStream os) {
        Writer out = new OutputStreamWriter(os, Charset.forName("utf-8"));
        try {
            freeMarkerRender(model, templateName, out);
        } catch (Exception e) {
            logger.error("渲染模板失败：" + e.getMessage(), e);
            throw new RuntimeException("渲染模板失败：" + e.getMessage(), e);
        }
    }

    private static void freeMarkerRender(Object model, String templateName, Writer out)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException,
            TemplateException {
        FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
        String path = RenderUtils.class.getResource("/templates").toString();
        freeMarkerConfigurer.setTemplateLoaderPath(path);
        freeMarkerConfigurer.setDefaultEncoding("utf-8");
        Configuration configuration = freeMarkerConfigurer.createConfiguration();
        Template template = configuration.getTemplate(templateName);
        template.process(model, out);
    }
}
