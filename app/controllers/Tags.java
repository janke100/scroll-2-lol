package controllers;

import models.User;
import play.*;
import play.mvc.*;
 
@With(Secure.class)
public class Tags extends CRUD {    
    
	@Before
    static void setConnectedUser() {
        if(Security.isConnected()) {
            User user = User.find("byEmail", Security.connected()).first();
            renderArgs.put("user", user);
        }
    }
}
