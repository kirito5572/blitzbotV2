package me.kirito5572.objects.main;

import java.util.ArrayList;
import java.util.List;

public class OptionData {
    //ONLY AWS SQL SERVER data
    private static List<String> complainBanUserList = new ArrayList<>();

    @SuppressWarnings("unused") public static List<String> getComplainBanUserList() {
        return complainBanUserList;
    }

    public static void setComplainBanUserList(List<String> complainBanUserList) {
        OptionData.complainBanUserList = complainBanUserList;
    }
}
