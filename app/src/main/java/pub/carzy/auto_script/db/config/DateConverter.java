package pub.carzy.auto_script.db.config;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * @author admin
 */
public class DateConverter implements ConvertManager.Converter<Date, Long> {

    /* ===== Room 用 ===== */
    @TypeConverter
    public static Long toDb0(Date d) {
        return d == null ? null : d.getTime();
    }

    @TypeConverter
    public static Date toJava0(Long l) {
        return l == null ? null : new Date(l);
    }

    /* ===== Runtime 用 ===== */
    @Override
    public Class<Date> javaType() {
        return Date.class;
    }

    @Override
    public Class<Long> dbType() {
        return Long.class;
    }

    @Override
    public Long toDb(Date date) {
        return toDb0(date);
    }

    @Override
    public Date toJava(Long value) {
        return toJava0(value);
    }
}

