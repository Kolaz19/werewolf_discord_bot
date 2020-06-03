import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Listen extends ListenerAdapter {

    private JDA mr_jda;
    private Guild mr_guild;
    private long mv_serverId;
    List<PlayerRoles> ma_playerList;
    private int mv_gameState;


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

        if (la_content[1].equals("restart") && (la_content.length == 2)) {
            mv_gameState = 0;
            ma_playerList.clear();
        }
        //Add players
        if (la_content[1].equals("add") && (mv_gameState == 0) && (la_content.length > 2)) {
            for (int lv_loops = 2; la_content.length >= lv_loops + 1; lv_loops ++) {
                addPlayer(la_content[lv_loops],ir_event.getChannel());
            }
        }
        //Start game = Setting mv_gamestate to 1
        if (la_content[1].equals("start") && (mv_gameState == 0) && (la_content.length == 2)) {
            mv_gameState = 1;
            //TODO Add minimum number of players
            //openPrivateChannel gives a channel instance as a response
            //PrivateChannel tempChannel = ma_playerList.get(0).openPrivateChannel().complete();
            //tempChannel.sendMessage("Hey").queue();
        }

    }

    public void addPlayer (String iv_displayedName, MessageChannel lr_mChannel)  {
        User lr_user = null;
        //Get added player as user, if not, give back error
        try {
            lr_user = mr_guild.getMembersByEffectiveName(iv_displayedName, true).get(0).getUser();
        } catch (Exception ex) {
            String lv_errorOutput = Main.getParameter("translation.csv","user [NAME] was not found");
            lv_errorOutput = lv_errorOutput.replace("[NAME]",iv_displayedName);
            lr_mChannel.sendMessage(lv_errorOutput).queue();
            return;
        }
        //Add player if list is empty
        if (ma_playerList.isEmpty()) {
            ma_playerList.add(new PlayerRoles(lr_user));
            return;
        }
        //Add player if he is not already added
        for (PlayerRoles lr_addedUser : ma_playerList) {
            if (lr_addedUser.mr_user.equals(lr_user)) {
                return;
            } else {
                ma_playerList.add(new PlayerRoles(lr_user));
            }
        }
    }

    public void chooseRoles () {
        int lv_numberOfPlayers = ma_playerList.size();
        int lv_numberOfWolves;
        for (PlayerRoles lv_roles : ma_playerList) {
            lv_roles.nameOfRole = "citizen";
        }

        //First werewolf
        ma_playerList.get(ThreadLocalRandom.current().nextInt(1,lv_numberOfPlayers)).nameOfRole = "werewolf";
        
        if (lv_numberOfPlayers > 6) {
            boolean lv_randomIsWerewolf;
            //If second werewolf is first werewolf (number), roll dice again
            do {
                int lv_randomNumber = ThreadLocalRandom.current().nextInt(1, lv_numberOfPlayers);
                if (ma_playerList.get(lv_randomNumber).nameOfRole.equals("werewolf")) {
                    lv_randomIsWerewolf = true;
                } else {
                    lv_randomIsWerewolf = false;
                }
            } while (lv_randomIsWerewolf == true);
        }



    }

}
