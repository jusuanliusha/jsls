package com.jsls.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

public class ReflectionUtils {
    /**
     * 获取常量定义并封装成map
     * 
     * @param <T>
     * @param <D>
     * @param clazz
     * @param targetClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T, D> Map<String, D> getConstantMap(Class<T> clazz, Class<D> targetClass) {
        Field[] fields = clazz.getFields();
        Map<String, D> map = new HashMap<>();
        for (Field field : fields) {
            int m = field.getModifiers();
            if (targetClass.isAssignableFrom(field.getType()) && Modifier.isStatic(m) && Modifier.isPublic(m)
                    && Modifier.isFinal(m)) {
                try {
                    Object value = field.get(null);
                    map.put(field.getName(), (D) value);
                } catch (IllegalArgumentException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }
        return map;
    }

    public static Set<Class<?>> findCandidateComponentClasses(String basePackage, TypeFilter includeFilter) {
        return findClasses(basePackage, includeFilter, null, true);
    }

    public static Set<Class<?>> findClasses(String basePackage, TypeFilter includeFilter) {
        return findClasses(basePackage, includeFilter, null, false);
    }

    public static Set<Class<?>> findClasses(String basePackage, TypeFilter includeFilter, boolean candidateComponent) {
        return findClasses(basePackage, includeFilter, null, candidateComponent);
    }

    public static Set<Class<?>> findClasses(String basePackage, TypeFilter includeFilter, TypeFilter excludeFilter,
            boolean candidateComponent) {
        Set<Class<?>> classes = new HashSet<>();
        String resourcePath = ClassUtils.convertClassNameToResourcePath(basePackage);
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath + "/" + "**/*.class";
        try {
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> clazz = ClassUtils.forName(className, classLoader);
                if (excludeFilter != null && excludeFilter.match(metadataReader, metadataReaderFactory)) {
                    continue;
                }
                if (includeFilter != null && !includeFilter.match(metadataReader, metadataReaderFactory)) {
                    continue;
                }
                if (candidateComponent) {
                    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
                    if (!annotationMetadata.isConcrete() || !annotationMetadata.isIndependent()) {
                        continue;
                    }
                }
                classes.add(clazz);
            }
        } catch (IOException e) {
            // ignore
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (LinkageError e) {
            // ignore
        }
        return classes;
    }

    public static Set<Class<?>> findCandidateComponentClasses(String basePackage,
            ClassPathScanningCandidateComponentProvider scanner) {
        Set<Class<?>> classes = new HashSet<>();
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
        for (BeanDefinition candidate : candidates) {
            @SuppressWarnings("null")
            Class<?> clazz = ClassUtils.resolveClassName(candidate.getBeanClassName(),
                    ClassUtils.getDefaultClassLoader());
            classes.add(clazz);
        }
        return classes;
    }

}