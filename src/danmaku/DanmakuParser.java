package danmaku;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import base.model.DanmakuModel;
import base.model.GiftRank;
import base.model.MsgType;

public class DanmakuParser {
	public static DanmakuModel parse (String jsonText, long createTime, int version) {
		DanmakuModel d = new DanmakuModel (jsonText, createTime, version);
        switch (version) {
            case 1:{
            	JsonArray array = new JsonParser().parse(jsonText).getAsJsonArray();
                d.MsgType = MsgType.Comment;
                d.CommentText = array.get(1).getAsString();
                d.UserName = array.get(2).getAsJsonArray().get(1).getAsString();
            } break;
            case 2:{
                try {
                    resolveVersion2 (d,  new JsonParser().parse(jsonText).getAsJsonObject());
                } catch (Exception e) {
                    System.out.println (jsonText);
                    e.printStackTrace ();
                }
            }  break;
        }
        return d;
    }

    private static void resolveVersion2 (DanmakuModel d, JsonObject obj) {
        String cmd = obj.get("cmd").getAsString();
        switch (cmd) {
            case "LIVE":{
                d.MsgType = MsgType.LiveStart;
                d.roomID = obj.get("roomid").getAsString ();
            } break;
            case "PREPARING":{
                d.MsgType = MsgType.LiveEnd;
                d.roomID = obj.get("roomid").getAsString ();
            } break;
            case "DANMU_MSG":{
                d.MsgType = MsgType.Comment;
                resolveDanmakuMsg (d, obj);
            } break;
            case "SEND_GIFT":{
                d.MsgType = MsgType.GiftSend;
                JsonObject data = obj.get("data").getAsJsonObject();
                d.GiftName = data.get("giftName").getAsString ();
                d.UserName = data.get("uname").getAsString ();
                d.UserID = data.get("uid").getAsInt();
                // Giftrcost = data.get("rcost").getAsString();
                d.GiftCount = data.get("num").getAsInt();
            }break;
            case "GIFT_TOP":
                d.MsgType = MsgType.GiftTop;
                resolveGifTop (d, obj);
                break;
            case "WELCOME":{
                d.MsgType = MsgType.Welcome;
                JsonObject data = obj.get("data").getAsJsonObject();
                d.UserName = data.get("uname").getAsString();
                d.UserID = data.get("uid").getAsInt();
                d.isVIP = true;
                d.isAdmin = data.get("isadmin").getAsString ().equals("1");
            } break;
            case "WELCOME_GUARD":{
                d.MsgType = MsgType.WelcomeGuard;
                JsonObject data = obj.get("data").getAsJsonObject();
                d.UserName = data.get("username").getAsString ();
                d.UserID = data.get("uid").getAsInt();
                d.UserGuardLevel =data.get("guard_level").getAsInt();
            } break;
            case "GUARD_BUY":{
                d.MsgType = MsgType.GuardBuy;
                JsonObject data = obj.get("data").getAsJsonObject();
                d.UserID = data.get("uid").getAsInt();
                d.UserName = data.get("username").getAsString();
                d.UserGuardLevel = data.get("guard_level").getAsInt();
                d.GiftName = d.UserGuardLevel == 3 ? "舰长" : d.UserGuardLevel == 2 ? "提督" : d.UserGuardLevel == 1 ? "总督" : "";
                d.GiftCount = data.get("num").getAsInt();
            }  break;
            default:
                d.MsgType = MsgType.Unknown;
                break;
        }
    }

    private static void resolveGifTop (DanmakuModel d, JsonObject obj) {
    	JsonArray alltop = obj.get("data").getAsJsonArray();
        d.GiftRanking = new ArrayList<GiftRank> ();
        for(int i = 0; i < alltop.size(); i++) {
        	GiftRank gr = new GiftRank();
        	JsonObject item = alltop.get(i).getAsJsonObject();
        	gr.Uid=item.get("uid").getAsInt();
        	gr.UserName=item.get("uname").getAsString();
        	gr.Coin=item.get("coin").getAsInt();
            d.GiftRanking.add(gr);
        }
    }

    private static void resolveDanmakuMsg (DanmakuModel d, JsonObject obj) {
    	JsonArray data = obj.get("info").getAsJsonArray();
        int length = data.size();
        if (length > 7) {
            d.UserGuardLevel = data.get(7).getAsInt();
        }
        JsonArray a2=data.get(2).getAsJsonArray();
        JsonArray a0=data.get(0).getAsJsonArray();
        d.CommentText = data.get(1).getAsString();
        d.UserID = a2.get(0).getAsInt();
        d.UserName = a2.get(1).getAsString ();
        d.isAdmin = "1".equals(a2.get(2).getAsString());
        d.isVIP = "1".equals(a2.get(3).getAsString());
        //Get text only danmaku extension
        d.DmType = a0.get(1).getAsInt();
        d.Fontsize = a0.get(2).getAsInt();
        d.Color = a0.get(3).getAsInt();
        d.SendTimestamp = a0.get(4).getAsLong();
        d.UserHash =a0.get(7).getAsString();
    }
}
