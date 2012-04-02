package notifiers;

import play.*;
import play.mvc.*;
import java.util.*;
import models.*;
 
public class Mails extends Mailer {
   
   public static void registration(String newUserEmail, String token) {
      setSubject("scroll2lol registration");
      addRecipient(newUserEmail);
      setFrom("scroll2lol <scroll2lol@gmail.com>");
      send(token);
   }
}