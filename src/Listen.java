import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Listen extends ListenerAdapter {

    private JDA mr_jda;
    private Guild mr_guild;
    private long mv_serverId;
    List<PlayerRoles> ma_playerList;
    private int mv_gameState;
    private int mv_numberOfLivingPeople;
    private int mv_numberOfLivingWerewolves;


    Listen(JDA ir_jda) throws IOException {
        mr_jda = ir_jda;
        mv_serverId = Long.parseLong(Main.getParameter("server.csv", "server_id"));
        mr_guild = mr_jda.getGuildById(mv_serverId);
        ma_playerList = new ArrayList<>();
        mv_gameState = 0;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent ir_event) {

        String[] la_content = ir_event.getMessage().getContentRaw().split(" ");
        //See if message is werewolf related, else stop
        if (!la_content[0].equals("!ww") || (la_content.length == 1)) {
            return;
        }

        if (la_content[1].equals("reset") && (la_content.length == 2)) {
            mv_gameState = 0;
            ma_playerList.clear();
        }

        //TODO command to list players

        //Add players
        if (la_content[1].equals("add") && (mv_gameState == 0) && (la_content.length > 2)) {
            //TODO Give success message when someone was added
            for (int lv_loops = 2; la_content.length >= lv_loops + 1; lv_loops ++) {
                addPlayer(la_content[lv_loops],ir_event.getChannel());
            }
        }
        //Start game = Setting mv_gamestate to 1 at the end
        if (la_content[1].equals("start") && (mv_gameState == 0) && (la_content.length == 2)) {
            if (ma_playerList.size() < 2) { //TODO change back to 5
                ir_event.getChannel().sendMessage(Main.getParameter("translation.csv", "Not enough players to start")).queue();
                return;
            }
            chooseRoles();
            //Send role to every player
            for (PlayerRoles lr_playerRole : ma_playerList) {
                PrivateChannel lr_tempChannel = lr_playerRole.mr_user.openPrivateChannel().complete();
                String lv_whichRole = Main.getParameter("translation.csv","You are a [ROLE]");
                lv_whichRole = lv_whichRole.replace("[ROLE]",lr_playerRole.getNameOfRole());
                lr_tempChannel.sendMessage(lv_whichRole).queue();
            }
            //Send starting text -> players sleep at the end
            for (PlayerRoles lr_playerRole : ma_playerList) {
                PrivateChannel lr_tempChannel = lr_playerRole.mr_user.openPrivateChannel().complete();
                String lv_startingText = Main.getParameter("translation.csv","introduction text (players should sleep at the end)");
                lr_tempChannel.sendMessage(lv_startingText).queueAfter(5,TimeUnit.SECONDS);
                //Send wolves messages to pick first victim
                mv_gameState = 1;
            }

            String lv_chooseFirstVictim;
            if (getNumberOfLivingWerewolves() > 1) {
                lv_chooseFirstVictim = Main.getParameter("translation.csv","Choose the first victim with your brothers");
            } else {
                lv_chooseFirstVictim = Main.getParameter("translation.csv","Pick your first victim");
            }

            for (PlayerRoles lr_playerRole : ma_playerList) {
                if (lr_playerRole.nameOfRole == "werewolf") {
                    lr_playerRole.mr_user.openPrivateChannel().complete().sendMessage(lv_chooseFirstVictim).queueAfter(10,TimeUnit.SECONDS);
                    //TODO List the other werewolves
                }
            }
            
            

        }

    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent ir_event) {

    }

    public void addPlayer (String iv_displayedName, MessageChannel lr_mChannel) {
        User lr_user = null;
        //Get added player as user, if not, give back error
        try {
            lr_user = mr_guild.getMembersByEffectiveName(iv_displayedName, true).get(0).getUser();
        } catch (Exception ex) {
            String lv_errorOutput = Main.getParameter("translation.csv", "user [NAME] was not found");
            lv_errorOutput = lv_errorOutput.replace("[NAME]", iv_displayedName);
            lr_mChannel.sendMessage(lv_errorOutput).queue();
            return;
        }
        PlayerRoles roleToAdd = new PlayerRoles(lr_user);

        //Add player if list is empty
        if (ma_playerList.isEmpty()) {
            ma_playerList.add(roleToAdd);
            return;
        //Add player if he is not already added
        } else if (!ma_playerList.contains(roleToAdd)) {
            ma_playerList.add(roleToAdd);
        }
    }

    public void chooseRoles () {
        int lv_numberOfPlayers = ma_playerList.size();
        boolean lv_randomIsWerewolf;
        for (PlayerRoles lv_roles : ma_playerList) {
            lv_roles.nameOfRole = "citizen";
        }

        //First werewolf (minus 1 because array starts with 0)
        ma_playerList.get(ThreadLocalRandom.current().nextInt(lv_numberOfPlayers)).nameOfRole = "werewolf";
        
        if (lv_numberOfPlayers > 6) {
            //If second werewolf is first werewolf (number), roll dice again
            do {
                int lv_randomNumber = ThreadLocalRandom.current().nextInt(lv_numberOfPlayers);
                if (ma_playerList.get(lv_randomNumber).nameOfRole.equals("werewolf")) {
                    lv_randomIsWerewolf = true;
                } else {
                    ma_playerList.get(lv_randomNumber).nameOfRole = "werewolf";
                    lv_randomIsWerewolf = false;
                }
            } while (lv_randomIsWerewolf == true);
        }

        //Choose witch
        do {
            int lv_randomNumber = ThreadLocalRandom.current().nextInt(lv_numberOfPlayers);
            if (ma_playerList.get(lv_randomNumber).nameOfRole.equals("werewolf")) {
                lv_randomIsWerewolf = true;
            } else {
                ma_playerList.get(lv_randomNumber).nameOfRole = "witch";
                lv_randomIsWerewolf = false;
            }
        } while (lv_randomIsWerewolf == true);

    }

    private void refreshPlayercount () {
        if (ma_playerList.isEmpty()) {
            return;
        }
        mv_numberOfLivingPeople = 0;
        mv_numberOfLivingWerewolves = 0;
        for (PlayerRoles lr_playerRoles : ma_playerList) {
            switch (lr_playerRoles.nameOfRole) {
                case "citizen": mv_numberOfLivingPeople++;
                case "witch": mv_numberOfLivingPeople++;
                case "werewolf" : mv_numberOfLivingWerewolves++;
            }
        }
    }

    public int getNumberOfLivingWerewolves () {
        refreshPlayercount();
        return mv_numberOfLivingWerewolves;
    }
    public int getNumberOfLivingPeople () {
        refreshPlayercount();
        return mv_numberOfLivingPeople;
    }
}





//openPrivateChannel gives a channel instance as a response
//PrivateChannel tempChannel = ma_playerList.get(0).openPrivateChannel().complete();
//tempChannel.sendMessage("Hey").queue();