package com.jsls.util;

import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.jsls.core.Keyable;
import com.jsls.core.Pair;

public class ValueUtils {

    /**
     * 金额格式化
     * 
     * @param amount 请确保amount为纯金额字符串，没带货币单位
     * @return
     */
    public static String formatDecimal(String amount) {
        if (StringUtils.hasText(amount)) {
            return formatDecimal(new BigDecimal(amount.replaceAll(",", "")));
        }
        return amount;
    }

    /**
     * 金额格式化
     * 
     * @param obj
     * @return
     */
    public static String formatDecimal(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return formatNumber(amount, "###,##0.00");
    }

    /**
     * 数值格式化
     * 
     * @param num 数字类型
     * @return
     */
    public static String formatNumber(Number num, String pattern) {
        if (num == null) {
            return "";
        }
        // DecimalFormat默认使用的是进位方式是RoundingMode.HALF_EVEN，此舍入模式也称为“银行家算法”，主要在美国使用。
        // 银行家算法：四舍六入五考虑，五后非零就进一，五后为零看奇偶，五前为偶应舍去，五前为奇要进一
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(num);
    }

    /**
     * 数值格式化
     * 
     * @param num 数字类型
     * @return
     */
    public static String formatNumber(Number num, String pattern, RoundingMode mode) {
        if (num == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat(pattern);
        df.setRoundingMode(mode);
        return df.format(num);
    }

    /**
     * 
     * @param <A>
     * @param <B>
     * @param <G>
     * @param visitor
     * @param permissionList
     * @param cfn
     * @return
     */
    public static <A, B, G> List<G> combine(Collection<A> alist, Collection<B> blist, BiFunction<A, B, G> cfn) {
        List<G> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(alist) || CollectionUtils.isEmpty(blist)) {
            return list;
        }
        for (A a : alist) {
            for (B b : blist) {
                G g = cfn.apply(a, b);
                if (g != null) {
                    list.add(g);
                }
            }
        }
        return list;
    }

    /**
     * 集合相减A-B
     * 
     * @param <E>
     * @param <K>
     * @param list
     * @param subList
     * @param kfn
     */
    public static <E, K> void subtract(Collection<E> list, Collection<E> subList, Function<E, K> kfn) {
        if (CollectionUtils.isEmpty(list) || CollectionUtils.isEmpty(subList)) {
            return;
        }
        Set<K> ks = new HashSet<K>(subList.size());
        subList.forEach(item -> ks.add(kfn.apply(item)));
        list.removeIf(item -> {
            return ks.contains(kfn.apply(item));
        });
    }

    /**
     * 集合相减A-B
     * 
     * @param <E>
     * @param list
     * @param subList
     */
    public static <E> List<E> subtract(Collection<E> list, Collection<E> subList) {
        List<E> useList = new ArrayList<>(list.size());
        if (CollectionUtils.isEmpty(list)) {
            return useList;
        }
        if (CollectionUtils.isEmpty(subList)) {
            useList.addAll(list);
            return useList;
        }
        Set<E> ks = new HashSet<E>(subList);
        list.forEach(item -> {
            if (!ks.contains(item)) {
                useList.add(item);
            }
        });
        return useList;
    }

    /**
     * 累加化简
     * 
     * @param <D>
     * @param list
     * @param itemFn
     * @return
     */
    public static <D> BigDecimal calcSum(List<D> list, Function<D, BigDecimal> itemFn) {
        return calc(list, itemFn, BigDecimal::add, BigDecimal.ZERO);
    }

    /**
     * 计算化简
     * 
     * @param <D>
     * @param <V>
     * @param list
     * @param itemFn
     * @param calcFn
     * @param init
     * @return
     */
    public static <D, V> V calc(List<D> list, Function<D, V> itemFn,
            BiFunction<V, V, V> calcFn, V init) {
        return reduce(list, (D data, V dest) -> {
            V vi = itemFn.apply(data);
            if (vi != null) {
                dest = calcFn.apply(dest, vi);
            }
            return dest;
        }, init);
    }

    /**
     * 化简
     * 
     * @param <D>
     * @param <R>
     * @param list
     * @param rdfn
     * @return
     */
    public static <D, R> R reduce(Collection<D> list, BiFunction<D, R, R> rdfn) {
        return reduce(list, rdfn, null);
    }

    /**
     * 化简
     * 
     * @param <D>
     * @param <R>
     * @param list
     * @param rdfn
     * @param init
     * @return
     */
    public static <D, R> R reduce(Collection<D> list, BiFunction<D, R, R> rdfn, R init) {
        R r = init;
        if (!CollectionUtils.isEmpty(list)) {
            for (D item : list) {
                r = rdfn.apply(item, r);
            }
        }
        return r;
    }

    /**
     * 转化为属性值
     */
    public static Object usePropertyValue(Object cellValue, Class<?> clazz) {
        if (cellValue == null || clazz.isInstance(cellValue)) {
            return cellValue;
        }
        String numStr;
        if (cellValue instanceof String) {
            numStr = (String) cellValue;
            numStr = numStr.trim();
        } else {
            numStr = cellValue.toString();
            if (String.class.equals(clazz)) {
                return numStr.trim();
            }
        }
        if (BigDecimal.class.equals(clazz)) {
            if (!StringUtils.hasText(numStr)) {
                return null;
            }
            return new BigDecimal(numStr);
        }
        if (Number.class.isAssignableFrom(clazz)) {
            if (!StringUtils.hasText(numStr)) {
                return null;
            }
            Double number = Double.valueOf(numStr);
            if (Integer.class.equals(clazz)) {
                return number.intValue();
            }
            if (Long.class.equals(clazz)) {
                return number.longValue();
            }
            if (Double.class.equals(clazz)) {
                return number;
            }
            if (Short.class.equals(clazz)) {
                return number.shortValue();
            }
            if (Byte.class.equals(clazz)) {
                return number.byteValue();
            }
            if (Float.class.equals(clazz)) {
                return number.floatValue();
            }
            return number;
        }
        return cellValue;
    }

    /**
     * 收集mapper转换的value;
     * 
     * @param <D>
     * @param <V>
     * @param list
     * @param mapper
     * @return
     */
    public static <D, V> List<V> collect(Collection<D> list, Function<? super D, V> mapper) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * 去重条件
     * 
     * @param <D>
     * @param <K>
     * @param list
     * @param keyfn
     * @return
     */
    public static <D, K> List<D> distinct(Collection<D> list, Function<? super D, K> keyfn) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().filter(distinct(keyfn, new HashSet<>())).collect(Collectors.toList());
    }

    /**
     * 去重条件
     * 
     * @param <D>
     * @param <K>
     * @param keyfn
     * @param seen
     * @return
     */
    private static <D, K> Predicate<D> distinct(Function<? super D, K> keyfn, Set<K> seen) {
        return d -> seen.add(keyfn.apply(d));
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param fetcher
     * @param mixins
     */
    public static <K, D, E> void mappingMixins2(Map<K, D> dest,
            Function<Collection<K>, List<E>> fetcher, BiConsumer<Map<K, D>, List<E>> mixins) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        List<E> list = fetcher.apply(dest.keySet());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        mixins.accept(dest, list);
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param keyfn
     * @param fetcher
     * @param mix
     */
    public static <K, D, E extends Keyable<K>> Map<K, D> mappingMixins(Collection<D> data, Function<D, K> keyfn,
            Function<Collection<K>, List<E>> fetcher, BiConsumer<D, E> mix) {
        Map<K, D> dest = map(data, keyfn);
        mappingMixins(dest, fetcher, mix);
        return dest;
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param fetcher
     * @param mix
     */
    public static <K, D, E extends Keyable<K>> void mappingMixins(Map<K, D> dest,
            Function<Collection<K>, List<E>> fetcher, BiConsumer<D, E> mix) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        List<E> list = fetcher.apply(dest.keySet());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (E e : list) {
            D d = dest.get(e.useKey());
            mix.accept(d, e);
        }
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param fetcher
     * @param mix
     */
    public static <K, D, E extends Keyable<K>> void mappingMixinsList(Map<K, D> dest,
            Function<Collection<K>, List<E>> fetcher, BiConsumer<D, List<E>> mix) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        List<E> list = fetcher.apply(dest.keySet());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Map<K, List<E>> mixMap = mapList(list, item -> item.useKey());
        for (Map.Entry<K, List<E>> entry : mixMap.entrySet()) {
            D d = dest.get(entry.getKey());
            mix.accept(d, entry.getValue());
        }
    }

    /**
     * 映射混入
     * 
     * @param <K>
     * @param <V>
     * @param <D>
     * @param map
     * @param dest
     * @param fn
     */
    public static <K, V, D> void mapping(Map<K, V> map, Map<K, D> dest, BiConsumer<V, D> fn) {
        if (CollectionUtils.isEmpty(map) || CollectionUtils.isEmpty(dest)) {
            return;
        }
        for (Map.Entry<K, V> entry : map.entrySet()) {
            D data = dest.get(entry.getKey());
            fn.accept(entry.getValue(), data);
        }
    }

    /**
     * list 转换成目标类型list
     * 
     * @param <D>
     * @param <V>
     * @param list
     * @param fn
     * @return
     */
    public static <D, V> List<V> useList(Collection<? extends D> list, Function<D, V> fn) {
        List<V> use = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return use;
        }
        for (D d : list) {
            V v = fn.apply(d);
            use.add(v);
        }
        return use;
    }

    /**
     * list 转为map(根据key函数生成的key分组)
     * 
     * @param <K>
     * @param <T>
     * @param list
     * @param mk
     * @return
     */
    public static <K, D> Map<K, List<D>> mapList(Collection<? extends D> list, Function<D, K> fn) {
        Map<K, List<D>> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (D d : list) {
            K k = fn.apply(d);
            if (k == null) {
                continue;
            }
            List<D> temp = map.get(k);
            if (temp == null) {
                temp = new ArrayList<>();
                map.put(k, temp);
            }
            temp.add(d);
        }
        return map;
    }

    /**
     * list 转为map
     * 
     * @param <K>
     * @param <T>
     * @param list
     * @param mk   key函数
     * @return
     */
    public static <K, T> Map<K, T> map(Collection<? extends T> list, Function<T, K> mk) {
        Map<K, T> map = new HashMap<>(list.size());
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (T item : list) {
            K k = mk.apply(item);
            if (k == null) {
                continue;
            }
            map.put(k, item);
        }
        return map;
    }

    /**
     * 获取最小值
     *
     * @param list
     * @return
     */
    public static <D extends Comparable<D>> D min(List<D> list) {
        D use = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            D item = list.get(i);
            if (item == null) {
                continue;
            }
            if (use == null || use.compareTo(item) > 0) {
                use = item;
            }
        }
        return use;
    }

    /**
     * 获取最小值
     *
     * @param list
     * @return
     */
    public static <D extends Comparable<D>> D max(List<D> list) {
        D use = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            D item = list.get(i);
            if (item == null) {
                continue;
            }
            if (use == null || use.compareTo(item) < 0) {
                use = item;
            }
        }
        return use;
    }

    /**
     * 获取最小值
     *
     * @param items
     * @return
     */
    public static <D extends Comparable<D>> D min(D... items) {
        D use = null;
        if (items != null) {
            for (D item : items) {
                if (item == null) {
                    continue;
                }
                if (use == null || use.compareTo(item) > 0) {
                    use = item;
                }
            }
        }
        return use;
    }

    /**
     * 获取最大值
     *
     * @param items
     * @return
     */
    public static <D extends Comparable<D>> D max(D... items) {
        D use = null;
        if (items != null) {
            for (D item : items) {
                if (item == null) {
                    continue;
                }
                if (use == null || use.compareTo(item) < 0) {
                    use = item;
                }
            }
        }
        return use;
    }

    /**
     * 累加
     * 
     * @param items
     * @return
     */
    public static Integer sum(Integer... items) {
        return calculate((v1, v2) -> v1 + v2, items);
    }

    /**
     * 累加
     * 
     * @param items
     * @return
     */
    public static Long sum(Long... items) {
        return calculate((v1, v2) -> v1 + v2, items);
    }

    /**
     * 累加
     * 
     * @param items
     * @return
     */
    public static BigDecimal sum(BigDecimal... items) {
        return calculate((v1, v2) -> v1.add(v2), items);
    }

    /**
     * 顺序计算
     * 
     * @param <D>
     * @param fn    计算函数
     * @param items
     * @return
     */
    public static <D> D calculate(BiFunction<D, D, D> fn, D... items) {
        D use = null;
        if (items != null) {
            for (D item : items) {
                if (use == null) {
                    use = item;
                } else if (item != null) {
                    use = fn.apply(use, item);
                }
            }
        }
        return use;
    }

    /**
     * 组合
     */
    public static <C, T> List<Map<C, T>> compose(Map<C, List<T>> groups) {
        List<Map<C, T>> combinationList = new ArrayList<Map<C, T>>();
        for (Map.Entry<C, List<T>> entry : groups.entrySet()) {
            List<T> group = entry.getValue();
            C key = entry.getKey();
            if (CollectionUtils.isEmpty(group)) {
                continue;
            }
            List<Map<C, T>> tempCombinationList = new ArrayList<Map<C, T>>();
            if (CollectionUtils.isEmpty(combinationList)) {
                for (T item : group) {
                    Map<C, T> combination = new HashMap<>();
                    combination.put(key, item);
                    tempCombinationList.add(combination);
                }
            } else {
                for (T item : group) {
                    for (Map<C, T> itemCombination : combinationList) {
                        Map<C, T> combination = new HashMap<>();
                        combination.putAll(itemCombination);
                        combination.put(key, item);
                        tempCombinationList.add(combination);
                    }
                }
            }
            combinationList = tempCombinationList;
        }
        return combinationList;
    }

    /**
     * 分类组合，每个类型拿出一个形成组合
     */
    public static <T> List<List<T>> compose(List<List<T>> groups) {
        List<List<T>> combinationList = new ArrayList<List<T>>();
        for (List<T> group : groups) {
            if (CollectionUtils.isEmpty(group)) {
                continue;
            }
            List<List<T>> tempCombinationList = new ArrayList<List<T>>();
            if (CollectionUtils.isEmpty(combinationList)) {
                for (T item : group) {
                    List<T> combination = new ArrayList<>();
                    tempCombinationList.add(combination);
                    combination.add(item);
                }
            } else {
                for (T item : group) {
                    for (List<T> itemCombination : combinationList) {
                        List<T> combination = new ArrayList<>();
                        tempCombinationList.add(combination);
                        combination.addAll(itemCombination);
                        combination.add(item);
                    }
                }
            }
            combinationList = tempCombinationList;
        }
        return combinationList;
    }

    public static String genNextCode(String maxCode, int ln) {
        int nextCode = 1;
        if (StringUtils.hasText(maxCode)) {
            nextCode = Integer.parseInt(maxCode) + 1;
        }
        return ValueUtils.leftPad(nextCode + "", "0", ln);
    }

    public static <T> void copyProperties(T bean, Map<String, Object> map) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if (!wrapper.isReadableProperty(propertyName) || "class".equals(propertyName)) {
                continue;
            }
            map.put(propertyName, wrapper.getPropertyValue(propertyName));
        }
    }

    public static StringBuilder useMethodArgs(String[] parameterNames, Object[] args) {
        StringBuilder format = new StringBuilder();
        if (parameterNames.length == 1) {
            format.append(ValueUtils.toString(args[0]));
            return format;
        }
        int i = -1;
        for (String parameterName : parameterNames) {
            if (++i != 0) {
                format.append(", ");
            }
            format.append(parameterName).append("=").append(ValueUtils.toString(args[i]));
        }
        return format;
    }

    public static String toString(Object arg) {
        if (arg == null) {
            return String.valueOf(arg);
        }
        Class<?> argClass = arg.getClass();
        String argClassName = argClass.getName();
        if (argClassName.startsWith("java.") || argClassName.startsWith("javax.")
                || argClassName.startsWith("org.springframework.")) {
            return arg.toString();
        }
        if (HttpServletRequest.class.isAssignableFrom(argClass) || HttpServletResponse.class.isAssignableFrom(argClass)
                || HttpSession.class.isAssignableFrom(argClass)) {
            return arg.toString();
        }
        if (argClassName.startsWith("[")) {
            return ValueUtils.toJSONString(arg);
        }
        try {
            Method method = argClass.getMethod("toString");
            if (Object.class.equals(method.getDeclaringClass())) {
                return ValueUtils.toJSONString(arg);
            }
        } catch (Exception e) {
        }
        return arg.toString();
    }

    public static String toJSONString(Object bean) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        simpleModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        mapper.registerModule(simpleModule);
        try {
            return mapper.writeValueAsString(bean);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json系列化解析异常：" + e.getMessage());
        }
    }

    /**
     * json 反系列化
     * <p>
     * 例如
     * TypeReference<Result<List<user>>> resultTypeRef = new
     * TypeReference<Result<List<user>>>() {};
     * 
     * @param <T>
     * @param json
     * @param type
     * @return
     */
    public static <T> T parseObject(String json, TypeReference<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        simpleModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        mapper.registerModule(simpleModule);
        try {
            return mapper.readValue(json, type);
        } catch (JsonMappingException e) {
            throw new RuntimeException("json反系列化解析异常：" + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json反系列化解析异常：" + e.getMessage());
        }
    }

    public static <T> T parseJSON(String json, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        simpleModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_DATETIME)));
        mapper.registerModule(simpleModule);
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonMappingException e) {
            throw new RuntimeException("json反系列化解析异常：" + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json反系列化解析异常：" + e.getMessage());
        }
    }

    public static String rightPad(String text, String pad, int ln) {
        String use = StringUtils.hasText(text) ? text : "";
        for (;;) {
            if (use.length() >= ln) {
                break;
            }
            use = use + pad;
        }
        return use;
    }

    public static String leftPad(String text, String pad, int ln) {
        String use = StringUtils.hasText(text) ? text : "";
        for (;;) {
            if (use.length() >= ln) {
                break;
            }
            use = pad + use;
        }
        return use;
    }

    public static String replace(String text, Function<Integer, Object> fn) {
        Matcher matcher = Pattern.compile("\\{(?:\\s*|(\\d+))\\}").matcher(text);
        int ldi = 0;
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (; matcher.find(ldi);) {
            sb.append(text.substring(ldi, matcher.start()));
            String g1 = matcher.group(1);
            if (StringUtils.hasText(g1)) {
                sb.append(fn.apply(Integer.parseInt(g1)));
            } else {
                sb.append(fn.apply(index++));
            }
            ldi = matcher.end();
        }
        if (ldi == 0) {
            return text;
        }
        sb.append(text.substring(ldi));
        return sb.toString();
    }

    public static String replace(String text, Map<String, Object> model) {
        return replace(text, "\\$\\{(\\w*)\\}", name -> model.containsKey(name) ? model.get(name) : "");
    }

    public static String replace(String text, String regex, Function<String, Object> fn) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int ldi = 0;
        StringBuilder sb = new StringBuilder();
        for (; matcher.find(ldi);) {
            sb.append(text.substring(ldi, matcher.start()));
            sb.append(fn.apply(matcher.group(1)));
            ldi = matcher.end();
        }
        if (ldi == 0) {
            return text;
        }
        sb.append(text.substring(ldi));
        return sb.toString();
    }

    public static Map<String, String> useParamMap(Object bean) {
        return useParamMap(bean, (name, val) -> {
            if (val == null || val instanceof String) {
                return (String) val;
            }
            if (val instanceof LocalDateTime) {
                val = DateUtils.asDate((LocalDateTime) val);
            } else if (val instanceof LocalDate) {
                val = DateUtils.asDate((LocalDate) val);
            }
            if (val instanceof Date) {
                return DateUtils.formatDate((Date) val, DateUtils.DATE_FORMAT_DATETIME);
            }
            return val.toString();
        });
    }

    public static Map<String, String> useParamMap(Object bean, BiFunction<String, Object, String> fn) {
        Map<String, String> paramMap = new HashMap<>();
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();
        if (pds == null || pds.length == 0) {
            return paramMap;
        }
        for (PropertyDescriptor pd : pds) {
            if (!filter(pd)) {
                continue;
            }
            String propertyName = pd.getName();
            if (!wrapper.isReadableProperty(propertyName)) {
                continue;
            }
            Object value = wrapper.getPropertyValue(propertyName);
            String val = fn.apply(propertyName, value);
            if (val != null) {
                paramMap.put(propertyName, val);
            }
        }
        return paramMap;
    }

    private static boolean filter(PropertyDescriptor pd) {
        Method method = pd.getReadMethod();
        if (method == null || Object.class.equals(method.getDeclaringClass())) {
            return false;
        }
        return true;
    }

    public static void fillMap(Object bean, Map<String, Object> map) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();
        if (pds == null || pds.length == 0) {
            return;
        }
        for (PropertyDescriptor pd : pds) {
            Method method = pd.getReadMethod();
            String propertyName = pd.getName();
            if (method == null || Object.class.equals(method.getDeclaringClass())
                    || !wrapper.isReadableProperty(propertyName)) {
                continue;
            }
            Object value = wrapper.getPropertyValue(propertyName);
            map.put(propertyName, value);
        }
    }

    public static Pair<Map<String, Object>, Map<String, Object>> diff(Map<String, Object> oldValueMap,
            Map<String, Object> newValueMap) {
        Map<String, Object> oldDiffMap = new HashMap<String, Object>();
        Map<String, Object> newDiffMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : newValueMap.entrySet()) {
            String key = entry.getKey();
            if (!ObjectUtils.nullSafeEquals(entry.getValue(), oldValueMap.get(key))) {
                oldDiffMap.put(key, oldValueMap.get(key));
                newDiffMap.put(key, entry.getValue());
            }
        }
        Pair<Map<String, Object>, Map<String, Object>> pair = new Pair<Map<String, Object>, Map<String, Object>>();
        pair.setV1(oldDiffMap);
        pair.setV2(newDiffMap);
        return pair;
    }

    public static <T> boolean contains(T dest, T... array) {
        if (array == null || array.length == 0) {
            return false;
        }
        for (T item : array) {
            if (ObjectUtils.nullSafeEquals(item, dest)) {
                return true;
            }
        }
        return false;
    }

    public static String trim(String val) {
        return val == null ? val : val.trim();
    }

    public static <T> T defaultValue(T t, T def) {
        return t == null ? def : t;
    }

    public static String percent(double decimal, int fraction) {
        // 获取格式化对象
        NumberFormat nt = NumberFormat.getPercentInstance();
        // 设置百分数精确度2即保留两位小数
        nt.setMinimumFractionDigits(fraction);
        return nt.format(decimal);
    }

    public static BigDecimal decimalAdd(BigDecimal dec1, BigDecimal dec2) {
        if (dec1 == null || dec2 == null) {
            return dec1 == null ? dec2 : dec1;
        }
        return dec1.add(dec2);
    }

    public static int numberAdd(Integer... nums) {
        int result = 0;
        for (Integer num : nums) {
            if (num != null) {
                result += num.intValue();
            }
        }
        return result;

    }

    public static BigDecimal max(BigDecimal dec1, BigDecimal dec2) {
        if (dec1 == null || dec2 == null) {
            return dec1 == null ? dec2 : dec1;
        }
        return dec1.compareTo(dec2) >= 0 ? dec1 : dec2;
    }

    public static BigDecimal min(BigDecimal dec1, BigDecimal dec2) {
        if (dec1 == null || dec2 == null) {
            return dec1 == null ? dec2 : dec1;
        }
        return dec1.compareTo(dec2) <= 0 ? dec1 : dec2;
    }

    public static <K, V, R> Map<K, R> listToMap(List<V> list, ItemStrategy<K, V, R> itemStrategy) {
        Map<K, R> map = new HashMap<K, R>();
        for (V v : list) {
            K k = itemStrategy.getKey(v);
            R r;
            if (!map.containsKey(k)) {
                r = itemStrategy.whenNewKey(v);
                map.put(k, r);
            } else {
                r = map.get(k);
            }
            itemStrategy.onValue(r, v);
        }
        return map;
    }

    public static <K, T> Map<K, List<T>> listToMap(List<T> list, final KeyStrategy<K, T> keyStrategy) {
        return ValueUtils.listToMap(list, new ValueUtils.ItemStrategy<K, T, List<T>>() {

            @Override
            public K getKey(T v) {
                return keyStrategy.getKey(v);
            }

            @Override
            public List<T> whenNewKey(T v) {
                return new ArrayList<T>();
            }

            @Override
            public void onValue(List<T> r, T v) {
                r.add(v);
            }
        });
    }

    public static interface KeyStrategy<K, V> {
        K getKey(V v);
    }

    public static interface ItemStrategy<K, V, R> extends KeyStrategy<K, V> {
        R whenNewKey(V v);

        void onValue(R r, V v);
    }

    /**
     * 获取堆栈信息
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }
}
