import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Gamestates {

    private Listen mr_base;
    private String ma_choosenByWerewolf[];
    private boolean mv_witchHasHealPotion, mv_witchHasDeathPotion;

    Gamestates (Listen ir_listener) {
        mr_base = ir_listener;
        ma_choosenByWerewolf = new String[mr_base.getNumberOfLivingWerewolves()];
        mv_witchHasHealPotion = true;
        mv_witchHasDeathPotion = true;
    }

    //Get victims from werewolf, choose victim and send witch message to save or kill someone
    public void gamestate1 ( PrivateMessageReceivedEvent ir_event ) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();
        if (!isPlayerRole(ir_event.getAuthor(),"werewolf")) {
            return;
        }
        if (!isPlayerLiving(ir_event.getMessage().getContentRaw())) {
            String lv_outputError = Main.getParameter("translation.csv","Player [NAME] does not exist");
            lv_outputError = lv_outputError.replace("[NAME]",lv_messageContent);
            ir_event.getAuthor()
                    .openPrivateChannel()
                    .complete()
                    .sendMessage(lv_outputError)
                    .queue();
            return;
        }
        //Add victim -> based on how many werewolves there are
        if (ma_choosenByWerewolf[0] == null) {
            ma_choosenByWerewolf[0] = lv_messageContent;
            if (mr_base.getNumberOfLivingWerewolves() > 1) {
                return;
            }
        } else if (mr_base.getNumberOfLivingWerewolves() > 1) {
            ma_choosenByWerewolf[1] = lv_messageContent;
            //We have to write the actual victim into index[0] -> we only use index[0] in the next steps
            ma_choosenByWerewolf[0] = ma_choosenByWerewolf[ThreadLocalRandom.current().nextInt(mr_base.getNumberOfLivingWerewolves())];
        }

        boolean lv_witchLives = false;
        //Check if witch lives
        for (PlayerRoles lr_playerRole : mr_base.ma_playerList) {
            if (lr_playerRole.nameOfRole.equals("witch")) {
                lv_witchLives = true;
            }
        }
        if ((!lv_witchLives) || (!mv_witchHasDeathPotion && !mv_witchHasDeathPotion)) {
            //TODO jump to gamestate 3
            //TODO have to check again in gamestate3 if death potion is there!
            return;
        }
        //Witch has either heal or death potion, or both.
        //Either send a message to kill or to save a player
        //If she can save the player, ask to kill a player in gamestate2, otherwise, ask here and skip to gamestate3
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if (lr_player.nameOfRole.equals("witch")) {
                PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                if (mv_witchHasHealPotion) {
                    mr_base.mv_gameState = 2;
                    String lv_messagePlayerSave = Main.getParameter("translation.csv","Do you want to save [PLAYER]? (YES/NO)");
                    lv_messagePlayerSave = lv_messagePlayerSave.replace("[PLAYER]",ma_choosenByWerewolf[0]);
                    String lv_messageToWitch = Main.getParameter("translation.csv","A victim was choosen...");
                    lr_privateChannel.sendMessage(lv_messageToWitch).queue();
                    lr_privateChannel.sendMessage(lv_messagePlayerSave).queueAfter(2, TimeUnit.SECONDS);
                } else {
                    mr_base.mv_gameState = 3;
                    String lv_messagePlayerKill = Main.getParameter("translation.csv","Do you want to kill a player? (YES/Playername)");
                    lr_privateChannel.sendMessage(lv_messagePlayerKill).queue();
                    //TODO list player names
                }
            }
        }
    }

    //Only if witch has heal potion
    public void gamestate2 (PrivateMessageReceivedEvent ir_event) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();

        if (!isPlayerRole(ir_event.getAuthor(),"witch") || (!lv_messageContent.equalsIgnoreCase("YES") && !lv_messageContent.equalsIgnoreCase("NO")) ) {
            return;
        }

        if (mv_witchHasHealPotion) {

        }
        
    }

    public boolean isPlayerRole (User ir_userToCheck,String iv_roleToCheck) {
        boolean isRole = false;
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if ((lr_player.mr_user == ir_userToCheck) && (lr_player.nameOfRole.equals(iv_roleToCheck)) ) {
                isRole = true;
            }
        }
        return isRole;
    }

    public boolean isPlayerLiving (String iv_nameToCheck) {
        boolean lv_exists = false;
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if (lr_player.mr_user.getName().equals(iv_nameToCheck) && (lr_player.nameOfRole != "dead")) {
                lv_exists = true;
            }
        }
        return lv_exists;
    }

}


//TODO clear array for next message from werewolf (victim)