import net.dv8tion.jda.api.entities.User;

public class PlayerRoles {
        User mr_user;
        String nameOfRole;

        PlayerRoles (User ir_user) {
            mr_user = ir_user;
        }

        String getNameOfRole () {
            //roles get translated!
            return Main.getParameter("translation.csv",nameOfRole);
        }
}
