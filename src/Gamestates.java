import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;

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
    public void gamestate1 (PrivateMessageReceivedEvent ir_event ) {
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
        if (ma_choosenByWerewolf[0].isEmpty()) {
            ma_choosenByWerewolf[0] = lv_messageContent;
            if (mr_base.getNumberOfLivingWerewolves() > 1) {
                return;
            }
        } else if (mr_base.getNumberOfLivingWerewolves() > 1) {
            ma_choosenByWerewolf[1] = lv_messageContent;
        }
        //We have to write the actual victim into index[0] of the array to continue
        //TODO solution if one werewolf dies
        if (ma_choosenByWerewolf.length != 1) {
            ma_choosenByWerewolf[0] = ma_choosenByWerewolf[ThreadLocalRandom.current().nextInt(mr_base.getNumberOfLivingWerewolves())];
        }
        //Send message to witch to save player
        if (mv_witchHasHealPotion == false) {
            return;
        }
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if (lr_player.nameOfRole == "witch") {
                String lv_messageToWitch = Main.getParameter("translation.csv","A victim was choosen...");
                PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                lr_privateChannel.sendMessage(lv_messageToWitch).queue();
                String lv_messagePlayerSave = Main.getParameter("translation.csv","Do you want to save [PLAYER]?");
                lv_messagePlayerSave = lv_messagePlayerSave.replace("[PLAYER]",ma_choosenByWerewolf[0]);
                lr_privateChannel.sendMessage(lv_messagePlayerSave).queueAfter(2, TimeUnit.SECONDS);
            }
        }
        mr_base.mv_gameState = 2;
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
