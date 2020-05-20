import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class Listen extends ListenerAdapter {

    private JDA mr_jda;
    private Guild mr_guild;
    private long mv_serverId;
    List<User> ma_playerList;
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
        //See if message is werewolf related
        if (!la_content[0].equals("!ww") && (la_content.length == 1)) {
            return;
        }
        //Add players
        if (la_content[1].equals("add") && (mv_gameState == 0) && (la_content.length > 2)) {
            for (int lv_loops = 2; la_content.length >= lv_loops + 1; lv_loops ++) {
                addPlayer(la_content[lv_loops],ir_event.getChannel());
            }
        }
    }

    public void addPlayer (String iv_displayedName, MessageChannel lr_mChannel)  {
        User lv_user = null;
        try {
            lv_user = mr_guild.getMembersByEffectiveName(iv_displayedName, true).get(0).getUser();
        } catch (Exception ex) {
            lr_mChannel.sendMessage(Main.getParameter("translation.csv","user was not found")).queue();
            return;
        }

        if (ma_playerList.isEmpty()) {
            ma_playerList.add(lv_user);
            return;
        }

        for (User lv_addedUser : ma_playerList) {
            if (lv_addedUser.equals(lv_user)) {
                return;
            } else {
                ma_playerList.add(lv_user);
            }
        }
    }

}
