package net.pistonmaster.pistonfilter.utils;

public class StringHelper {
    public static String toLeetPattern(String str) {
        str = str.toUpperCase();

        char[] english = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        String[] leet = {"[A4]", "[B8]", "[C\\(]", "[D\\)]", "[E3]", "[F\\}]", "[G6]", "[H#]", "[I!]", "[J\\]]", "[KX]", "[L|]", "[M]", "[N]", "[O0]", "[P9]", "[Q]", "[R2]", "[SZ]", "[T7]", "[UM]", "[V]", "[W]", "[X]", "[J]", "[Z]"};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char tmp = str.charAt(i);
            boolean foundMatch = false;

            for (int j = 0; j < english.length; j++) {
                if (tmp == english[j]) {
                    result.append(leet[j]);
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                result.append("\\").append(tmp);
            }
        }

        return result.toString();
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
