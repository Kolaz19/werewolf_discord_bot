import net.dv8tion.jda.api.entities.User;

public class PlayerRole {
        User mr_user;
        String nameOfRole;

        PlayerRole(User ir_user) {
            mr_user = ir_user;
        }

        String getNameOfRole () {
            //roles get translated!
            return Main.getParameter("translation.csv",nameOfRole);
        }
}
