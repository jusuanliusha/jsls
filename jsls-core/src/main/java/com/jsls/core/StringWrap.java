package com.jsls.core;

public class StringWrap implements CharSequence {
    private String value;
    private String leftPad;
    private String rightPad;

    public StringWrap(String value, String pad) {
        this(value, pad, pad);
    }

    public StringWrap(String value, String leftPad, String rightPad) {
        this.value = value;
        this.leftPad = leftPad;
        this.rightPad = rightPad;
    }

    public String toString() {
        return leftPad + value + rightPad;
    }

    @Override
    public int length() {
        return value.length() + leftPad.length() + rightPad.length();
    }

    @Override
    public char charAt(int index) {
        if (index < 0 && index > length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        if (index < leftPad.length()) {
            return leftPad.charAt(index);
        }
        int rsi = value.length() + leftPad.length();
        if (index >= leftPad.length() && index < rsi) {
            return value.charAt(index - leftPad.length());
        }
        return rightPad.charAt(index - rsi);
    }

    public static StringWrap argsValue(CharSequence... args) {
        StringWrap sw = new StringWrap(String.join(", ", args), "(", ")");
        return sw;
    }

    public static StringWrap StrValue(String value) {
        StringWrap sw = new StringWrap(value, "\"");
        return sw;
    }

    public static StringWrap holdValue(String name) {
        StringWrap sw = new StringWrap(name, "${", "}");
        return sw;
    }

    public static StringWrap sqlValue(String value) {
        StringWrap sw = new StringWrap(value, "'");
        return sw;
    }

    public static StringWrap hqlValue(String name) {
        StringWrap sw = new StringWrap(name, ":", "");
        return sw;
    }

    public static StringWrap mapperValue(String name) {
        StringWrap sw = new StringWrap(name, "#{", "}");
        return sw;
    }

    public static StringWrap mapperValue(String name, String jdbc) {
        StringWrap sw = new StringWrap(name, "#{", " jdbcType=" + jdbc + "}");
        return sw;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException("Unimplemented method 'subSequence'");
    }

}
