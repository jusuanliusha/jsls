package com.jsls.util;

import java.awt.Color;
import java.util.Map;

import org.springframework.util.StringUtils;

public class StyleUtils {
    private static final Map<String, Color> constantMap =ReflectionUtils.getConstantMap(Color.class, Color.class);
    public static Color convertColor(String source) {
		if (StringUtils.hasText(source)) {
			if (source.startsWith("#")) {
				source = source.substring(1);
			}
			if (constantMap.containsKey(source)) {
				return constantMap.get(source);
			} else {
				return Color.decode("0x" + source);
			}
		}
		return null;
	}
}
