package base.model;

import java.util.List;

public class DanmakuModel {
	// Danmaku type
	public MsgType MsgType;

	// Danmaku text
	public String CommentText;

	public String UserName;

	public int UserID;

	/// <summary>
	/// 用户舰队等级
	/// <para>0 为非船员 1 为总督 2 为提督 3 为舰长</para>
	public int UserGuardLevel;

	public String GiftName;

	/// <summary>
	/// 礼物数量
	/// <para>此项有值的消息类型：<list type="bullet">
	/// <item><see cref="MsgTypeEnum.GiftSend"/></item>
	/// <item><see cref="MsgTypeEnum.GuardBuy"/></item>
	/// </list></para>
	/// <para>此字段也用于标识上船 <see cref="MsgTypeEnum.GuardBuy"/> 的数量（月数）</para>
	/// </summary>
	public int GiftCount;

	/// <summary>
	/// 禮物排行
	/// <para>此项有值的消息类型：<list type="bullet">
	/// <item><see cref="MsgTypeEnum.GiftTop"/></item>
	/// </list></para>
	/// </summary>
	public List<GiftRank> GiftRanking;

	/// <summary>
	/// 该用户是否为房管（包括主播）
	/// <para>此项有值的消息类型：<list type="bullet">
	/// <item><see cref="MsgTypeEnum.Comment"/></item>
	/// <item><see cref="MsgTypeEnum.GiftSend"/></item>
	/// </list></para>
	/// </summary>
	public boolean isAdmin;

	/// <summary>
	/// 是否VIP用戶(老爺)
	/// <para>此项有值的消息类型：<list type="bullet">
	/// <item><see cref="MsgTypeEnum.Comment"/></item>
	/// <item><see cref="MsgTypeEnum.Welcome"/></item>
	/// </list></para>
	/// </summary>
	public boolean isVIP;

	/// <summary>
	/// <see cref="MsgTypeEnum.LiveStart"/>,<see cref="MsgTypeEnum.LiveEnd"/>
	/// 事件对应的房间号
	/// </summary>
	public String roomID;

	/// <summary>
	/// 原始数据, 高级开发用
	/// </summary>
	public String RawData;

	/// <summary>
	/// 内部用, JSON数据版本号 通常应该是2
	/// </summary>
	public int JSON_Version;

	// extension for text only danmaku
	public int DmType = 1;
	public int Fontsize;
	public int Color;
	public long SendTimestamp;
	public String UserHash;
	public final long CreateTime;

	public DanmakuModel() {
		CreateTime = 0;
	}

	public DanmakuModel(String JSON, long createTime) {
		this(JSON, createTime, 1);
	}

	public DanmakuModel(String JSON, long createTime, int version) {
		RawData = JSON;
		CreateTime = createTime;
		JSON_Version = version;
	}

	public String toString(long startTime) {
		return "<d p=\"" + ((CreateTime - startTime) / 1000) + "," + DmType + "," + Fontsize + "," + Color + ","
				+ SendTimestamp + ",0," + UserHash + ",0\">" + CommentText + "</d>";
	}
}
