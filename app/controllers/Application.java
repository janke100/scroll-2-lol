package controllers;

import java.util.*;
 
import play.*;
import play.mvc.*;
import play.data.validation.*;
import play.db.jpa.Blob;
import play.libs.*;
import play.cache.*;
 
import models.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.io.*;
import java.net.URLConnection;

public class Application extends Controller {
	
	@Before
	static void addDefaults() {
	    loadCookieIfPresent();
		if(Security.isConnected()) {
            User user = User.find("byEmail", Security.connected()).first();
            renderArgs.put("user", user);
        }
	}
	
	private static void loadCookieIfPresent() {
	    Http.Cookie remember = request.cookies.get("rememberme");
	    if (remember != null && remember.value.indexOf("-") > 0) {
	        String sign = remember.value.substring(0, remember.value.indexOf("-"));
	        String username = remember.value.substring(remember.value.indexOf("-") + 1);
	        if (Crypto.sign(username).equals(sign)) {
	            session.put("username", username);
	        }
	    }
	}

	
	public static void index() {
		boolean nextButton = false;
		List<Post> posts = Post.find("order by postedAt desc").from(0).fetch(5);
		if(Post.count() > 5)
			nextButton = true;
		int nextPage = 1;
        render(posts, nextPage, nextButton);
	}

    public static void page(int page) {
		boolean nextButton = false;
    	List<Post> posts = Post.find("order by postedAt desc").from(page*5).fetch(5);
		if(Post.count() > page*5 + 5)
			nextButton = true;
		int nextPage = page + 1;
		int previousPage = page - 1;
        render("@index", posts, previousPage, nextPage, nextButton);
    }
	
    public static void hot(int page) {
		boolean nextButton = false;
    	List<Post> posts = Post.find("likes > 1 order by postedAt desc").from(page*5).fetch(5);
		if(Post.count("likes > 1") > page*5 + 5)
			nextButton = true;
		int nextPage = page + 1;
		int previousPage = page - 1;
        render(posts, previousPage, nextPage, nextButton);
    }

    public static void top(String category, int page) {
		boolean nextButton = false;
		List<Post> posts = new ArrayList();
		int timeOffset = 0;
		if(category == null)
			category = "all-time";
		
		if(category.equals("all-time")) {
			posts = Post.find("order by likes desc").from(page*5).fetch(5);
			category = "All Time";
			if(Post.count() > page*5 + 5)
				nextButton = true;
		}
		else {
			if(category.equals("this-month")) {
				timeOffset = -30*24;
				category = "This Month";
			}
			else if(category.equals("this-week")) {
				timeOffset = -7*24;
				category = "This Week";
			}
			else if(category.equals("today")) {
				timeOffset = -24;
				category = "Today";
			}
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.HOUR, timeOffset);
			Date startingDate = calendar.getTime();
			posts = Post.find("postedAt > ? order by likes desc", startingDate).from(page*5).fetch(5);
			if(Post.count("postedAt > ?", startingDate) > page*5 + 5)
				nextButton = true;
		}
		int nextPage = page + 1;
		int previousPage = page - 1;
        render(posts, previousPage, nextPage, category, nextButton);
    }
    
	public static void random(){
		int postCount = (int) Post.count();
		Random random = new Random();
		int postIndex = random.nextInt(postCount);
		Post post = Post.all().from(postIndex).first();
//		Post post = Post.findById(x);
//		if(post==null)
//			random();
    	render("@show", post);
	}
    
    public static void show(long id) {
    	Post post = Post.findById(id);
    	notFoundIfNull(post);
    	render(post);
    }
    
    public static void deletePost(long id){
    	Post post = Post.findById(id);
    	notFoundIfNull(post);
    	if(post.author.email.equals(Security.connected())) {
    		post.image.getFile().delete();
        	post.delete();
        	flash.success("Post deleted!");
        	index();
    	}
    	index();
    }
    
    public static void like(long id){
    	Post post = Post.findById(id);
		User user = User.find("byEmail", Security.connected()).first();
    	post.likes++;
		post.save();
		user.liked.add(post);
		user.save();
		renderJSON(post.likes);
    }
    
	public static void unlike(long id){
    	Post post = Post.findById(id);
		User user = User.find("byEmail", Security.connected()).first();
    	post.likes--;
		post.save();
		user.liked.remove(post);
		user.save();
		renderJSON(post.likes);
    }
	
    public static void form(boolean isFile) throws Throwable {			// upload form
	 	if(Security.isConnected()) {
	 		render(isFile);
	 	}
 		flash.put("url", "GET".equals(request.method) ? request.url : "/");
	  	Secure.login();										// if not used delete "throws Throwable"
    }
   
	public static void save(Blob imageFile, String url, String title, boolean isFile, String tags, String source)
	{
		User author = User.find("byEmail", Security.connected()).first();
		Blob image = new Blob();
		String trimSource = source.trim();
		Post post = new Post(author, title, null, trimSource);
		
		for(String tag : tags.split("\\,")) {
			if(tag.trim().length() > 0) {
				post.tags.add(Tag.findOrCreateByName(tag.trim()));
			}
		}
		
		if(isFile)
			image = imageFile;
		else {
			String trimUrl = url.trim();
			validation.required(trimUrl).key("invalid-url").message("URL is required!");
			validation.url(trimUrl).key("invalid-url").message("Invalid URL!");
			if(!validation.hasError("invalid-url")) {
				try {
					URL imageUrl = new URL(trimUrl);
					URLConnection urlcon = imageUrl.openConnection();
					InputStream is = urlcon.getInputStream();
					image.set(is, urlcon.getContentType());	
				} catch (IOException e) {
					validation.addError("exception", "Something went wrong. Please try again.");
					render("@form", post, isFile);
				}
			} 
		}
		
		if(image!=null && image.exists()) {
			validation.isTrue(image.type().equals("image/jpeg") || image.type().equals("image/png") || image.type().equals("image/gif")).key("type").message("Invalid image format!");
			validation.isTrue(image.length() < 2097152).key("size").message("Image is too big! (limit: 2MB)");
		}		
		
		post.image = image;
		validation.valid(post);
		if(validation.hasErrors())
			render("@form", post, isFile);
		
		try {			// doesn't work for animated gifs (animation is lost after resize)
			BufferedImage bi = ImageIO.read(image.getFile());
			if(bi.getWidth() > 500) {
				if(image.type().equals("image/gif")) {
					validation.addError("exception", "Gif images wider than 500px are not acceptable.");
					render("@form", post, isFile);
				}
				Blob resizedImage = new Blob();
				String imgType = image.type().substring(6);
				File resizedFile = new File("tmp_file." + imgType);
				Images.resize(image.getFile(), resizedFile, 500, 30000, true);
				resizedImage.set(new FileInputStream(resizedFile), image.type());
				resizedFile.delete();
				post.image = resizedImage;
			}
		} catch (IOException e) {
			validation.addError("exception", "Something went wrong. Please try again.");
			render("@form", post, isFile);
		}	
		
		post.save();
		flash.success("Thanks for posting %s!", author.username);
		show(post.id);
	}

    public static void postComment(long postId,  
            @Required(message="A message is required!") @MaxSize(value=10000, message="Message is too long!") String content)
    {
        Post post = Post.findById(postId);
        if (validation.hasErrors()) {
            render("Application/show.html", post);
        }
        User author = User.find("byEmail", Security.connected()).first();
        post.addComment(author.username, content);
        flash.success("Thanks for commenting %s!", author.username);
        show(post.id);
    }
    
    public static void captcha(String randomID) {
        Images.Captcha captcha = Images.captcha();
        String code = captcha.getText("#E4EAFD");
        Cache.set(randomID, code, "10mn");
        renderBinary(captcha);
    }
    
    public static void showPic(long id) {
        Post post = Post.findById(id);
        notFoundIfNull(post);
        response.setContentTypeIfNotSet(post.image.type());
        renderBinary(post.image.get()); 
    }
    
    public static void regform(){
    	if (Security.isConnected())
    		index();
    	String randomID = Codec.UUID();
    	render(randomID);
    }
    
    public static void addUser(String email, String password, String retypepass, String username, 
    		@Required(message="Please enter the code.") String code, String randomID)
    {
    	validation.equals(password, retypepass).key("mismatch").message("Passwords don't match!");
    	validation.equals(
                code, Cache.get(randomID)
            ).key("captcha").message("Invalid code! Please enter it again.");
    	User newUser = new User(email, password, username);
    	validation.valid(newUser);
    	if(validation.hasErrors()) {
            render("@regform", newUser, randomID);
        }
    	Cache.delete(randomID);
    	newUser.save();
    	flash.success("Oh yeah! You're signed up now, %s! Post something funny!", newUser.username);
		session.put("username", email);
		//response.setCookie("rememberme", Crypto.sign(email) + "-" + email, "30d");	//to set rememberme cookie
    	index();
    }
    
    public static void listTagged(long tagId, String tagNameSlugified, int page) {
        boolean nextButton = false;
		List<Post> posts = Post.find(
			"select distinct p from Post p join p.tags as t where t.id = ? order by p.likes desc", tagId
		).from(page*5).fetch(5);
		long taggedCount = Post.count("select count(p.id) from Post p join p.tags as t where t.id = ?", tagId);
		if(taggedCount > page*5 + 5)
			nextButton = true;
		int nextPage = page + 1;
		int previousPage = page - 1;
        Tag tag = Tag.findById(tagId);
        render(tag, taggedCount, posts, previousPage, nextPage, nextButton);
    }
    
    public static void profile(String username, int page) {
    	User profile = User.find("byUsername", username).first();
    	notFoundIfNull(profile);
		boolean nextButton = false;
    	List<Post> posts = Post.find("author.username = ? order by postedAt desc", username).from(page*5).fetch(5);
		if(Post.count("author.username = ?", username) > page*5 + 5)
			nextButton = true;
		int nextPage = page + 1;
		int previousPage = page - 1;
    	render(profile, posts, previousPage, nextPage, nextButton);
    }
	
	public static void likes(String username, int page) {
    	User profile = User.find("byUsername", username).first();
    	notFoundIfNull(profile);
		boolean nextButton = false;
		List<Post> posts = Post.find(
			"select distinct p from User u join u.liked as p where u.username = ? order by p.postedAt desc", username
		).from(page*5).fetch(5);
		if(profile.liked.size() > page*5 + 5)
			nextButton = true;
		int nextPage = page + 1;
		int previousPage = page - 1;
    	render(profile, posts, previousPage, nextPage, nextButton);
    }
    
    public static void settings() throws Throwable {
    	if(Security.isConnected())
    		render();
    	flash.put("url", "GET".equals(request.method) ? request.url : "/");
	  	Secure.login();										// if not used delete "throws Throwable"
    }
    
    public static void saveUser(String password, String newpass, String retypenew, String username) {
    	User profile = User.find("byEmail", Security.connected()).first();
    	validation.equals(profile.password, password).key("password").message("Wrong password!");  	
		if(!username.equals(""))
			profile.username = username;
		if(!newpass.equals("") || !retypenew.equals("")){
			profile.password = newpass;
			validation.equals(newpass, retypenew).key("mismatch").message("Passwords don't match!");
		}
    	validation.valid(profile);
    	if(validation.hasErrors()){	
			profile.refresh();
    		render("@settings", username);
    	}
    	profile.save();
    	flash.success("Changes saved.");
    	profile(profile.username, 0);
    }
	
	public static void results(String query, int page) {
		String[] queryarray = query.trim().split("\\s+");
		List<Post> allPosts = new ArrayList();
		List<Post> subPosts = new ArrayList();
		List<Post> posts = new ArrayList();
		boolean nextButton = false;
		for(int i=0; i<queryarray.length; i++) {
			subPosts = Post.findTitleOrTag(queryarray[i]);
			int size = subPosts.size();
			for(int j=0; j<size; j++) {
				if(!allPosts.contains(subPosts.get(j)))
					allPosts.add(subPosts.get(j));
			}
		}
		int allPostsSize = allPosts.size();
		if(allPostsSize > page*5) {
			if(allPostsSize > page*5 + 5) {
				posts = allPosts.subList(page*5, page*5 + 5);
				nextButton = true;
			}	
			else
				posts = allPosts.subList(page*5, allPostsSize);
		}
		int nextPage = page + 1;
		int previousPage = page - 1;
		render(query, allPostsSize, posts, previousPage, nextPage, nextButton);
	}
	
	public static void getPopularTags() {
		List<Map> cloud = Tag.getCloud();
		render(cloud);
	}
}
