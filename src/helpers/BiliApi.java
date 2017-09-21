package helpers;

import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import base.Utils;
import base.interfaces.ILogger;
import base.model.LiveStatus;
import base.model.RoomInfo;
import download.DownloadUtils;

@SuppressWarnings("unused")
public class BiliApi {

    public static class Const {
        public static final String AppKey = "<Bilibili%20App%20Key%20Here>";
        public static final String SecretKey = "<Bilibili%20App%20Secret%20Key%20Here>";
        public static final String CidUrl = "http://live.bilibili.com/api/player?id=cid:";
        public static final String[] DefaultHosts = new String[]{ "livecmt-2.bilibili.com", "livecmt-1.bilibili.com" };
        public static final String DefaultChatHost = "chat.bilibili.com";
        public static final int DefaultChatPort = 2243;
    }
    
    public static class ServerBean{
    	public String url;
    	public String port;
    	public boolean mayNotExist;
    	public boolean ok;
    }

    public final String userAgent;
    private final ILogger logger;
    private final StatHelper statHelper;

    public BiliApi (ILogger logger, String userAgent){
    	this(logger, userAgent, null);
    }
    public BiliApi (ILogger logger, String userAgent, StatHelper statHelper) {
        this.logger = logger;
        this.userAgent = userAgent;
        this.statHelper = statHelper;
    }

    public ServerBean getDmServerAddr (String roomId) {
    	ServerBean bean = new ServerBean();
        //Get real danmaku server url
    	bean.mayNotExist = false;
        //Download xml file
        String xmlText = null;
        try {
            xmlText = DownloadUtils.downloadString(Const.CidUrl + roomId);
            System.out.println(xmlText);
        } /*catch (Exception e) {
            e.printStackTrace ();
            var errorResponse = e.Response as HttpWebResponse;
            if (errorResponse != null && errorResponse.StatusCode == HttpStatusCode.NotFound) {
                logger.appendLine ("ERROR", "Maybe "+roomId+" is not a valid room id.");
                bean.mayNotExist = true;
            } else {
                logger.appendLine ("ERROR", "Download cid xml fail : " + e.getMessage());
            }
        }*/ catch (Exception e) {
            e.printStackTrace ();
            logger.appendLine ("ERROR", "Download cid xml fail : " + e.getMessage());
        }
        if (Utils.isNullOrEmpty (xmlText)) {
            return bean;
        }
        
        try {
            //Analyzing danmaku Xml
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource src = new InputSource(new StringReader("<root>" + xmlText + "</root>"));
            Document doc = db.parse(src);
        	bean.url = doc.getElementsByTagName ("dm_server").item(0).getFirstChild().getNodeValue();
        	bean.port = doc.getElementsByTagName ("dm_port").item(0).getFirstChild().getNodeValue();
        	bean.ok=true;
        	return bean;
        } catch (Exception e) {
            e.printStackTrace ();
            logger.appendLine ("ERROR", "Analyzing XML fail : " + e.getMessage());
            return bean;
        }
    }

    public String[] tryGetRoomIdAndUrl (String roomId) {
        String flvUrl = null;
        String realRoomId = getRealRoomId (roomId);
        if (Utils.isNullOrEmpty (realRoomId)) {
            return null;
        }
        //Step2.Get flv url
        try {
        	flvUrl = getRealUrl (realRoomId);
        	if(!Utils.isNullOrEmpty(flvUrl)){
        		return new String[] {realRoomId, flvUrl};
        	}
        } catch (Exception e) {
            e.printStackTrace ();
            logger.appendLine ("ERROR", "Get real url fail, Msg : " + e.getMessage());
        }
        return null;
    }

    public String getRealRoomId (String originalRoomId) {
        logger.appendLine ("INFO", "Trying to get real roomId");

        String roomWebPageUrl = "http://live.bilibili.com/" + originalRoomId;
        /*var wc = new WebClient ();
        wc.Headers.Add ("Accept: text/html");
        wc.Headers.Add ("User-Agent: " + userAgent);
        wc.Headers.Add ("Accept-Language: zh-CN,zh;q=0.8,en;q=0.6,ja;q=0.4");*/
        String roomHtml;

        try {
            roomHtml = DownloadUtils.downloadString(roomWebPageUrl);
        } catch (Exception e) {
            logger.appendLine ("ERROR", "Open live page fail : " + e.getMessage());
            return null;
        }

        //Get real room id from HTML
        Pattern pattern = Pattern.compile("(?<=var ROOMID = )(\\d+)(?=;)");
        Matcher matcher = pattern.matcher(roomHtml);
        while(matcher.find()) {
            logger.appendLine ("INFO", "Real Room Id : " + matcher.group());
            return matcher.group();
        }

        logger.appendLine ("ERROR", "Fail Get Real Room Id");
        return null;
    }

    public String getRealUrl (String roomId) throws Exception {
        if (roomId == null) {
            logger.appendLine ("ERROR", "Invalid operation, No roomId");
            throw new Exception ("No roomId");
        }
        String apiUrl = createApiUrl (roomId);
        logger.appendLine ("INFO", "Fetch apiUrl : " + apiUrl);

        String xmlText;

        //Get xml by API
        //var wc = getBaseWebClient ();
        try {
            xmlText = DownloadUtils.downloadString(apiUrl);
        } catch (Exception e) {
            logger.appendLine ("ERROR", "Fail sending analysis request : " + e.getMessage());
            throw e;
        }

        //Analyzing xml
        String realUrl = "";
        try {
        	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource src = new InputSource(new StringReader(xmlText));
            Document doc = db.parse(src);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPathExpression expr = xPathfactory.newXPath().compile("/video/result");
            String nl = (String)expr.evaluate(doc, XPathConstants.STRING);
        	//Get analyzing result
            if (nl == null || nl.length() <=0 || !"suee".equals (nl)) {
                logger.appendLine ("ERROR", "Analyzing url address fail");
                throw new Exception ("No Avaliable download url in xml information.");
            }
            XPathExpression expr2 = xPathfactory.newXPath().compile("/video/durl/url");
            realUrl = (String)expr2.evaluate(doc, XPathConstants.STRING);;
        } catch (Exception e) {
            e.printStackTrace ();
            logger.appendLine ("ERROR", "Analyzing XML fail : " + e.getMessage());
            throw e;
        }
        if (!Utils.isNullOrEmpty (realUrl)) {
            logger.appendLine ("INFO", "Analyzing url address successful!");
            logger.appendLine ("INFO", realUrl);
        }
        return realUrl;
    }

    //TODO complete
    public RoomInfo getRoomInfo (int realRoomId) {
        String url = "https://live.bilibili.com/live/getInfo?roomid="+realRoomId;
        String infoJson;

        try {
            infoJson = DownloadUtils.downloadString (url);
            JsonObject info = new JsonParser().parse(infoJson).getAsJsonObject();
            JsonObject data =info.getAsJsonObject("data");
            System.out.println(infoJson);
            System.out.println(data.isJsonObject());
            //logger.appendLine ("Info", infoJson);
            if (data != null && data.isJsonObject()) {
                logger.appendLine ("_status", data.get("_status").getAsString());
                logger.appendLine ("LiveStatus", data.get("LIVE_STATUS").getAsString());
                logger.appendLine ("RoomTitle", data.get("ROOMTITLE").getAsString());

                RoomInfo roomInfo = new RoomInfo ();
                roomInfo.liveStatus = LiveStatus.valueOf(data.get ("LIVE_STATUS").getAsString().toUpperCase());
                roomInfo.isOn = "on".equals (data.get ("_status").getAsString().toLowerCase());
                roomInfo.title = data.get ("ROOMTITLE").getAsString();
                roomInfo.timeLine = data.get("LIVE_TIMELINE").getAsInt();
                roomInfo.anchor = data.get("ANCHOR_NICK_NAME").getAsString();
                return roomInfo;
            }
        } catch (Exception e) {
            logger.appendLine ("ERROR", "Open live page fail : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String createApiUrl (String roomId) {
        //Generate parameters
        StringBuilder apiParams = new StringBuilder ().append ("appkey=").append (Const.AppKey).append ("&")
            .append ("cid=").append (roomId).append ("&")
            .append ("player=1&quality=0&ts=");
        Date ts = new Date(System.currentTimeMillis()); //UNIX TimeStamp
        apiParams.append (String.valueOf(ts.getTime()/1000));

        String apiParam = apiParams.toString (); //Origin parameters String

        //Generate signature
        String waitForSign = apiParam + Const.SecretKey;
        String sign = "";
		try {
			sign = Utils.md5(waitForSign);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
        //Final API
        return "http://live.bilibili.com/api/playurl?" + apiParam + "&sign=" + sign;
    }
}
