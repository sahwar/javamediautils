package pt.com.santos.util.media.imdb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.TextExtractor;
import pt.com.santos.util.Pair;
import pt.com.santos.util.StringUtilities;

public class IMDB {

    private IMDB() {
        throw new UnsupportedOperationException();
    }
    protected static String USER_AGENT =
            "Mozilla/5.0 (Windows; U; "
            + "Windows NT 6.0; en-GB; rv:1.9.1.2) Gecko/20090729 "
            + "Firefox/3.5.2 (.NET CLR 3.5.30729)";

    protected static HttpURLConnection redirect(HttpURLConnection conn)
            throws IOException {
        // normally, 3xx is redirect
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                // get redirect url from "location" header field
                String newUrl = conn.getHeaderField("Location");

                // get the cookie if need, for login
                String cookies = conn.getHeaderField("Set-Cookie");

                // open the new connnection again
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.setRequestProperty("User-Agent", USER_AGENT);
            }
        }
        return conn;
    }

    public static List<String> getSpokenLanguages(String imdbID)
            throws IOException {
        checkParameter(imdbID);
        List<String> res = new ArrayList<String>();
        URL url = new URL("http://www.imdb.com/title/" + imdbID + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        conn = redirect(conn);

        Source source = new Source(conn.getInputStream());
        source.setLogger(null);
        source.fullSequentialParse();
        Element element = source.getElementById("titleDetails");
        for (Element h4 : element.getAllElements(HTMLElementName.H4)) {
            Segment ch4 = h4.getContent();
            if (ch4 == null) {
                continue;
            }
            String sch4 = ch4.toString();
            if (sch4 == null || sch4.compareTo("Language:") != 0) {
                continue;
            }

            List<Element> childs = h4.getParentElement().getChildElements();
            if (childs.size() <= 1) {
                continue;
            }

            for (Element elem : childs) {
                if (!elem.getName().equals(HTMLElementName.A)) {
                    continue;
                }
                Segment content2 = elem.getContent();
                if (content2 == null) {
                    continue;
                }
                String lang = content2.toString();
                res.add(lang.trim());
            }
        }

        return res;
    }

    public static String getIMDBID(String search)
            throws IOException {
        if (search == null) {
            return null;
        }
        String imdb = null;
        search = search.replace(":", "");
        search = URLEncoder.encode(search, "UTF-8");
        URL url = new URL("http://www.imdb.com/find?s=tt&q="
                + search);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);

        conn = redirect(conn);

        Source source = new Source(conn.getInputStream());
        source.setLogger(null);
        String x = source.toString();
        int l = x.indexOf("div id=\"main\"");
        if (l < 0) {
            l = x.indexOf("<head>");
        }

        if (l >= 0) {
            x = x.substring(l);
            Pattern pattern2 = Pattern.compile("tt\\d{7}");
            Matcher matcher = pattern2.matcher(x);
            if (matcher.find()) {
                imdb = matcher.group(0);
            }
        }

        if (imdb != null) {
            imdb = imdb.substring(2);
        }
        return imdb;
    }
    
    public static Pair<String, String>
            getOriginalTitle(String imdbLink) throws IOException {
        URL url = new URL(imdbLink);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn = redirect(conn);
        Source source = new Source(conn.getInputStream());
        source.setLogger(null);
        Element yearElement = source.getElementById("titleYear");
        Element yearAElement = 
                yearElement.getFirstElement(HTMLElementName.A);
        String year = yearAElement.getContent().toString();
        
        StartTag originalTitleTag =
                source.getFirstStartTag("div class=\"originalTitle\"");
        
        Element element = null;
        if (originalTitleTag != null) {
            element = originalTitleTag.getElement();            
        } else {
            StartTag titleWrapperDivTag = 
                    source.getFirstStartTag("div class=\"title_wrapper\"");
            Element titleWrapperDivElement = titleWrapperDivTag.getElement();
            element = titleWrapperDivElement.
                    getFirstElement(HTMLElementName.H1);
        }
        
        String result = new HTMLElementTextExtractor(element, 
                HTMLElementName.SPAN).toString();
        
        if (StringUtilities.isNullOrEmpty(result)) {
            return null;
        }
        
        return new Pair<String, String>(year, result);
    }
    
    private static class HTMLElementTextExtractor extends TextExtractor {
        protected String htmlElement;
        public HTMLElementTextExtractor(Segment sgmnt, String htmlElement) {
            super(sgmnt);
            this.htmlElement = htmlElement;
        }
        @Override
        public boolean excludeElement(StartTag startTag) {
            boolean result = super.excludeElement(startTag);
            return result || startTag.getName().equals(htmlElement);
        }
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
