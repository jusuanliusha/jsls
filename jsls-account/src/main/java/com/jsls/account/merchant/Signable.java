package com.jsls.account.merchant;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.BeanWrapperImpl;

import com.jsls.crypto.EncryptUtils;
import com.jsls.util.DateUtils;

public interface Signable {
    default boolean filter(PropertyDescriptor pd){
        Method  method =pd.getReadMethod();
        if(method==null||Object.class.equals(method.getDeclaringClass())){
            return false;
        }
        return true;
    }
    default String useContentForSign(){
        Map<String,Object> orderMap=new TreeMap<String,Object>();
        applyForSign(orderMap);
        return EncryptUtils.buildForSign(orderMap);
    }
    default void applyForSign(Map<String,Object> orderMap){
        BeanWrapperImpl wrapper=new BeanWrapperImpl(this);
        PropertyDescriptor[] pds=wrapper.getPropertyDescriptors();
        if(pds==null||pds.length==0){
            return;
        }
        for(PropertyDescriptor pd:pds){
            if(!filter(pd)){
                continue;
            }
            String propertyName=pd.getName();
           if(!wrapper.isReadableProperty(propertyName)){
                continue; 
           }
           Object value=wrapper.getPropertyValue(propertyName);
           orderMap.put(propertyName, convertValue(value));
        }
    }
    @SuppressWarnings("unchecked")
    public static Object convertValue(Object value){
        if(value==null){
            return null;
        }
        if(value instanceof Signable){
            return convertValue((Signable)value);
        }else if(value instanceof Collection<?>){
            return convertValue((Collection<?>)value);
        }else if(value instanceof Object[]){
            return convertValue((Object[])value);
        }else if(value instanceof Map){
            return convertValue((Map<String,Object>)value);
        }else if(value instanceof Date){
            return DateUtils.formatDate((Date)value, DateUtils.DATE_FORMAT_DATETIME);
        }else if(value instanceof LocalDate){
            Date date=DateUtils.asDate((LocalDate)value);
            return DateUtils.formatDate(date, DateUtils.DATE_FORMAT_DATETIME);
        }else if(value instanceof LocalDateTime){
            Date date=DateUtils.asDate((LocalDateTime)value);
            return DateUtils.formatDate(date, DateUtils.DATE_FORMAT_DATETIME);
        }else{
            String className=value.getClass().getName();
            if(className.startsWith("java.")||className.startsWith("javax.")){
                return value;
            }else{
                throw new RuntimeException(className+" must implements Signable");
            }
        }
    }
    public static Object convertValue(Map<String,Object> map){
        Map<String,Object> orderMap=new TreeMap<String,Object>();
        for(Map.Entry<String,Object> entry:map.entrySet()){
            orderMap.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return orderMap;
    }
    public static Object convertValue(Collection<?> values){
        List<Object> list=new ArrayList<Object>();
        for(Object item:values){
            list.add(convertValue(item));
        }
        return list;
    }
    public static Object convertValue(Object[] values){
        for(int i=0,ln=values.length;i<ln;i++){
            values[i]=convertValue(values[i]);
        }
        return values;
    }
    public static Object convertValue(Signable value){
        Map<String,Object> orderMap=new TreeMap<String,Object>();
        value.applyForSign(orderMap);
        return orderMap;
    }
}
