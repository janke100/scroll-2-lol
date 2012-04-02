package models;

import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;

@Entity
public class Post extends Model {
 
	@Required(message = "Title is required!")
	@MaxSize(value=150, message="Title is too long!")
    public String title;
	
	@Required(message = "Date is required!")
    public Date postedAt;
    
	@Required(message = "Image is required!")
    public Blob image;
    
//	@Required(message = "Author is required!")
    @ManyToOne
    public User author;
    
    @OneToMany(mappedBy="post", cascade=CascadeType.ALL)
    public List<Comment> comments;
    
    @ManyToMany(cascade=CascadeType.PERSIST)
    public Set<Tag> tags;
    
    public int likes;
	
	@URL(message = "Invalid source URL!")
	public String source;

    public Post(User author, String title, Blob image, String source) {
    	this.comments = new ArrayList<Comment>();
    	this.tags = new TreeSet<Tag>();
        this.author = author;
        this.title = title;
        this.image = image;
		this.source = source;
        this.postedAt = new Date();
        this.likes = 0;
    }
	
	@Override
	public void _delete() {
		List<User> users= User.find("select u from User u join u.liked as l where l.id = ?", this.id ).fetch();
		for (User user : users) {
			user.liked.remove(this);
			user.save();
			}
		this.image.getFile().delete();
		super._delete();
	}
    
    public Post addComment(String author, String content) {
        Comment newComment = new Comment(this, author, content).save();
        this.comments.add(newComment);
        this.save();
        return this;
    }
    
    public Post next() {
        return Post.find("postedAt < ? order by postedAt desc", postedAt).first();
    }
     
    public Post previous() {
        return Post.find("postedAt > ? order by postedAt asc", postedAt).first();
    }
    
    public String toString() {
        return title;
    }
    
    public Post tagItWith(String name) {
        tags.add(Tag.findOrCreateByName(name));
        return this;
    }
    
    public static List<Post> findTaggedWith(String tagName) {
        return Post.find(
            "select distinct p from Post p join p.tags as t where t.name = ?", tagName
        ).fetch();
    }
    
    public static List<Post> findTaggedWith(long tagId) {
        return Post.find(
            "select distinct p from Post p join p.tags as t where t.id = ?", tagId
        ).fetch();
    }
    
    public static List<Post> findTitleOrTag(String s) {
    	String str = s.toLowerCase();
    	return Post.find(
    		"select distinct p from Post p left outer join p.tags as t where LOWER(p.title) like ? or LOWER(t.name) like ?", "%"+str+"%", "%"+str+"%"
    	).fetch();
    }

}