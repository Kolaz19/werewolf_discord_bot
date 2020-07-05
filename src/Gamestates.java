import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Gamestates {

    private Listen mr_base;
    private PlayerRoles ma_choosenDeath[];
    private boolean mv_witchHasHealPotion, mv_witchHasDeathPotion;

    Gamestates (Listen ir_listener) {
        mr_base = ir_listener;
        ma_choosenDeath = new PlayerRoles[2];
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
        if (ma_choosenDeath[0] == null) {
            ma_choosenDeath[0] = getPlayerRolesFromName(lv_messageContent);
            if (mr_base.getNumberOfLivingWerewolves() > 1) {
                return;
            }
        } else  {
            ma_choosenDeath[1] = getPlayerRolesFromName(lv_messageContent);
            //We have to write the actual victim into index[0] -> we only use index[0] in the next steps
            ma_choosenDeath[0] = ma_choosenDeath[ThreadLocalRandom.current().nextInt(mr_base.getNumberOfLivingWerewolves())];
            ma_choosenDeath[1] = null;
        }

        boolean lv_witchLives = false;
        //Check if witch lives
        for (PlayerRoles lr_playerRole : mr_base.ma_playerList) {
            if (lr_playerRole.nameOfRole.equals("witch")) {
                lv_witchLives = true;
            }
        }
        if ((!lv_witchLives) || (!mv_witchHasHealPotion && !mv_witchHasDeathPotion)) {
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
                    lv_messagePlayerSave = lv_messagePlayerSave.replace("[PLAYER]",ma_choosenDeath[0].mr_user.getName());
                    String lv_messageToWitch = Main.getParameter("translation.csv","A victim was choosen...");
                    lr_privateChannel.sendMessage(lv_messageToWitch).queue();
                    lr_privateChannel.sendMessage(lv_messagePlayerSave).queueAfter(2, TimeUnit.SECONDS);
                } else {
                    mr_base.mv_gameState = 3;
                    String lv_messagePlayerKill = Main.getParameter("translation.csv","Do you want to kill a player? (NO/Playername)");
                    lr_privateChannel.sendMessage(lv_messagePlayerKill).queue();
                    lr_privateChannel.sendMessage(mr_base.getLivingPlayerNames("witch")).queue();
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

        if (lv_messageContent.equalsIgnoreCase("YES")) {
            ma_choosenDeath[0] = null;
            mv_witchHasHealPotion = false;
        }

        if (mv_witchHasDeathPotion) {
            for (PlayerRoles lr_player : mr_base.ma_playerList) {
                if (lr_player.nameOfRole.equals("witch")) {
                    PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                    String lv_messagePlayerKill = Main.getParameter("translation.csv","Do you want to kill a player? (YES/Playername)");
                    lr_privateChannel.sendMessage(lv_messagePlayerKill).queue();
                    lr_privateChannel.sendMessage(mr_base.getLivingPlayerNames("witch")).queue();
                }
            }
        }
        mr_base.mv_gameState = 3;
    }

    public void gamestate3 (PrivateMessageReceivedEvent ir_event) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();

        //Have to make sure that witch responses (if deathpotion avaiable)
        if (mv_witchHasDeathPotion) {
            if (isPlayerRole(ir_event.getAuthor(),"witch")) {
                if (isPlayerLiving(lv_messageContent)) {
                    ma_choosenDeath[1] = getPlayerRolesFromName(lv_messageContent);
                    mv_witchHasDeathPotion = false;
                } else if (!lv_messageContent.equalsIgnoreCase("NO")) {
                    String lv_outputError = Main.getParameter("translation.csv","Player [NAME] does not exist");
                    lv_outputError = lv_outputError.replace("[NAME]",lv_messageContent);
                    ir_event.getAuthor()
                            .openPrivateChannel()
                            .complete()
                            .sendMessage(lv_outputError)
                            .queue();
                    return;
                }
            } else {
                return;
            }

         //Selection of dead players and role reveal 
         String lv_resultsOfNight;
         String lv_resultsRoles;

         if((ma_choosenDeath[0] == null) && (ma_choosenDeath[1] == null)) {
            lv_resultsOfNight = Main.getParameter("translation.csv","No player died that night!");
         } else if ((ma_choosenDeath[0] != null) && (ma_choosenDeath[1] != null)) {
             lv_resultsOfNight = Main.getParameter("translation.csv","[PLAYER1] and [PLAYER2] died that night!");
             lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER1]",ma_choosenDeath[0].mr_user.getName()).replace("[PLAYER2]",ma_choosenDeath[1].mr_user.getName());
             lv_resultsRoles = getTextForRoleReveal(ma_choosenDeath[0]);
             lv_resultsRoles = lv_resultsRoles + "\n" + getTextForRoleReveal(ma_choosenDeath[1]);
             ma_choosenDeath[0].nameOfRole = "dead";
             ma_choosenDeath[1].nameOfRole = "dead";
         } else if ((ma_choosenDeath[0] != null)) {
             lv_resultsOfNight = Main.getParameter("translation.csv","[PLAYER] didn't survive the night!");
             lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER]",ma_choosenDeath[0].mr_user.getName());
             lv_resultsRoles = getTextForRoleReveal(ma_choosenDeath[0]);
             ma_choosenDeath[0].nameOfRole = "dead";
         } else {
             lv_resultsOfNight = Main.getParameter("translation.csv","[PLAYER] didn't survive the night!");
             lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER]",ma_choosenDeath[1].mr_user.getName());
             lv_resultsRoles = getTextForRoleReveal(ma_choosenDeath[0]);
             ma_choosenDeath[1].nameOfRole = "dead";
         }

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
    public PlayerRoles getPlayerRolesFromName(String ir_accountName) {
        PlayerRoles lr_rightPlayer = null;
        for (PlayerRoles lr_player : mr_base.ma_playerList) {
            if (lr_player.mr_user.getName().equals(ir_accountName)) {
                    lr_rightPlayer = lr_player;
            }
        }
        return lr_rightPlayer;
    }
    public String getTextForRoleReveal (PlayerRoles ir_player) {
        String lv_output = null;
        if (ir_player.nameOfRole.equals("werewolf")) {
            lv_output = Main.getParameter("translation.csv","[PLAYER] died as a werewolf=");
            lv_output = lv_output.replace("[PLAYER]",ir_player.mr_user.getName());
        } else {
            lv_output = Main.getParameter("translation.csv","[PLAYER] died as a citizen=");
            lv_output = lv_output.replace("[PLAYER]",ir_player.mr_user.getName());
        }
        return lv_output;
    }

}


//TODO clear array for next message from werewolf (victim)
//TODO Change .equals to equalsIgnoreCase