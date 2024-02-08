/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.boringdroid.systemui.utils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class StringUtils {
    /**
     * 非空判斷
     *
     * @param ojb
     * @return
     */
    public static String ToString(Object ojb) {
        if (ojb == null) {
            return "";
        } else {
            return String.valueOf(ojb).trim();
        }
    }

    public static Long ToLong(Object ojb) {
        if (ojb == null) {
            return 0L;
        } else {
            String str = ToString(ojb);
            if (str.contains(".")) {
                String[] arr = str.split(".");
                str = arr[0];
            }
            return Long.valueOf(str);
        }
    }

    public static int ToInt(Object ojb) {
        if (ojb == null) {
            return 0;
        } else {
            return ToDouble(ojb).intValue();
        }
    }

    /**
     * @param date
     * @param IntegerDigits
     * @param FractionDigits
     * @return
     */
    public static String getPercentFormat(double date, int IntegerDigits, int FractionDigits) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumIntegerDigits(IntegerDigits);//小数点前保留几位
        nf.setMinimumFractionDigits(FractionDigits);// 小数点后保留几位
        String str = nf.format(date);
        return str;
    }


    public static Double ToDouble(Object ojb) {
        if (ojb == null) {
            return 0.0;
        } else {
            return Double.valueOf(ToString(ojb));
        }
    }

    public static String ToDecimal(Object value) {
        double time = ToDouble(value);
        return new BigDecimal(time).toPlainString();
    }


    //传入时间戳即可
    public static String conversionTime(Object timeStamp) {
        try {
            //yyyy-MM-dd HH:mm:ss 转换的时间格式  可以自定义
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //转换
            String time = sdf.format(new Date(ToLong(timeStamp)));
            return time;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Long convertDateStringToLong(String dateString, String formatStr) {
        try {
            DateFormat format = new SimpleDateFormat(formatStr);
            Date date = format.parse(dateString);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    //传入指定时间
    public long convertToTimestamp(String time) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            long timestamp = cal.getTimeInMillis();
            return timestamp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getPercentFormat(Object data, int IntegerDigits, int FractionDigits) {
        try {
            double date = ToDouble(data);
            NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMaximumIntegerDigits(IntegerDigits);//小数点前保留几位
            nf.setMinimumFractionDigits(FractionDigits);// 小数点后保留几位
            String str = nf.format(date);
            return str;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0%";
    }

}
