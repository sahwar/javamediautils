package pt.com.santos.util.media.tmdb;

import java.util.Locale;
import org.json.JSONArray;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.json.JSONObject;
import pt.com.santos.util.StringUtilities;

public class TMDbSearch {

    private static final String API_KEY = "d4ad46ee51d364386b6cf3b580fb5d8c";
    private static String BASE_PATH =
            "https://api.themoviedb.org/3/%s?api_key=" + API_KEY + "%s";
    private static final Logger logger =
            Logger.getLogger(TMDbSearch.class.getName());
    protected static DefaultHttpClient client;

    static {
        client = new DefaultHttpClient();
    }

    public static Logger getLogger() {
        return logger;
    }

    private TMDbSearch() {
        throw new UnsupportedOperationException();
    }

    public static List<TMDbMovieSearchResult> search(String query) 
            throws IOException {
        checkParameter(query);
        //query = query.replaceAll(" ", "+");
        query = query.replaceAll(":", "");
        query = query.replaceAll("-", " ");
        query = URLEncoder.encode(query, "UTF-8");

        HttpResponse response = client.execute(new HttpGet(
                String.format(BASE_PATH, "configuration", "")));

        HttpEntity entity = response.getEntity();
        String answer = EntityUtils.toString(entity);
        JSONObject json = new JSONObject(answer);
        json = json.getJSONObject("images");

        String baseURL = json.getString("base_url");

        response = client.execute(new HttpGet(
                String.format(BASE_PATH, "search/movie", "&query=" + query)));
        entity = response.getEntity();
        answer = EntityUtils.toString(entity);
        json = new JSONObject(answer);

        int pages = json.getInt("total_pages");

        List<TMDbMovieSearchResult> l = new ArrayList<TMDbMovieSearchResult>();
        int page = 1;
        
        do {
            JSONArray results = json.getJSONArray("results");

            for (Object obj : results) {
                JSONObject result = (JSONObject) obj;
                String name = result.getString("title");

                if (name == null || name.length() <= 0) {
                    continue;
                }
                //System.out.println(name);
                int id = result.getInt("id");

                //make request to movie
                response = client.execute(new HttpGet(
                        String.format(BASE_PATH, "movie/" + id, "")));
                entity = response.getEntity();
                answer = EntityUtils.toString(entity);

                JSONObject movie = new JSONObject(answer);
                String imdbID = movie.isNull("imdb_id")
                        ? null : movie.getString("imdb_id");
                String homepage = movie.isNull("homepage")
                        ? null : movie.getString("homepage");
                URL url = homepage == null ? null : new URL(homepage);

                String imgURL = movie.isNull("poster_path")
                        ? null : movie.getString("poster_path");
                l.add(new TMDbMovieSearchResult(name, id + "",
                        imgURL == null ? null : 
                        new URL(baseURL + "original" + imgURL), url, imdbID));
            }
            if (++page <= pages) {
                //get next page
                response = client.execute(new HttpGet(
                        String.format(BASE_PATH, "search/movie", "&query="
                        + query + "&page=" + page)));
                entity = response.getEntity();
                answer = EntityUtils.toString(entity);
                json = new JSONObject(answer);
            }
        } while (page <= pages);

        return l;
    }
    
    public static String getID(String imdbID)
            throws IOException {
        checkParameter(imdbID);
        String httpGetUrl = String.format(BASE_PATH, "find/" + imdbID, 
                "&external_source=imdb_id");
        HttpResponse response = client.execute(new HttpGet(httpGetUrl));

        HttpEntity entity = response.getEntity();
        String answer = EntityUtils.toString(entity);
        JSONObject json = new JSONObject(answer);
        JSONArray jsonArray = json.getJSONArray("movie_results");
        
        if (jsonArray.length() < 1) return null;
        
        return jsonArray.getJSONObject(0).getInt("id") + "";
    }

    public static String getOriginalLanguage(String movieID) 
            throws IOException {
        checkParameter(movieID);
        String httpGetUrl = String.format(BASE_PATH, "movie/" + movieID, "");
        HttpResponse response = client.execute(new HttpGet(httpGetUrl));

        HttpEntity entity = response.getEntity();
        String answer = EntityUtils.toString(entity);

        JSONObject movie = new JSONObject(answer);
        
        if (!movie.isNull("original_language")) {
            String lang = movie.getString("original_language");
            if (!StringUtilities.isNullOrEmpty(lang)) {
                return new Locale(lang).getDisplayLanguage();
            }
        }
        
        return null;
    }
    
    public static List<String> getSpokenLanguages(String movieID) 
        throws IOException {
        checkParameter(movieID);
        List<String> res = new ArrayList<String>();
        String httpGetUrl = String.format(BASE_PATH, "movie/" + movieID, "");
        HttpResponse response = client.execute(new HttpGet(httpGetUrl));

        HttpEntity entity = response.getEntity();
        String answer = EntityUtils.toString(entity);

        JSONObject movie = new JSONObject(answer);
        
        if (!movie.isNull("spoken_languages")) {
            JSONArray jsonSpokenLangs = 
                    movie.getJSONArray("spoken_languages");
            for (int i = 0; i < jsonSpokenLangs.length(); i++) {
                JSONObject jsonSpokenLang = 
                        jsonSpokenLangs.getJSONObject(i);
                String langISO6391 = 
                        jsonSpokenLang.isNull("iso_639_1") ? null :
                        jsonSpokenLang.getString("iso_639_1");
                String lang = jsonSpokenLang.isNull("name") ? null : 
                        jsonSpokenLang.getString("name");
                if (!StringUtilities.isNullOrEmpty(langISO6391)) {
                    lang = new Locale(langISO6391).getDisplayLanguage();
                }
                    
                if (StringUtilities.isNullOrEmpty(lang)) continue;
                
                res.add(lang);
            }
        }
        
        return res;
    }

    private static void checkParameter(String s) {
        if (s == null) {
            throw new NullPointerException(
                    "the parameter can't be null");
        }
        if (s.isEmpty()) {
            throw new IllegalArgumentException(
                    "the parameter can't be empty");
        }
    }
}
