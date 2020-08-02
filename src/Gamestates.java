import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Gamestates {

    private Listen mr_base;
    private PlayerRole[] ma_chosenDeath;
    private PlayerRole[][] ma_chosenHanged;
    private boolean mv_witchHasHealPotion, mv_witchHasDeathPotion;
    private boolean mv_secondVote;

    Gamestates(Listen ir_listener) {
        mr_base = ir_listener;
        ma_chosenDeath = new PlayerRole[2];
        mv_witchHasHealPotion = true;
        mv_witchHasDeathPotion = true;
        mv_secondVote = false;
    }

    //Get victims from werewolf, choose victim and send witch message to save or kill someone
    public void gamestate1(PrivateMessageReceivedEvent ir_event) {

        String lv_messageContent = ir_event.getMessage().getContentRaw();
        if (!isPlayerRole(ir_event.getAuthor(), "werewolf")) {
            return;
        }
        if (!isPlayerLiving(ir_event.getMessage().getContentRaw())) {
            String lv_outputError = Main.getParameter("translation.csv", "Player [NAME] does not exist");
            lv_outputError = lv_outputError.replace("[NAME]", lv_messageContent);
            ir_event.getAuthor()
                    .openPrivateChannel()
                    .complete()
                    .sendMessage(lv_outputError)
                    .queue();
            return;
        }
        //Add victim -> based on how many werewolves there are
        if (ma_chosenDeath[0] == null) {
            ma_chosenDeath[0] = getPlayerRoleFromName(lv_messageContent);
            if (mr_base.getNumberOfLivingWerewolves() > 1) {
                return;
            }
        } else {
            ma_chosenDeath[1] = getPlayerRoleFromName(lv_messageContent);
            //We have to write the actual victim into index[0] -> we only use index[0] in the next steps
            ma_chosenDeath[0] = ma_chosenDeath[ThreadLocalRandom.current().nextInt(mr_base.getNumberOfLivingWerewolves())];
            ma_chosenDeath[1] = null;
        }

        boolean lv_witchLives = false;
        //Check if witch lives
        for (PlayerRole lr_playerRole : mr_base.ma_playerList) {
            if (lr_playerRole.nameOfRole.equals("witch")) {
                lv_witchLives = true;
            }
        }
        if ((!lv_witchLives) || (!mv_witchHasHealPotion && !mv_witchHasDeathPotion)) {
            //For gamestate3 (check if deathpotion avaiable)
            mv_witchHasDeathPotion = false;
            gamestate3(ir_event);
            return;
        }
        //Witch has either heal or death potion, or both.
        //Either send a message to kill or to save a player
        //If she can save the player, ask to kill a player in gamestate2, otherwise, ask here and skip to gamestate3
        for (PlayerRole lr_player : mr_base.ma_playerList) {
            if (lr_player.nameOfRole.equals("witch")) {
                PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                if (mv_witchHasHealPotion) {
                    mr_base.mv_gameState = 2;
                    String lv_messagePlayerSave = Main.getParameter("translation.csv", "Do you want to save [PLAYER]? (YES/NO)");
                    lv_messagePlayerSave = lv_messagePlayerSave.replace("[PLAYER]", ma_chosenDeath[0].mr_user.getName());
                    lv_messagePlayerSave = "`" + lv_messagePlayerSave + "`";
                    String lv_messageToWitch = Main.getParameter("translation.csv", "A victim was chosen...");
                    lr_privateChannel.sendMessage(lv_messageToWitch).queue();
                    lr_privateChannel.sendMessage(lv_messagePlayerSave).queueAfter(2, TimeUnit.SECONDS);
                } else {
                    mr_base.mv_gameState = 3;
                    String lv_messagePlayerKill = Main.getParameter("translation.csv", "Do you want to kill a player? (NO/Playername)");
                    lv_messagePlayerKill = "`" + lv_messagePlayerKill + "`";
                    lr_privateChannel.sendMessage(lv_messagePlayerKill).queue();
                    lr_privateChannel.sendMessage(mr_base.getLivingPlayerNames("witch")).queue();
                }
            }
        }
    }
    //Only if witch has heal potion
    public void gamestate2(PrivateMessageReceivedEvent ir_event) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();

        if (!isPlayerRole(ir_event.getAuthor(), "witch") || (!lv_messageContent.equalsIgnoreCase("YES") && !lv_messageContent.equalsIgnoreCase("NO"))) {
            return;
        }

        if (lv_messageContent.equalsIgnoreCase("YES")) {
            ma_chosenDeath[0] = null;
            mv_witchHasHealPotion = false;
        }

        if (mv_witchHasDeathPotion) {
            for (PlayerRole lr_player : mr_base.ma_playerList) {
                if (lr_player.nameOfRole.equals("witch")) {
                    PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                    String lv_messagePlayerKill = Main.getParameter("translation.csv", "Do you want to kill a player? (NO/Playername)");
                    lv_messagePlayerKill = "`" + lv_messagePlayerKill + "`";
                    lr_privateChannel.sendMessage(lv_messagePlayerKill).queue();
                    lr_privateChannel.sendMessage(mr_base.getLivingPlayerNames("witch")).queue();
                }
            }
        }
        mr_base.mv_gameState = 3;
    }
    //Chose killed player/s and present results
    public void gamestate3(PrivateMessageReceivedEvent ir_event) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();


        //Have to make sure that witch responses (if deathpotion avaiable)
        if (mv_witchHasDeathPotion) {
            if (isPlayerRole(ir_event.getAuthor(), "witch")) {
                if (isPlayerLiving(lv_messageContent)) {
                    ma_chosenDeath[1] = getPlayerRoleFromName(lv_messageContent);
                    mv_witchHasDeathPotion = false;
                } else if (!lv_messageContent.equalsIgnoreCase("NO")) {
                    String lv_outputError = Main.getParameter("translation.csv", "Player [NAME] does not exist");
                    lv_outputError = lv_outputError.replace("[NAME]", lv_messageContent);
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
        }

        //Selection of dead players and role reveal
        String lv_resultsOfNight;
        String lv_resultsRoles = "";

        //Check if witch and werewolf chose the same victim
        if (ma_chosenDeath[0] == ma_chosenDeath[1]) {
            ma_chosenDeath[1] = null;
        }

        if ((ma_chosenDeath[0] == null) && (ma_chosenDeath[1] == null)) {
            lv_resultsOfNight = Main.getParameter("translation.csv", "No player died that night!");
        } else if ((ma_chosenDeath[0] != null) && (ma_chosenDeath[1] != null)) {
            lv_resultsOfNight = Main.getParameter("translation.csv", "[PLAYER1] and [PLAYER2] died that night!");
            lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER1]", ma_chosenDeath[0].mr_user.getName()).replace("[PLAYER2]", ma_chosenDeath[1].mr_user.getName());
            lv_resultsOfNight = "```diff" + "\n"  + "- "  + lv_resultsOfNight+ "\n" + "```";
            lv_resultsRoles = getTextForRoleReveal(ma_chosenDeath[0]);
            lv_resultsRoles = lv_resultsRoles + "\n" + getTextForRoleReveal(ma_chosenDeath[1]);
            ma_chosenDeath[0].nameOfRole = "dead";
            ma_chosenDeath[1].nameOfRole = "dead";
        } else if ((ma_chosenDeath[0] != null)) {
            lv_resultsOfNight = Main.getParameter("translation.csv", "[PLAYER] didn't survive the night!");
            lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER]", ma_chosenDeath[0].mr_user.getName());
            lv_resultsOfNight = "```diff" + "\n"  + "- "  + lv_resultsOfNight+ "\n" + "```";
            lv_resultsRoles = getTextForRoleReveal(ma_chosenDeath[0]);
            ma_chosenDeath[0].nameOfRole = "dead";
        } else {
            lv_resultsOfNight = Main.getParameter("translation.csv", "[PLAYER] didn't survive the night!");
            lv_resultsOfNight = lv_resultsOfNight.replace("[PLAYER]", ma_chosenDeath[1].mr_user.getName());
            lv_resultsOfNight = "```diff" + "\n"  + "- "  + lv_resultsOfNight+ "\n" + "```";
            lv_resultsRoles = getTextForRoleReveal(ma_chosenDeath[1]);
            ma_chosenDeath[1].nameOfRole = "dead";
        }

        for (PlayerRole lr_player : mr_base.ma_playerList) {
            PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
            lr_privateChannel.sendMessage(lv_resultsOfNight).queue();
            if ((ma_chosenDeath[0] != null) && (ma_chosenDeath[1] != null)) {
                lr_privateChannel.sendMessage(lv_resultsRoles).queueAfter(3, TimeUnit.SECONDS);
            }
        }

       if(winCondition()) {
           return;
       }

        String lv_livingPlayersToChoose = mr_base.getLivingPlayerNames("citizen");
        lv_livingPlayersToChoose = "`" + lv_livingPlayersToChoose + "`";
        for (PlayerRole lr_player : mr_base.ma_playerList) {
                PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                lr_privateChannel.sendMessage(Main.getParameter("translation.csv", "Hungry wolves are still in our series")).queueAfter(6, TimeUnit.SECONDS);
                lr_privateChannel.sendMessage(Main.getParameter("translation.csv", "It's time for all citizens to make a decision")).queueAfter(7, TimeUnit.SECONDS);
                lr_privateChannel.sendMessage(lv_livingPlayersToChoose).queueAfter(10, TimeUnit.SECONDS);
        }

        ma_chosenDeath[0] = null;
        ma_chosenDeath[1] = null;
        mr_base.mv_gameState = 4;
        //Preparation for gamestate4
        int lv_numberOfPlayers = mr_base.getNumberOfLivingWerewolves() + mr_base.getNumberOfLivingPeople();
        ma_chosenHanged = new PlayerRole[lv_numberOfPlayers][2];

    }
    //Players can vote for player to be hanged
    public void gamestate4(PrivateMessageReceivedEvent ir_event) {
        String lv_messageContent = ir_event.getMessage().getContentRaw();

        int lv_numberOfLivingPlayers = ma_chosenHanged.length;

        if (isPlayerRole(ir_event.getAuthor(),"dead")) {
            return;
        }
        
        if ((!isPlayerLiving(lv_messageContent)) && (!lv_messageContent.equalsIgnoreCase("NO"))) {
            String lv_playerNotExisting = Main.getParameter("translation.csv","Player [NAME] does not exist");
            lv_playerNotExisting = lv_playerNotExisting.replace("[NAME]",lv_messageContent);
            ir_event.getAuthor()
                    .openPrivateChannel()
                    .complete()
                    .sendMessage(lv_playerNotExisting)
                    .queue();
            return;
        }

        String lv_forElimination = "";

        for (int lv_loop = 0; lv_loop < lv_numberOfLivingPlayers; lv_loop++) {
            if ((ma_chosenHanged[lv_loop][1] == null) || (ma_chosenHanged[lv_loop][1].mr_user.equals(ir_event.getAuthor()))) {

                if (lv_messageContent.equalsIgnoreCase("NO")) {
                    ma_chosenHanged[lv_loop][0] = null;
                    ma_chosenHanged[lv_loop][1] = getPlayerRoleFromName(ir_event.getAuthor().getName());

                    lv_forElimination = Main.getParameter("translation.csv", "[PLAYER] has chosen noone");
                    lv_forElimination = lv_forElimination.replace("[PLAYER]", ir_event.getAuthor().getName());
                    lv_forElimination = "```apache" + "\n"    + lv_forElimination + "\n" + "```";
                } else {
                    ma_chosenHanged[lv_loop][0] = getPlayerRoleFromName(lv_messageContent);
                    ma_chosenHanged[lv_loop][1] = getPlayerRoleFromName(ir_event.getAuthor().getName());
                    lv_forElimination = Main.getParameter("translation.csv", "[PLAYER1] has voted for [PLAYER2] for elimination");
                    lv_forElimination = lv_forElimination.replace("[PLAYER1]", ir_event.getAuthor().getName()).replace("[PLAYER2]", lv_messageContent);
                    lv_forElimination = "```apache" + "\n"    + lv_forElimination + "\n" + "```";
                }
                break;
            }
        }
            //Send message to notify everyone what choice the player made
         for (PlayerRole lr_playerRole : mr_base.ma_playerList) {
                 PrivateChannel lr_privateChannel = lr_playerRole.mr_user.openPrivateChannel().complete();
                 lr_privateChannel.sendMessage(lv_forElimination).queue();
         }

         //If last entry of array is empty, there are players left to vote
         if (ma_chosenHanged[lv_numberOfLivingPlayers - 1][1] == null) {
             return;
         }

         //Choose player out of array
         PlayerRole lr_chosenPlayer = null;
         PlayerRole lr_playerLoop;
         int lv_loop = lv_numberOfLivingPlayers;
         int lv_count = 0;
         int lv_rememberCount = 0;
         boolean lv_redundant = false;

         while (lv_loop != 0) {
             lr_playerLoop = ma_chosenHanged[lv_loop-1][0];
             for (PlayerRole[] lr_personHanged : ma_chosenHanged) {
                if (lr_personHanged[0] == lr_playerLoop) {
                    lv_count++;
                }
             }
             if ((lv_count == lv_rememberCount) && (lr_playerLoop != lr_chosenPlayer)) {
                lv_redundant = true;
             } else if (lv_count > lv_rememberCount) {
                 lr_chosenPlayer = lr_playerLoop;
                 lv_redundant = false;
                 lv_rememberCount = lv_count;
             }
             lv_count = 0;
             lv_loop--;
         }
         //Check if two options have the same amount of votes -> Repeat gamestate4 ONCE every round (mv_secondVote)
         if (lv_redundant){
             if (mv_secondVote) {
                lr_chosenPlayer = null;
                mv_secondVote = false;
             } else if (mr_base.getNumberOfLivingPeople() + mr_base.getNumberOfLivingWerewolves() != 2) {
                 String lv_noConclusion = Main.getParameter("translation.csv", "No player could be clearly chosen");
                 String lv_voteAgain = Main.getParameter("translation.csv", "Everyone can vote once again");
                 for (PlayerRole lr_player : mr_base.ma_playerList) {
                     PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                     lr_privateChannel.sendMessage(lv_noConclusion).queue();
                     lr_privateChannel.sendMessage(lv_voteAgain).queueAfter(2, TimeUnit.SECONDS);
                 }
                 ma_chosenHanged = new PlayerRole[ma_chosenHanged.length][2];
                 mv_secondVote = true;
                 return;
             }
         }

        //Message to players
         String lv_conclusion;
         String lv_conclusionPlayer;
         if (lr_chosenPlayer == null) {
             if (lv_redundant) {
                 lv_conclusion = Main.getParameter("translation.csv", "The citizens could not come to a conclusion");
             } else {
                 lv_conclusion = Main.getParameter("translation.csv","The citizens did come to a conclusion");
             }
             lv_conclusionPlayer = Main.getParameter("translation.csv", "Noone will be hanged");
         } else {
             lv_conclusion = Main.getParameter("translation.csv","The citizens did come to a conclusion");
             lv_conclusionPlayer = Main.getParameter("translation.csv","Player [PLAYER] will be hanged");
             lv_conclusionPlayer = lv_conclusionPlayer.replace("[PLAYER]",lr_chosenPlayer.mr_user.getName());
             lv_conclusionPlayer = "```diff" + "\n"  + "- "  + lv_conclusionPlayer + "\n" + "```";
         }

        for (PlayerRole lr_player : mr_base.ma_playerList) {
            PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
            lr_privateChannel.sendMessage(lv_conclusion).queueAfter(3,TimeUnit.SECONDS);
            lr_privateChannel.sendMessage(lv_conclusionPlayer).queueAfter(4,TimeUnit.SECONDS);
        }

        //Only when a player was hanged, reveal his role
        if (lr_chosenPlayer != null) {
            String lv_roleReveal;
            if (lr_chosenPlayer.nameOfRole.equals("werewolf")) {
                lv_roleReveal = Main.getParameter("translation.csv", "[PLAYER] died as a werewolf");
            } else {
                lv_roleReveal = Main.getParameter("translation.csv", "[PLAYER] died as a citizen");
            }
            lv_roleReveal  = lv_roleReveal.replace("[PLAYER]",lr_chosenPlayer.mr_user.getName());
            lr_chosenPlayer.nameOfRole = "dead";

            for (PlayerRole lr_player : mr_base.ma_playerList) {
                PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                lr_privateChannel.sendMessage(lv_roleReveal).queueAfter(6,TimeUnit.SECONDS);
            }
        }

        if(winCondition()) {
            return;
        }


        //Night time and werewolves can choose victim again
        String lv_goHome = Main.getParameter("translation.csv","The citizens are getting tired and go home");
        String lv_nightTime = Main.getParameter("translation.csv","The night comes and all citizens go to sleep");
        String lv_nextVictim;
        String lv_chosenext;

        if (mr_base.getNumberOfLivingWerewolves() != 1) {
            lv_nextVictim = Main.getParameter("translation.csv", "The werewolves pick their next victim...");
            lv_chosenext = Main.getParameter("translation.csv","Choose the next victim with your brothers");
        } else {
            lv_nextVictim = Main.getParameter("translation.csv","The werewolf picks his next victim...");
            lv_chosenext = Main.getParameter("translation.csv","Pick your next victim");
        }
        lv_chosenext = "`" + lv_chosenext + "`";

        String lv_livingPlayerNames = mr_base.getLivingPlayerNames("werewolf");
        lv_livingPlayerNames = "`" + lv_livingPlayerNames + "`";

        for (PlayerRole lr_player : mr_base.ma_playerList) {
            PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
            lr_privateChannel.sendMessage(lv_goHome).queueAfter(7,TimeUnit.SECONDS);
            lr_privateChannel.sendMessage(lv_nextVictim).queueAfter(8,TimeUnit.SECONDS);

            if (lr_player.nameOfRole.equals("werewolf")) {
                lr_privateChannel.sendMessage(lv_chosenext).queueAfter(9, TimeUnit.SECONDS);
                lr_privateChannel.sendMessage(lv_livingPlayerNames).queueAfter(10,TimeUnit.SECONDS);
            }
        }

        mr_base.mv_gameState = 1;
        }



        public boolean isPlayerRole (User ir_userToCheck, String iv_roleToCheck){
            boolean isRole = false;
            for (PlayerRole lr_player : mr_base.ma_playerList) {
                if ((lr_player.mr_user == ir_userToCheck) && (lr_player.nameOfRole.equals(iv_roleToCheck))) {
                    isRole = true;
                }
            }
            return isRole;
        }
        public boolean isPlayerLiving (String iv_nameToCheck){
            boolean lv_exists = false;
            for (PlayerRole lr_player : mr_base.ma_playerList) {
                if (lr_player.mr_user.getName().equalsIgnoreCase(iv_nameToCheck) && (lr_player.nameOfRole != "dead")) {
                    lv_exists = true;
                }
            }
            return lv_exists;
        }
        public PlayerRole getPlayerRoleFromName (String ir_accountName){
            PlayerRole lr_rightPlayer = null;
            for (PlayerRole lr_player : mr_base.ma_playerList) {
                if (lr_player.mr_user.getName().equalsIgnoreCase(ir_accountName)) {
                    lr_rightPlayer = lr_player;
                }
            }
            return lr_rightPlayer;
        }
        public String getTextForRoleReveal (@NotNull PlayerRole ir_player){
            String lv_output = null;
            if (ir_player.nameOfRole.equals("werewolf")) {
                lv_output = Main.getParameter("translation.csv", "[PLAYER] died as a werewolf=");
                lv_output = lv_output.replace("[PLAYER]", ir_player.mr_user.getName());
            } else {
                lv_output = Main.getParameter("translation.csv", "[PLAYER] died as a citizen=");
                lv_output = lv_output.replace("[PLAYER]", ir_player.mr_user.getName());
            }
            return lv_output;
        }
        public boolean winCondition () {
            boolean lv_win = false;

            String lv_noLeft = "";
            String lv_haveWon = "";
            if ((mr_base.getNumberOfLivingWerewolves() == 0) && (mr_base.getNumberOfLivingPeople() == 0)) {
                lv_win = true;
                lv_noLeft = Main.getParameter("translation.csv","There is noone left on both sides");
                lv_haveWon = Main.getParameter("translation.csv","There is no winner");
            } else if (mr_base.getNumberOfLivingWerewolves() == 0) {
                lv_win = true;
                lv_noLeft = Main.getParameter("translation.csv","There is no werewolf left!");
                lv_haveWon = Main.getParameter("translation.csv","The citizens have won!");
            } else if (mr_base.getNumberOfLivingPeople() == 0) {
                lv_win = true;
                lv_noLeft = Main.getParameter("translation.csv","There is no citizen left!");
                lv_haveWon = Main.getParameter("translation.csv","The werewolves win!");
            }

            lv_haveWon = "```json" + "\n" + "\"" + lv_haveWon + "\"" + "\n" + "```";

            if (lv_win) {

                for (PlayerRole lr_player : mr_base.ma_playerList) {
                    PrivateChannel lr_privateChannel = lr_player.mr_user.openPrivateChannel().complete();
                    lr_privateChannel.sendMessage(lv_noLeft).queueAfter(9,TimeUnit.SECONDS);
                    lr_privateChannel.sendMessage(lv_haveWon).queueAfter(13,TimeUnit.SECONDS);
                }
                mr_base.mv_gameState = 0;
                mr_base.ma_playerList.clear();
            }
            return lv_win;

        }

    }
