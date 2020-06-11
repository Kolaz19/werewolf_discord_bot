import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class Gamestates {

    private Listen mr_base;

    Gamestates (Listen ir_listener) {
        mr_base = ir_listener;
    }

    public void gamestate1 (PrivateMessageReceivedEvent ir_event ) {
        PlayerRoles lr_victim1, lr_victim2;
        if (!isPlayerRole(ir_event.getAuthor(),"werewolf")) {
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

}
