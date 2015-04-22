package models;
import java.util.ArrayList;
import java.util.TreeMap;
import models.User;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.*;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

//Database imports
import play.db.*;
import javax.sql.DataSource;
import java.sql.*;

public class AllUsers{
    
    /*TreeMap<Username, Userdata>*/
    TreeMap<String, User> allusers = new TreeMap<String, User>();
    /*ArrayList<MovieTitles> index would be movieID*/
    ArrayList<String> allmovies = new ArrayList<String>();
    /*ArrayList<genres> index would be movieID*/
    ArrayList<String> allgenres = new ArrayList<String>();
    
    Connection connection = DB.getConnection("default");

    
    public ArrayList<String> shortlist = new ArrayList<String>();
    
    public int getSizeOfAll(){
        return allusers.size();
    }
    
    
    public void movieParse(File moviefile) throws IOException{
		String line;
		BufferedReader input = null;

		try {
			input = new BufferedReader(new FileReader(moviefile));

			while((line = input.readLine()) != null) {
				String[] wordArray = line.split("[|]+");
				allmovies.add(wordArray[1]);
			}
			
		} catch(FileNotFoundException e) {
			System.out.println("File Not Found");

		} catch (IOException e) {
			System.out.println("File is not readable");
			
		} finally {
			input.close();
			
		}
	}
	
	public void userParse(File userfile) throws IOException{
	    String line;
		BufferedReader input = null;

		try {
			input = new BufferedReader(new FileReader(userfile));

			while((line = input.readLine()) != null) {
				String[] wordArray = line.split("[\t]+");
				String user = wordArray[0];
				User newuserdata = new User();
				addToAll(user, newuserdata);
				newuserdata.setUserName(user);
				
				for(int i = 1; i < wordArray.length; i++){
						String[] rating = wordArray[i].split("[,]");
						allusers.get(user).userdata.put(Integer.parseInt(rating[0]), Integer.parseInt(rating[1]));
					}
			}

		} catch(FileNotFoundException e) {
			System.out.println("File Not Found");

		} catch (IOException e) {
			System.out.println("File is not readable");
			
		}finally {
			input.close();
			
		}
	}
    

    /**
     * SQL OPERATIONS
     * 
     * loginInsert(username, password):
     *      Insert into Table with all user login information. Username and Hashed Password.
     * 
     * loginCheck(username, password):
     *      Returns true if username is in database along with a password that matches.
     * 
     * loginPrint():
     *      Testing purposes.
     *
     * tableCreate(username):
     *      Create table for individual user that holds movie id and rating associated with it.
     * 
     * tableInsert (username, movieid, rating):
     *      Inserts movie id and associated rating into table associated with user. 
     * 
     * tableGetMap(username)
     *      Returns treemap of movie ids and ratings for given username.
     * 
     * tablePrint (username):
     *      Testing purposes.
     * 
     * */
    public boolean loginInsert(String username, String password){
        
        try{
            Connection conn = DB.getConnection("default");
            if(loginCheck(username, password)){
                return false;
            }
            String query = " insert into loginInfo (username, password)"
        + " values (?, PASSWORD(?))";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query); 
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.execute();
            
            conn.close();
        }   catch (SQLException ex){
                System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
        }
        return true;
        
    }
    
    public boolean loginCheck(String username, String password){
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "SELECT * from loginInfo WHERE username = '"+username+"' AND password = PASSWORD('"+password+"');";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();

            if (!rs.next()){
                conn.close();
                return false;   
            }
            else{
                conn.close();
            }
        }   catch (SQLException sql){
                System.out.println("exception");
        }
        return true;
    }
    
    public ArrayList<String> loginGetUsers(){
        ArrayList<String> users = new ArrayList<String>();
        
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "select * from loginInfo;";

            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet resultset = preparedStatement.executeQuery();
            
            while (resultset.next()) {
                users.add(resultset.getString("username"));
            }
            
            conn.close();
        } catch (SQLException ex){
            System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
        }
        
        return users;
        
        
    }
    
    public void loginPrint(){
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "SELECT * FROM loginInfo;";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet resultset = preparedStatement.executeQuery(); 

            while (resultset.next()) {
                System.out.println(resultset.getString("username") + "--> " + resultset.getString("password"));
            }
            
            conn.close();
        } catch (SQLException ex){
            System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
        }
    }
    
    public void tableCreate(String username){
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "CREATE TABLE "+username+"Table (movie INTEGER, rating INTEGER);";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.execute();
            
            conn.close();
        }catch(SQLException ex){
            System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
        }
    }
    
    public void tableInsert(String username, int movie, int rating){
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "INSERT INTO "+username+"Table (movie,rating)" 
            + "VALUES (?,?);";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setInt(1, movie);
            preparedStatement.setInt(2, rating);
            preparedStatement.execute();

            conn.close();
        } catch (SQLException ex){
                System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
            }
    }
    
    public int tableSize(String username){
        try{
            Connection conn = DB.getConnection("default");
            
            String query1 = "SELECT * FROM " + username+"Table;";
            String query2 = "SELECT COUNT(*) FROM "+username+"Table;";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query1);
            ResultSet rs = preparedStatement.executeQuery();
            rs = preparedStatement.executeQuery(query2);

            // get the number of rows from the result set
            rs.next();
            int rowCount = rs.getInt(1);
            
            conn.close();
            return rowCount;
        } catch(SQLException sql){
            System.out.println("THERE HAS BEEN AN SQLEXCEPTION IN TABLESIZE");
        }
        return 0;
    }
    
    public void tablePrint(String username){
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "SELECT * FROM "+username+"Table;";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet resultset = preparedStatement.executeQuery();
            
            while (resultset.next()) {
                System.out.println(resultset.getInt("movie") + "--> " + resultset.getInt("rating"));
            }
            
            conn.close();
        } catch (SQLException ex){
            System.out.println("THERE HAS BEEN AN SQLEXCEPTION");
        }
    }
    
    public TreeMap<Integer, Integer> tableGetMap(String username){
        TreeMap<Integer, Integer> userdata = new TreeMap<Integer, Integer>();
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "SELECT * FROM "+username+"Table;";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet resultset = preparedStatement.executeQuery();
            
            while(resultset.next()){
                userdata.put(resultset.getInt("movie"), resultset.getInt("rating"));
            }
            
            conn.close();
        } catch (SQLException ex){
            System.out.println("THERE HAS BEEN AN EXCEPTION tablegetMap for :" + username);
        }
        return userdata;
    }
    
    public ArrayList<Integer> tableGetLastTen(String username){
        ArrayList<Integer> lastten = new ArrayList<Integer>();
        try{
            Connection conn = DB.getConnection("default");
            
            String query = "SELECT * FROM "+username+"Table;";
            
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet resultset = preparedStatement.executeQuery();
           
            resultset.last();
            lastten.add(resultset.getInt("movie"));
           
            while(resultset.previous() && lastten.size() < 10){
                lastten.add(resultset.getInt("movie"));
            }
            
            conn.close();
            return lastten;
        } catch (SQLException ex){
            System.out.println("THERE HAS BEEN AN EXCEPTION");
        }
        return null;
    }
    
    /** END TO SQL CODE **/
    
    public void addToAll(String username, User user){
        
        if(user.username == null){
            user.setUserName(username);
        }
        user.setUserID(allusers.size()+1);
        allusers.put(username, user);  
        
    }
    
    public boolean hasUser(String username){
        if(allusers.containsKey(username)){
            return true;
        }
        return false;
    }
    
    public User getUserData(String username){
        return allusers.get(username);
    }
    
    public String findGenre(int id){
        return allgenres.get(id);
    }
    //Change by Daniel
    public String findMovie(int id){
        return allmovies.get(id-1);
    }
    
    public ArrayList<Integer> getTenRandomIDS(){
        ArrayList<Integer> random = new ArrayList<Integer>();
        for(int i = 0; i < 10; i++)
        {
          random.add((int)(Math.random() * allmovies.size()) + 1);  
        }
        makeRandomList(random);
        return random;
    }
    
    public ArrayList<String> getLastTen(String username){
        ArrayList<Integer> lasttenID = tableGetLastTen(username);
        ArrayList<String> lastten = new ArrayList<String>();
        for(int i = 0; i < lasttenID.size(); i++){
            lastten.add(findMovie(lasttenID.get(i)));
        }
        return lastten;
    }
    
    public void makeRandomList(ArrayList<Integer> movieIds){
        for(int i = 0; i < movieIds.size(); i++){
            shortlist.add(findMovie(movieIds.get(i)));
        }
    }
    
   // Added by Daniel  
    double xyStandarddeviation(double meanx,
			double meany, int moviesize, int usersize, TreeMap<Integer, Integer> userMap, String otheruser) {
		double sumx = 0;
		double sumy = 0;
		for (int j = 0; j < moviesize; j++) {
			int x = CheckUserID(userMap, j);
			int y = CheckUserID(otheruser, j);
			double xx =x  - meanx;
			sumx += xx * xx;
			double yy = y - meany;
			sumy += yy * yy;

		}
		sumx = sumx / (moviesize - 2);
		sumy = sumy / (moviesize - 2);
		sumx = Math.sqrt(sumx);
		sumy = Math.sqrt(sumy);
		//System.out.println("sx for user "+id + "is " + sumx  + "sy for user "+i +"is "+sumy );
		return sumx * sumy;

	}	
	
	public int CheckUserID(TreeMap<Integer, Integer> usermap, int movieindex){
	    
	    if (usermap.get(movieindex) == null){
	        return 0;
	    }
	    return usermap.get(movieindex);
	}
	
	// Added by Daniel  
      public int CheckUserID(String userid, int movieindex){
        /*ResultSet rs = statement.executeQuery("SELECT * FROM " + userid +"Table WHERE movie='" + movieindex +"'");
        
        
        if(rs.absolute(1)){
            System.out.println(movieindex);
            return rs.getInt("rating");
        }
        else{
            return 0;
        }
           
      }*/
        
    	if (allusers.get(userid).userdata.get(movieindex) == null){
			return 0;
		}
    	return allusers.get(userid).userdata.get(movieindex) ;
      }
    
    
    // Added by Daniel  
    public void suggestMovies(TreeMap<Integer, Integer> userMap, String recommender,
			ArrayList<Integer> suggest,int random) {
    
        
		int size = allmovies.size();
		//System.out.println(size);
		for (int i = 1; i <= size; i++) {
            //ResultSet checkRec = statement.executeQuery("SELECT * FROM " + recommender +"Table WHERE movie = '" + i +"'");
            //ResultSet checkUser = statement.executeQuery("SELECT * FROM " + user +"Table WHERE movie = '" + i +"'");
		    //if (random >= 2 && checkRec.absolute(1) && !checkUser.absolute(1) && !suggest.contains(i)) {
			if (random >=2 && allusers.get(recommender).userdata.containsKey(i)
				//	&& auserIndex.movieIndex.get(id).get(i) == 0
					&& !userMap.containsKey(i)
					&& !suggest.contains(i)) {
            //    String query = "SELECT * FROM "+recommender+"Table WHERE movie = '" +i+"';";
            //    ResultSet resultset = statement.executeQuery(query);
            //    int recRating = resultset.getInt("rating");			
			//	if (recRating == 5){	    
				if (allusers.get(recommender).userdata.get(i) == 5){
				    suggest.add(i);
				    return;
				}
			}
		}for (int i = 1; i <= size; i++) {
		    //ResultSet checkRec = statement.executeQuery("SELECT * FROM " + recommender +"Table WHERE movie = '" + i +"'");
            //ResultSet checkUser = statement.executeQuery("SELECT * FROM " + user +"Table WHERE movie = '" + i +"'");
		    //if (random >= 2 && checkRec.absolute(1) && !checkUser.absolute(1) && !suggest.contains(i)) {
			if (random >=2 && allusers.get(recommender).userdata.containsKey(i)
					//	&& auserIndex.movieIndex.get(id).get(i) == 0
						&& !userMap.containsKey(i)
						&& !suggest.contains(i)) {
				//String query = "SELECT * FROM "+recommender+"Table WHERE movie = '" +i+"';";
                //ResultSet resultset = statement.executeQuery(query);
                //int recRating = resultset.getInt("rating");		
				//if (recRating > 3){	    
				if (allusers.get(recommender).userdata.get(i) >3){
			        suggest.add(i);
			        return;
				}
			}
		}
	}

    
    
    
    // Added by Daniel  
    public void updateMovies(HashMap<String, MovieObject> pearsonmap,ArrayList<Integer> movies, TreeMap<Integer,Integer> usermap){
        ArrayList<MovieObject> finalResults = new ArrayList<MovieObject>(pearsonmap.values());
 	    Collections.sort(finalResults);
        int t = 10;
        //System.out.println("Index 0 is " + finalResults.get(0).getID());
		for (int j = 1; j <= t; j++) {
			//Random rand = new Random();
			//int random = rand.nextInt(4);//incase we implement backward and forward traversal
			//random = 2;// debugging
			
			MovieObject i = finalResults.get(j);
	        suggestMovies(usermap, i.getID(), movies,2);
	        
	        //System.out.println(movies);
	        //Logger.debug("message: %s, movies);
		}

 	
    }
    
    // Added by Daniel  //implementing Apache
    public void checkForSimUsers(String user, ArrayList<Integer> movies){
        
         //try {
           // Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    
        	HashMap<String, MovieObject> pearsonmap = new HashMap<String, MovieObject>();
	    	//TreeMap<Integer, Integer> userMap = allusers.get(user).userdata;
	    	
		    TreeMap<Integer, Integer> userMap = tableGetMap(user);
		
	        int usersize = allusers.size(); //neww
	        int moviesize = allmovies.size(); //neww
	    
	        //ArrayList<String> usernames = loginGetUsers();
	        //int usersize = usernames.size();
	        //for(int i = 0; i < usersize-1; i++) {
	    
	        for(Entry<String, User>  entry : allusers.entrySet()) { //neww
			    double sumofx = 0;
			    double sumofy = 0;
		    	double xy = 0;
			        for (int j = 0; j < moviesize; j++) {
			    	    int x = CheckUserID(userMap,j);
			    	    //int y = CheckUserID(usernames.get(i), j, statement);
			    	    int y = CheckUserID(entry.getKey(),j);
			    	    sumofx += x;
			    	    sumofy += y;
			    	    xy += (x * y);
			        }
			        double meanx = sumofx / (moviesize - 1);
			        double meany = sumofy / (moviesize - 1);
			        
			        //double sd = xyStandarddeviation(meanx, meany, moviesize, usersize, user, usernames.get(i), statement);
		            double sd = xyStandarddeviation(meanx, meany,
			        		moviesize, usersize, userMap, entry.getKey());
			        double meanxy = meanx * meany;
			        meanxy = (moviesize - 1) * meanxy;
			        xy = xy - meanxy;
			        xy = xy / (moviesize - 2);
			        double pcc = xy / sd;
			        //pearsonmap.put(usernames.get(i), new MovieObject(usernames.get(i), pcc));
			        pearsonmap.put(entry.getKey(),
				      	new MovieObject(entry.getKey(),pcc));	
		    }
	        updateMovies(pearsonmap, movies, userMap);
        } 
	     
	     /*try{
		 DataModel model = new FileDataModel(new File("data/dataset.csv")); //should be changed after every user submission
		UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
		UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
		UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
		//GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(model,similarity);
		int ouruser = Integer.parseInt(user);
		List<RecommendedItem> recommendations = recommender.recommend(ouruser, 10);
		
		//long[] users = recommender.mostSimilarUserIDs(user, 5);
		for (RecommendedItem recommendation : recommendations) {
		  System.out.println(recommendation);
		  movies.add((int)recommendation.getItemID());
		  
		}
		}
		catch (IOException | TasteException e) {
			// TODO Auto-generated catch block
			System.out.println("I/O or Taste Exception");
		}
}
	     */
	     

    
        /*
    Hui's Code
    
    public void checkForSimUsers(String user, ArrayList<Integer> movies){
		
		TreeMap<Integer, Integer> userMap = allusers.get(user).userdata;
	
	    ArrayList<String> userList = new ArrayList<String>();
	    
	    pearson(user, userMap, userList);
		
		
		for(int i = 0; i < userList.size(); i++){
			User u = allusers.get(userList.get(i));
			for (int m : u.userdata.keySet()) {
				if (u.userdata.get(m) == 5 && !movies.contains(m) && movies.size() < 10) {
					movies.add(m);
				}
			}
		}
		
					
	}
	
   public void pearson(String user, TreeMap<Integer, Integer> userMap, 
			ArrayList<String> userList) {
		double avg = 0, top = 0, bottom_x = 0,bottom_y = 0, similarity = 0;
		double[] averages = new double[allusers.keySet().size()];
		double[] x_values = new double[allmovies.size()];
		double[] y_values;
		TreeMap<Double, String> similarities = new TreeMap<Double, String>();

		
		for (String u : allusers.keySet()) {
			for (int m : allusers.get(u).userdata.keySet()) {
				avg += allusers.get(u).userdata.get(m);
			}
			avg = avg / allmovies.size();
			averages[allusers.get(u).userid - 1] = avg;
		}

		for (int i = 1; i <= allmovies.size(); i++) {
			if (allusers.get(user).userdata.containsKey(i)) {
				x_values[i-1] = allusers.get(user).userdata.get(i) - averages[allusers.get(user).userid - 1];
			} else {
				x_values[i-1] = 0 - averages[allusers.get(user).userid - 1];
			}
		}
		
		for (String uid : allusers.keySet()) {
			y_values = new double[allmovies.size()];
			similarity = 0;
			
			for (int i = 1; i < allmovies.size(); i++) {
				if (allusers.get(uid).userdata.containsKey(i)) {
					y_values[i-1] = allusers.get(uid).userdata.get(i) - averages[allusers.get(uid).userid -1];
				} else {
					y_values[i-1] = 0 - averages[allusers.get(uid).userid - 1];
				}
			}
						
			for (int j = 0; j < allmovies.size(); j++) {
				top += x_values[j]*y_values[j];
				bottom_x += x_values[j] * x_values[j];
				bottom_y += y_values[j]*y_values[j];
			}
			similarity = top/(Math.sqrt(bottom_x) * Math.sqrt(bottom_y));
			similarities.put(similarity, uid);

			top = 0;
			bottom_x = 0;
			bottom_y = 0;
		}
		
		NavigableMap<Double,String> nmap = similarities.descendingMap();
		int count = 10;
		for (double i : nmap.keySet()) {
			if  (count > 0 && !user.equals(nmap.get(i))) {
				userList.add(nmap.get(i).toString());
				count -- ;
			}
		}
		

	}
   */ 
}