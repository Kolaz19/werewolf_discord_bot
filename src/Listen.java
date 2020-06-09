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

    private final JDA mr_jda;
    private final Guild mr_guild;
    private final long mv_serverId;
    private int mv_gameState;
    private int mv_numberOfLivingPeople;
    private int mv_numberOfLivingWerewolves;
    List<PlayerRoles> ma_playerList;


    Listen(JDA ir_jda) throws IOException {
        mr_jda = ir_jda;
        mv_serverId = Long.parseLong(Main.getParameter("server.csv", "server_id"));
        mr_guild = mr_jda.getGuildById(mv_serverId);
        ma_playerList = new ArrayList<>();
        mv_gameState = 0;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent ir_event) {

        String[] la_content = ir_event.getMessage()
                                      .getContentRaw()
                                      .split(" ");
        //See if message is werewolf related, else stop
        if (!la_content[0].equals("!ww") || (la_content.length == 1)) {
            return;
        }

        if (la_content[1].equals("reset") && (la_content.length == 2)) {
            mv_gameState = 0;
            ma_playerList.clear();
        }
        //TODO command to list players
        if (la_content[1].equals("list") && (la_content.length == 2)) {
            String lv_playerListMessage = Main.getParameter("translation.csv","Added players:") + " ";
            if (ma_playerList.isEmpty()) {
                lv_playerListMessage = Main.getParameter("translation.csv","No added players yet");
            } else {
                for (PlayerRoles lr_playerRole : ma_playerList) {
                    lv_playerListMessage = lv_playerListMessage + lr_playerRole.mr_user.getName() + " ";
                }
            }
            ir_event.getChannel().sendMessage(lv_playerListMessage).queue();
        }

        //Add players
        if (la_content[1].equals("add") && (mv_gameState == 0) && (la_content.length > 2)) {
            for (int lv_loops = 2; la_content.length >= lv_loops + 1; lv_loops ++) {
                addPlayer(la_content[lv_loops],ir_event.getChannel());
            }
        }

        if (la_content[1].equals("start") && (mv_gameState == 0) && (la_content.length == 2)) {
            //Check if enough players
            if (ma_playerList.size() < 2) { //TODO change back to 5
                ir_event.getChannel()
                        .sendMessage(Main.getParameter("translation.csv", "Not enough players to start"))
                        .queue();
                return;
            }
            chooseRoles();
            //Send role to every player
            for (PlayerRoles lr_playerRole : ma_playerList) {
                PrivateChannel lr_tempChannel = lr_playerRole.mr_user.openPrivateChannel().complete();
                String lv_whichRole = Main.getParameter("translation.csv","You are a [ROLE]");
                lv_whichRole = lv_whichRole.replace("[ROLE]",lr_playerRole.getNameOfRole());
                lr_tempChannel.sendMessage(lv_whichRole).queue();
                //If not enough werewolves, continue
                if ((getNumberOfLivingWerewolves() == 1) || (!lr_playerRole.nameOfRole.equals("werewolf"))) {
                    continue;
                }
                //Send current werewolf name of partner-werewolf
                for (PlayerRoles lr_playerRole2 : ma_playerList) {
                    if ((lr_playerRole2.nameOfRole.equals("werewolf")) && (lr_playerRole != lr_playerRole2)) {
                        String lv_werewolfFriend = Main.getParameter("translation.csv","[NAME] is your werewolf brother");
                        lv_werewolfFriend = lv_werewolfFriend.replace("[NAME]",lr_playerRole.mr_user.getName());
                        lr_playerRole.mr_user.openPrivateChannel()
                                             .complete()
                                             .sendMessage(lv_werewolfFriend)
                                             .queue();
                    }
                }
            }
            //Send starting text -> players sleep at the end
            for (PlayerRoles lr_playerRole : ma_playerList) {
                PrivateChannel lr_tempChannel = lr_playerRole.mr_user.openPrivateChannel().complete();
                String lv_startingText = Main.getParameter("translation.csv","introduction text (players should sleep at the end)");
                lr_tempChannel.sendMessage(lv_startingText).queueAfter(5,TimeUnit.SECONDS);
            }
            //Choose text based on amount of werewolves
            String lv_chooseFirstVictim;
            if (getNumberOfLivingWerewolves() > 1) {
                lv_chooseFirstVictim = Main.getParameter("translation.csv","Choose the first victim with your brothers");
            } else {
                lv_chooseFirstVictim = Main.getParameter("translation.csv","Pick your first victim");
            }
            //Send wolves the message to choose the first victim
            for (PlayerRoles lr_playerRole : ma_playerList) {
                if (lr_playerRole.nameOfRole == "werewolf") {
                    lr_playerRole.mr_user.openPrivateChannel()
                                         .complete()
                                         .sendMessage(lv_chooseFirstVictim)
                                         .queueAfter(10,TimeUnit.SECONDS);
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
            lr_user = mr_guild.getMembersByEffectiveName(iv_displayedName, true)
                    .get(0)
                    .getUser();
        } catch (Exception ex) {
            String lv_errorOutput = Main.getParameter("translation.csv", "user [NAME] was not found");
            lv_errorOutput = lv_errorOutput.replace("[NAME]", iv_displayedName);
            lr_mChannel.sendMessage(lv_errorOutput).queue();
            return;
        }

        PlayerRoles roleToAdd = new PlayerRoles(lr_user);
        //Add player and give out success message
        if ((ma_playerList.isEmpty()) || (!ma_playerList.contains(roleToAdd))) {
            ma_playerList.add(roleToAdd);
            String lv_nameWasAdded = Main.getParameter("translation.csv","[NAME] was added");
            lv_nameWasAdded = lv_nameWasAdded.replace("[NAME]",lr_user.getName());
            lr_mChannel.sendMessage(lv_nameWasAdded).queue();
        }
    }


    public void chooseRoles () {
        int lv_numberOfPlayers = ma_playerList.size();
        boolean lv_randomIsWerewolf;
        for (PlayerRoles lv_roles : ma_playerList) {
            lv_roles.nameOfRole = "citizen";
        }

        //First werewolf (minus 1 because array starts with 0)
        int lv_numberOfFirstWerewolf = ThreadLocalRandom.current().nextInt(lv_numberOfPlayers);
        ma_playerList.get(lv_numberOfFirstWerewolf).nameOfRole = "werewolf";
        
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