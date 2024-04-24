package com.jsls.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public interface Verifiable {
    public static final String MODE_DEFAULT="DEFAULT";
    public static final String MODE_IMPORT="IMPORT";
    public static final String MODE_INSERT="INSERT";
    public static final String MODE_UPDATE="UPDATE";
    default Result<Void> validate(){
        return validate(MODE_DEFAULT);
    }
    default Result<Void> validate(String mode){
        List<String> messages=new ArrayList<>();
        Rule rule=new Rule(messages);
        applyRule(rule, mode);
        return rule.useResult();
    }
    void applyRule(Rule rule, String mode);
    default boolean applyRule(Rule rule, String mode,Function<String,String> msgFn){
        Function<String,String> pre=rule.messageFn;
        rule.messageFn=msgFn;
        int start=rule.messages.size();
        applyRule(rule, mode);
        rule.messageFn=pre;
        return rule.messages.size()>=start;
    }
    public static class Rule{
        private final List<String> messages;
        private Function<String,String> messageFn;
        private Map<String,Object> stateMap=new HashMap<>();
        public Rule(){
            this(new ArrayList<>());
        }
        public Rule(List<String> messages){
            this.messages=messages;
        }
        public boolean isSuccess(){
            return messages.isEmpty();
        }
        public Result<Void> useResult(){
            if(!messages.isEmpty()){
                return Result.fail(messages.toString());
            }
            return Result.SUCCESS;
        }
        @SuppressWarnings("unchecked")
        public <S> S getState(String key){
            return (S)stateMap.get(key);
        }
        public void setState(String key,Object state){
            stateMap.put(key,state);
        }
        public boolean withIn(Function<String,String> msgFn,Runnable runnable){
            Function<String,String> pre=this.messageFn;
            this.messageFn=msgFn;
            int start=this.messages.size();
            runnable.run();
            this.messageFn=pre;
            return this.messages.size()>=start;
        }
        public boolean applyList(Collection<? extends Verifiable> list,String mode,String name){
            if(CollectionUtils.isEmpty(list)){
                return true;
            }
            int startSize=messages.size();
            Function<String,String> pre=this.messageFn;
            int i=0;
            for(Verifiable item:list){
                i++;
                int usei=i;
                this.messageFn=message->{
                    String temp="第"+usei+"个"+name+message;
                    return pre!=null?pre.apply(temp):temp;
                };
                item.applyRule(this, mode);
            }
            this.messageFn=pre;
            return messages.size()==startSize;
        }
        private void addMessage(String message){
            if(messageFn!=null){
                message=messageFn.apply(message);
            }
            messages.add(message);
        }
        public boolean hasText(String v,String message){
            if(!StringUtils.hasText(v)){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean notEmptyAny(String message,Object...vs){
            boolean flag=false;
            for(Object v:vs){
                if(v==null){
                    continue;
                }
                if(v instanceof String){
                    if(StringUtils.hasText((String)v)){
                        flag=true;
                        break;
                    }
                }else if(!isEmpty(v)){
                    flag=true;
                    break;
                }
            }
            if(!flag){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean startsWith(String v,String head,String message){
            return expect(v, head, (a,b)->a.startsWith(b), message);
        }
        public boolean endsWith(String v,String tail,String message){
            return expect(v, tail, (a,b)->a.endsWith(b), message);
        }
        public boolean mobile(String mobile,String message){
            if(isEmpty(mobile)||validateMobile(mobile)){
                return true;
            }
            addMessage(message);
            return false;
        }
        public boolean mobile(String mobile){
            return mobile(mobile,"手机号格式不正确！");
        }
        public boolean email(String email,String message){
            if(isEmpty(email)||validateEmail(email)){
                return true;
            }
            addMessage(message);
            return false;
        }
        public boolean email(String email){
            return email(email,"邮箱格式不正确！");
        }
        public boolean digit(String text,String message){
            if(isEmpty(text)||validateDigit(text)){
                return true;
            }
            addMessage(message);
            return false;
        }
        public boolean regex(String text,String regex,String message){
            if(isEmpty(text)||isEmpty(regex)||validateRegex(text,regex)){
                return true;
            }
            addMessage(message);
            return false;
        }
        public boolean regex(String text,String regex,String flags,String message){
            if(isEmpty(text)||isEmpty(regex)||validateRegex(text,regex,flags)){
                return true;
            }
            addMessage(message);
            return false;
        }
        public boolean contains(String v,String key,String message){
            return expect(v, key, (a,b)->a.contains(b), message);
        }
        public <T> boolean contains(T v,String message,@SuppressWarnings("unchecked") T...vals){
            return expect(vals, v, (a,b)->{
                for(T item:a){
                    if(b.equals(item)){
                        return true;
                    }
                }
                return false;
            }, message);
        }
        public boolean contains(Collection<?> v,String key,String message){
            return expect(v, key, (a,b)->a.contains(b), message);
        }
        public boolean contains(Map<?,?> v,String key,String message){
            return expect(v, key, (a,b)->a.containsKey(b), message);
        }
        public boolean length(String v,int min,int max,String message){
            if(isEmpty(v)){
                return true;
            }
            int ln=v.length();
            if(min>ln||max>0&&max<ln){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean size(Collection<?> v,int min,int max,String message){
            if(v==null||v.isEmpty()){
                return true;
            }
            int ln=v.size();
            if(min>ln||max>0&&max<ln){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean size(Map<?,?> v,int min,int max,String message){
            if(v==null||v.isEmpty()){
                return true;
            }
            int ln=v.size();
            if(min>ln||max>0&&max<ln){
                addMessage(message);
                return false;
            }
            return true;
        }
        public <V extends Comparable<V>> boolean range(V v,V min,V max,String message){
            if(isEmpty(v)){
                return true;
            }
            if(min!=null&&min.compareTo(v)>0
            ||max!=null&&max.compareTo(v)<0){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean notNull(Object v,String message){
            if(v==null){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean notEmpty(Collection<?> v,String message){
            if(v==null||v.isEmpty()){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean notEmpty(Map<?,?> v,String message){
            if(v==null||v.isEmpty()){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean eq(Object v1,Object v2,String message){
            if(!ObjectUtils.nullSafeEquals(v1, v2)){
                addMessage(message);
                return false;
            }
            return true;
        }
        public <V extends Comparable<V>> boolean gt(V v1,V v2,String message){
            return expect(v1, v2, (a1,a2)->a1.compareTo(a2)>0, message);
        }
        public <V extends Comparable<V>> boolean ge(V v1,V v2,String message){
            return expect(v1, v2, (a1,a2)->a1.compareTo(a2)>=0, message);
        }
        public <V extends Comparable<V>> boolean lt(V v1,V v2,String message){
            return expect(v1, v2, (a1,a2)->a1.compareTo(a2)<0, message);
        }
        public <V extends Comparable<V>> boolean le(V v1,V v2,String message){
            return expect(v1, v2, (a1,a2)->a1.compareTo(a2)<=0, message);
        }
        public <V,X> boolean expect(V v,X x,BiFunction<V,X,Boolean> expect,String message){
            if(isEmpty(v)||isEmpty(x)){
                return true;
            }
            if(!expect.apply(v,x)){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean truely(boolean v,String message){
            if(!v){
                addMessage(message);
                return false;
            }
            return true;
        }
        public boolean falsely(boolean v,String message){
            if(v){
                addMessage(message);
                return false;
            }
            return true;
        }
    }
    public static boolean isEmpty(CharSequence text){
        return text==null||text.length() == 0;
    }
    public static boolean isEmpty(Object[] arr){
        return arr==null||arr.length == 0;
    }
    @SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof Optional) {
			return !((Optional) obj).isPresent();
		}
		if (obj instanceof CharSequence) {
			return ((CharSequence) obj).length() == 0;
		}
		if (obj.getClass().isArray()) {
			return Array.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection) obj).isEmpty();
		}
		if (obj instanceof Map) {
			return ((Map) obj).isEmpty();
		}

		// else
		return false;
	}
    /**
	 * 验证手机号
	 * 
	 * @param mobile
	 * @return
	 */
	public static boolean validateMobile(String mobile) {
		return validateRegex(mobile, "^1\\d{10}$");
	}

	/**
	 * 是否数字
	 * 
	 * @param text
	 * @return
	 */
	public static boolean validateDigit(String text) {
		return validateRegex(text, "^\\d+$");
	}

	/**
	 * 验证Number类型
	 */
	public static boolean validateNumber(String text) {
		return validateRegex(text, "^([1-9]\\d*|0)(\\.\\d+)?$");
	}

	/**
	 * 验证邮箱
	 */
	public static boolean validateEmail(String email) {
		return validateRegex(email, "^\\w+@\\w+(\\.\\w+)*$");
	}
	
	/**
	 * 正则表达式验证
	 */
	public static boolean validateRegex(String text, String regex, String flags) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		int useFlags = 0;
		if (flags.contains("i") || flags.contains("I")) {
			useFlags |= Pattern.CASE_INSENSITIVE;
		}
		if (flags.contains("m") || flags.contains("M")) {
			useFlags |= Pattern.MULTILINE;
		}
		if (flags.contains("g") || flags.contains("G")) {
			useFlags |= Pattern.DOTALL;
		}
		return Pattern.compile(regex, useFlags).matcher(text).matches();
	}

	/**
	 * 正则表达式验证
	 */
	public static boolean validateRegex(String text, String regex, int flags) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		return Pattern.compile(regex, flags).matcher(text).matches();
	}

	/**
	 * 正则表达式验证
	 */
	public static boolean validateRegex(String text, String regex) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		return Pattern.matches(regex, text);
	}
	/**
	 * 正则匹配查找
	 * @param text
	 * @param regex
	 * @return
	 */
	public static boolean matchFind(String text,String regex){
		return Pattern.compile(regex).matcher(text).find();
	}
}
