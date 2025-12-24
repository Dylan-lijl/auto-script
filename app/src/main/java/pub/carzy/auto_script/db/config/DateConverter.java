package pub.carzy.auto_script.db.config;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * @author admin
 */
public class DateConverter {

    @TypeConverter
    public static Long dateToLong(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date longToDate(Long value) {
        return value == null ? null : new Date(value);
    }
}

