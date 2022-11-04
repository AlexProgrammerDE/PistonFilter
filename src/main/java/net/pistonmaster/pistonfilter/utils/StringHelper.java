package net.pistonmaster.pistonfilter.utils;

public class StringHelper {
    public static String revertLeet(String str) {
        str = str.toLowerCase();

        str = str.replace("4", "a");
        str = str.replace("&", "a");
        str = str.replace("@", "a");
        str = str.replace("8", "b");
        str = str.replace("(", "c");
        str = str.replace("3", "e");
        str = str.replace("6", "g");
        str = str.replace("9", "g");
        str = str.replace("#", "h");
        str = str.replace("1", "i");
        str = str.replace("!", "i");
        str = str.replace("]", "i");
        str = str.replace("}", "i");
        str = str.replace("0", "o");
        str = str.replace("?", "o");
        str = str.replace("5", "s");
        str = str.replace("$", "s");
        str = str.replace("7", "t");
        str = str.replace("2", "z");

        return str;
    }

    public static boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }
}
