package site.hanschen;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    private Utils() {
    }

    public static String formatDate(String dateString) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            Date date = simpleDateFormat.parse(dateString.substring(0, 10) + " " + dateString.substring(11, 19));
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
            return simpleDateFormat.format(date);
        } catch (ParseException ignored) {
        }
        return dateString;
    }
}
