package controllers.pub;

import play.mvc.*;

import utilities.AuthManager;
import utilities.Config;
import views.html.*;

public class Application extends Controller {

    public Result index() {
        return ok(index.render(Config.ServerName, AuthManager.isLoggedIn(request().cookies())));
    }

}
