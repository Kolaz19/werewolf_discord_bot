import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

import java.util.List;

public class Gamestates {

    private Listen mr_base;
    private String lv_choosenByWerewolf[][];

    Gamestates (Listen ir_listener) {
        mr_base = ir_listener;
    }

    public void gamestate1 (PrivateMessageReceivedEvent ir_event ) {
        if (!isPlayerRole(ir_event.getAuthor(),"werewolf")) {
            return;
        }
        if (!isPlayerExisting(ir_event.getMessage().getContentRaw())) {
            String lv_outputError = Main.getParameter("translation.csv","Player [NAME] does not exist");
            lv_outputError = lv_outputError.replace("[NAME]",ir_event.getMessage().getContentRaw());
            ir_event.getAuthor()
                    .openPrivateChannel()
                    .complete()
                    .sendMessage(lv_outputError)
                    .queue();
            return;
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

    public boolean isPlayerExisting (String iv_nameToCheck) {
        boolean lv_exists = false;
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if (lr_player.mr_user.getName().equals(iv_nameToCheck)) {
                lv_exists = true;
            }
        }
        return lv_exists;
    }

}
