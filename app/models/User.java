package models;

import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
 
@Entity
public class User extends Model {
 
	@Required(message = "Email is required!")
	@Email
	@Unique(message = "Email is already in use!")
    public String email;
	
	@Required(message = "Password is required!")
    @Password
    public String password;
	
	@Required(message = "Username is required!")
	@MaxSize(value = 50, message = "Username is too long!")
	@Unique(message = "Username is already in use!")
    public String username;
	
    public boolean isAdmin;
	
	@ManyToMany //(cascade=CascadeType.PERSIST)
	public List<Post> liked;
    
    public User(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
		this.isAdmin = false;
		this.liked = new ArrayList<Post>();
    }
	
	@Override
	public void _delete() {
		List<Post> posts = Post.find("author.id = ?", this.id).fetch();
		for (Post post : posts) {
			post.author = null;
			post.save();
		}
		super._delete();
	}
    
    public static User connect(String email, String password) {
        return find("byEmailAndPassword", email, password).first();
    }
    
    public String toString() {
        return email;
    }
}