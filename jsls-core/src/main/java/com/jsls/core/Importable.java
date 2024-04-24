package com.jsls.core;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.jsls.util.DateUtils;

public interface Importable extends Verifiable {

    default void applyRow(Object... values) {
        applyRow(values, Verifiable.MODE_IMPORT);
    }

    void applyRow(Object[] values, String mode);

    default void apply(Map<String, Object> map, Verifiable.Rule rule) {
        applyRow(map, rule, Verifiable.MODE_IMPORT);
    }

    void apply(Map<String, Object> map, Verifiable.Rule rule, String mode);

    /**
     * 转化为属性值
     */
    public static Object usePropertyValue(Object cellValue, Class<?> clazz) {
        if (cellValue == null || clazz.isInstance(cellValue)) {
            return cellValue;
        }
        String useStr = "";
        if (cellValue instanceof String) {
            useStr = (String) cellValue;
            useStr = useStr.trim();
        } else {
            useStr = cellValue.toString();
            if (String.class.equals(clazz)) {
                return useStr.trim();
            }
        }
        if (BigDecimal.class.equals(clazz)) {
            if (!org.springframework.util.StringUtils.hasText(useStr)) {
                return null;
            }
            return new BigDecimal(useStr);
        }
        if (Number.class.isAssignableFrom(clazz)) {
            if (!org.springframework.util.StringUtils.hasText(useStr)) {
                return null;
            }
            Double number = Double.valueOf(useStr);
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
        if (Date.class.equals(clazz)) {
            if (!StringUtils.hasText(useStr)) {
                return null;
            }
            return DateUtils.smartParseDate(useStr);
        }
        return cellValue;
    }

    public static class Position {
        private final int rowIndex;
        private final int cellIndex;

        public Position(int rowIndex, int cellIndex) {
            Assert.state(rowIndex >= 0, "rowIndex 必须大于等于0");
            Assert.state(cellIndex >= 0, "cellIndex 必须大于等于0");
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public int getCellIndex() {
            return cellIndex;
        }

        public Region cellRegion(int cdiff) {
            return new Region(this, new Position(rowIndex, cellIndex + cdiff));
        }

        public Region region(int rdiff, int cdiff) {
            return new Region(this, new Position(rowIndex + rdiff, cellIndex + cdiff));
        }

        public Region rowRegion(int rdiff) {
            return new Region(this, new Position(rowIndex + rdiff, cellIndex));
        }

        public boolean sameCell(Position position) {
            return cellIndex == position.cellIndex;
        }

        public boolean sameRow(Position position) {
            return rowIndex == position.rowIndex;
        }

        public Position right() {
            return right(1);
        }

        public Position right(int i) {
            return new Position(rowIndex, cellIndex + i);
        }

        public Position left() {
            return left(1);
        }

        public Position left(int i) {
            if (cellIndex == 0) {
                return null;
            }
            return new Position(rowIndex, cellIndex - i);
        }

        public Position top() {
            return top(1);
        }

        public Position top(int i) {
            if (rowIndex == 0) {
                return null;
            }
            return new Position(rowIndex - i, cellIndex);
        }

        public Position bottom() {
            return bottom(1);
        }

        public Position bottom(int i) {
            return new Position(rowIndex + i, cellIndex);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cellIndex;
            result = prime * result + rowIndex;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Position other = (Position) obj;
            if (cellIndex != other.cellIndex)
                return false;
            if (rowIndex != other.rowIndex)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Position [rowIndex=" + rowIndex + ", cellIndex=" + cellIndex + "]";
        }

    }

    public static class Region {
        private final Position position1;
        private final Position position2;

        public Region(Position position1, Position position2) {
            this.position1 = position1;
            this.position2 = position2;
        }

        public boolean inRegion(Region region) {
            int maxRowIndex = getMaxRowIndex();
            int minRowIndex = getMinRowIndex();
            if (minRowIndex <= region.getMinRowIndex() && region.getMaxRowIndex() <= maxRowIndex) {
                int maxCellIndex = getMaxCellIndex();
                int minCellIndex = getMinCellIndex();
                if (minCellIndex <= region.getMinCellIndex() && region.getMaxCellIndex() <= maxCellIndex) {
                    return true;
                }
            }
            return false;

        }

        private int getMaxCellIndex() {
            return Math.max(position1.getCellIndex(), position2.getCellIndex());
        }

        private int getMinCellIndex() {
            return Math.min(position1.getCellIndex(), position2.getCellIndex());
        }

        private int getMaxRowIndex() {
            return Math.max(position1.getRowIndex(), position2.getRowIndex());
        }

        private int getMinRowIndex() {
            return Math.min(position1.getRowIndex(), position2.getRowIndex());
        }

        public boolean inRange(Position position) {
            int ri = position.getRowIndex();
            int ci = position.getCellIndex();

            if (inRange(position1.getRowIndex(), position2.getRowIndex(), ri)) {
                if (inRange(position1.getCellIndex(), position2.getCellIndex(), ci)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean inRange(int v1, int v2, int value) {
            int min = Math.min(v1, v2);
            int max = Math.max(v1, v2);
            if (min <= value && value <= max) {
                return true;
            }
            return false;
        }

        public Position getPosition1() {
            return position1;
        }

        public Position getPosition2() {
            return position2;
        }
    }

}
