package com.zov.zovcapture.client;



import com.zov.zovcapture.network.ShopOfferSync;



import java.util.Collections;

import java.util.List;

import java.util.OptionalInt;



public final class ClientEconomyData {

    private static int personalMoney;

    private static int teamMoney;

    private static boolean captain;

    private static String teamName = "";

    private static String playerClassId = "";

    private static List<ShopOfferSync> shopOffers = Collections.emptyList();

    private static boolean shopAtBase;

    private static int teamMoneyPulse;

    private static long teamMoneyPulseExpireMs;



    private ClientEconomyData() {

    }



    public static void update(

            int personal,

            int teamBalance,

            boolean isCaptain,

            String team,

            String classId,

            List<ShopOfferSync> offers,

            boolean atBase,

            int pulse

    ) {

        personalMoney = personal;

        teamMoney = teamBalance;

        captain = isCaptain;

        teamName = team;

        playerClassId = classId != null ? classId : "";

        shopOffers = List.copyOf(offers);

        shopAtBase = atBase;

        if (pulse > 0) {

            teamMoneyPulse = pulse;

            teamMoneyPulseExpireMs = System.currentTimeMillis() + 2500L;

        }

    }



    public static int personalMoney() {

        return personalMoney;

    }



    public static int teamMoney() {

        return teamMoney;

    }



    public static boolean captain() {

        return captain;

    }



    public static String teamName() {

        return teamName;

    }



    public static String playerClassId() {

        return playerClassId;

    }



    public static List<ShopOfferSync> shopOffers() {

        return shopOffers;

    }



    public static boolean shopAtBase() {

        return shopAtBase;

    }



    public static OptionalInt activeTeamMoneyPulse() {

        if (teamMoneyPulse <= 0 || System.currentTimeMillis() > teamMoneyPulseExpireMs) {

            return OptionalInt.empty();

        }

        return OptionalInt.of(teamMoneyPulse);

    }

}


