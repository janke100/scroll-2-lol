package models;

import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;

@Entity
public class Comment extends Model {
 
	@Required(message = "Author is required!")
	@MaxSize(50)
    public String author;
	
	@Required(message = "Date is required!")
    public Date postedAt;
     
    @Lob
    @Required(message = "Comment is required!")
    @MaxSize(value = 10000, message = "Comment is too long")
    public String content;
    
    @ManyToOne
    @Required(message = "Post is required!")
    public Post post;
    
    public Comment(Post post, String author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.postedAt = new Date();
    }
    
    public String toString() {
        return content;
    }
}