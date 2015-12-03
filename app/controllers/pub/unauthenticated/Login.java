package controllers.pub.unauthenticated;

import com.avaje.ebean.Ebean;
import models.Password;
import models.UserAttribute;
import models.UserData;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import utilities.AuthManager;
import utilities.Config;
import utilities.JWTFactory;
import utilities.LoginCooldown;
import views.html.forbidden;
import views.html.login;
import views.html.login_failure;
import views.html.login_success;

import java.util.List;

/**
 * Created by MegaEduX on 23/10/15.
 */

public class Login extends Controller {

    public Result loginPage() {
        DynamicForm form = Form.form().bindFromRequest();

        String callback = form.get("callback");

        if (AuthManager.isLoggedIn(request().cookies()))
            return forbidden(forbidden.render(Config.ServerName, true));

        return ok(login.render(Config.ServerName, (callback != null ? callback : "")));
    }

    public Result handlePerformLogin() {
        UserData u = null;

        DynamicForm form = Form.form().bindFromRequest();

        String user = form.get("username").toLowerCase();
        String pass = form.get("password");
        String callback = form.get("callback");

        String remStr = form.get("remember");

        boolean remember = false;

        if (remStr != null && remStr.equals("true"))
            remember = true;

        if (AuthManager.isLoggedIn(request().cookies())) {
            String username = AuthManager.currentUsername(request().cookies());

            u = UserData.getUserDataFromUsername(username);

            if (callback != null && !callback.equals("")) {
                try {
                    return ok(login_success.render(Config.ServerName, callback + "?jwt=" + JWTFactory.createAuthenticationJWT(u, request().remoteAddress(), Config.ServerName, "auth", remember)));
                } catch (Exception e) {
                    return internalServerError(e.getMessage());
                }
            } else
                return ok(login_success.render(Config.ServerName, ""));
        } else {
            if (user == null || user == "")
                return ok(login_failure.render(Config.ServerName, "Missing field: \"username\"."));

            if (pass == null || pass == "")
                return ok(login_failure.render(Config.ServerName, "Missing field: \"password\"."));

            List<UserData> users = Ebean.find(UserData.class).where().eq("username", user).findList();

            if (users.size() == 0)
                return ok(login_failure.render(Config.ServerName, "User not found: " + user));

            int cooldown = LoginCooldown.getInstance().getCooldownForUsername(user);

            if (cooldown != 0)
                return ok(login_failure.render(Config.ServerName, "You must wait " + cooldown + " seconds before attempting to login again."));

            u = users.get(0);

            for (UserAttribute a : u.attributes)
                if (a.key.equals("validation-key"))
                    return forbidden(login_failure.render(Config.ServerName, "Account not validated!"));

            if (!u.enabled)
                return ok(login_failure.render(Config.ServerName, "Resource disabled."));

            try {
                Password pi = new Password(u.passwordDigest, u.passwordSalt);

                if (pi.validate(pass)) {
                    LoginCooldown.getInstance().removeCooldown(user);

                    if (callback != null && !callback.equals(""))
                        return ok(login_success.render(Config.ServerName, callback + "?jwt=" + JWTFactory.createAuthenticationJWT(u, request().remoteAddress(), Config.ServerName, "auth", remember)));
                    else {
                        response().setCookie(
                                "jwt",
                                JWTFactory.createAuthenticationJWT(u, request().remoteAddress(), Config.ServerName, "auth", remember),
                                (remember ? 1209600 : 3600),
                                "/",
                                Config.getServerURI(request()),
                                true,
                                true
                        );

                        return ok(login_success.render(Config.ServerName, ""));
                    }
                } else {
                    LoginCooldown.getInstance().addFailedTryForUsername(user);

                    return ok(login_failure.render(Config.ServerName, "Incorrect username or password!"));
                }
            } catch (Exception e) {
                return internalServerError(e.getMessage());
            }
        }
    }

}
