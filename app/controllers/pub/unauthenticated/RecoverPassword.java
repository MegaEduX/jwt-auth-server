package controllers.pub.unauthenticated;

import models.UserData;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import utilities.Config;
import utilities.KeyGenerator;
import utilities.Mailer;
import views.html.generic_failure;
import views.html.generic_success;
import views.html.password_recovery;

/**
 * Created by MegaEduX on 13/11/15.
 */

public class RecoverPassword extends Controller {

    public Result forgotPage() {
        return ok(password_recovery.render(Config.ServerName));
    }

    public Result handleForgot() {
        DynamicForm form = Form.form().bindFromRequest();

        String user = form.get("username").toLowerCase();
        String email = form.get("email");

        UserData u = UserData.getUserDataFromUsername(user);

        if (u == null)
            return notFound(generic_failure.render(Config.ServerName, false, Messages.get("login.userNotFound")));

        if (!u.emailAddress.equals(email))
            return notFound(generic_failure.render(Config.ServerName, false, Messages.get("recover.incorrectEmail")));

        KeyGenerator g = new KeyGenerator();

        String newPassword = g.nextKey();

        try {
            u.changePassword(newPassword);
        } catch (Exception e) {
            return internalServerError(generic_failure.render(Config.ServerName, false, e.getMessage()));
        }

        u.save();

        Mailer m = new Mailer(Config.ServerName);

        if (m.sendEmailPasswordChanged(u.username, u.emailAddress, newPassword)) {
            return ok(generic_success.render(Config.ServerName, false, Messages.get("recover.success")));
        } else {
            return internalServerError(generic_success.render(Config.ServerName, false, Messages.get("recover.failure")));
        }
    }

}
