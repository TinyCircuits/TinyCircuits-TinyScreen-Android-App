package com.tinycircuits.tinycircuitsble.helpers;


import java.util.ArrayList;

/**
 * Created by Nic on 9/4/2014.
 */
public class DataFormat {

    private static final int MAX_LENGTH = 40;
    private static final int MAX_BYTES = 16;
    /**
     *
     * @param tooLong String to trim. Is truncated if it is greater than {@code MAX_LENGTH} characters
     * @return shortened string based on static requirements
     */
    public static String TrimText(String tooLong){
        if(tooLong.isEmpty() || tooLong == null){
            return "";
        }

        if(tooLong.length() > MAX_LENGTH){
            String result = "";
            result = tooLong.substring(0, Math.min(tooLong.length(), MAX_LENGTH));
            return result;

        }else{
            //just long enough
            return tooLong;
        }
    }

    /**
     *
     * @param tooLong is a string that has too many characters to be sent via bluetooth Gatt.
     *                If the length is greater than {@code MAX_BYTES}, cut it and add it to an
     *                array so it can be sent in separate packets.
     * @return a new string array
     */
    public static String[] ToStringArray(String tooLong){
        ArrayList<String> strings = new ArrayList<String>();
        String temp = "";
        //20 is the max number of bytes that can be sent to ble device
        while(tooLong.length() > MAX_BYTES){
            temp = tooLong.substring(0, Math.min(tooLong.length(),MAX_BYTES));
            strings.add(temp);
            tooLong = tooLong.substring(temp.length(),tooLong.length());
        }
        strings.add(tooLong);

        return strings.toArray(new String[strings.size()]);

    }

    /**
     *
     * @param check checks if this string is null or {@code isEmpty()}
     * @return based on null or empty state
     */
    public static boolean CheckString(String check){
        return (check != null && !check.isEmpty());
    }

}
