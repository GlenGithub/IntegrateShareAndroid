package com.comsince.phonebook.receiver;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;
import com.comsince.phonebook.MainActivity;
import com.comsince.phonebook.PhoneBookApplication;
import com.comsince.phonebook.R;
import com.comsince.phonebook.asynctask.SendMsgAsyncTask;
import com.comsince.phonebook.constant.Constant;
import com.comsince.phonebook.entity.Message;
import com.comsince.phonebook.entity.MessageItem;
import com.comsince.phonebook.entity.User;
import com.comsince.phonebook.preference.PhoneBookPreference;
import com.comsince.phonebook.util.L;
import com.comsince.phonebook.util.PhoneBookUtil;
import com.comsince.phonebook.util.PhotoUtil;
import com.comsince.phonebook.util.T;
import com.google.gson.Gson;
import com.tencent.mm.sdk.platformtools.PhoneUtil;


/**
 * Push消息处理receiver
 */
public class PushMessageReceiver extends BroadcastReceiver {
	/** TAG to Log */
	public static final String TAG = PushMessageReceiver.class.getSimpleName();

	AlertDialog.Builder builder;
	/**其他在线用户接到发送来的消息就设置tag为此值，在发送给本身**/
	public static final String RESPONSE = "response";
	public static final String PHONEBOOK = "phoneBook";
	
	public static int mNewNum = 0;// 通知栏新消息条目
	public static final int NOTIFY_ID = 0x000;
	
	public static abstract interface EventHandler {
		
		public abstract void onMessage(Message message);

		public abstract void onBind(String method, int errorCode, String content);

		public abstract void onNotify(String title, String content);

		public abstract void onNetChange(boolean isNetConnected);
        /**增加新的朋友**/
		public void onNewFriend(User u);
	}
	
	/**处理消息的回调集合**/
	public static ArrayList<EventHandler> ehList = new ArrayList<EventHandler>();

	/**
	 * 
	 * 
	 * @param context
	 *            Context
	 * @param intent
	 *            接收的intent
	 */
	@Override
	public void onReceive(final Context context, Intent intent) {

		Log.d(TAG, ">>> Receive intent: \r\n" + intent);

		if (intent.getAction().equals(PushConstants.ACTION_MESSAGE)) {
			//获取消息内容
			String message = intent.getExtras().getString(
					PushConstants.EXTRA_PUSH_MESSAGE_STRING);

			//消息的用户自定义内容读取方式
			L.i("onMessage: " + message);
           // T.showShort(context, message);
			//用户在此自定义处理消息,以下代码为demo界面展示用
			try {
				Message msgItem = PhoneBookApplication.getInstance().getGson().fromJson(message, Message.class);
				parseMessage(msgItem);// 预处理，过滤一些消息，比如说新人问候或自己发送的
			} catch (Exception e) {
			}

		} else if (intent.getAction().equals(PushConstants.ACTION_RECEIVE)) {
			//处理绑定等方法的返回数据
			//PushManager.startWork()的返回值通过PushConstants.METHOD_BIND得到
			
			//获取方法
			final String method = intent.getStringExtra(PushConstants.EXTRA_METHOD);
			//方法返回错误码。若绑定返回错误（非0），则应用将不能正常接收消息。
			//绑定失败的原因有多种，如网络原因，或access token过期。
			//请不要在出错时进行简单的startWork调用，这有可能导致死循环。
			//可以通过限制重试次数，或者在其他时机重新调用来解决。
			final int errorCode = intent.getIntExtra(PushConstants.EXTRA_ERROR_CODE,PushConstants.ERROR_SUCCESS);
			// 返回内容
			final String content = new String(intent.getByteArrayExtra(PushConstants.EXTRA_CONTENT));

			// 用户在此自定义处理消息,以下代码为demo界面展示用
			paraseContent(context, errorCode, content);// 处理消息
			L.i(content);

			// 回调函数
			for (int i = 0; i < ehList.size(); i++)
				((EventHandler) ehList.get(i)).onBind(method, errorCode, content);

		//可选。通知用户点击事件处理
		} else if (intent.getAction().equals(
				PushConstants.ACTION_RECEIVER_NOTIFICATION_CLICK)) {
			Log.d(TAG, "intent=" + intent.toUri(0));
			//弹出通知栏消息用,并做点击调用
			Constant.IS_UPDATE_DATA_FROM_NOTIFICATION = "Y";
			Intent aIntent = new Intent();
			aIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			aIntent.setClass(context, MainActivity.class);
			String title = intent.getStringExtra(PushConstants.EXTRA_NOTIFICATION_TITLE);
			aIntent.putExtra(PushConstants.EXTRA_NOTIFICATION_TITLE, title);
			String content = intent.getStringExtra(PushConstants.EXTRA_NOTIFICATION_CONTENT);
			aIntent.putExtra(PushConstants.EXTRA_NOTIFICATION_CONTENT, content);
			context.startActivity(aIntent);
		}
	}
	
	/**
	 * 处理登录结果
	 * 
	 * @param errorCode
	 * @param content
	 */
	private void paraseContent(final Context context, int errorCode, String content) {
		if (errorCode == 0) {
			String appid = "";
			String channelid = "";
			String userid = "";

			try {
				JSONObject jsonContent = new JSONObject(content);
				JSONObject params = jsonContent.getJSONObject("response_params");
				appid = params.getString("appid");
				channelid = params.getString("channel_id");
				userid = params.getString("user_id");
			} catch (JSONException e) {
				Log.e(TAG, "Parse bind json infos error: " + e);
			}
			PhoneBookPreference phonebookPreference = PhoneBookApplication.getInstance().phoneBookPreference;
			phonebookPreference.saveAppId(appid);
			phonebookPreference.saveChannelId(channelid);
			phonebookPreference.saveUserId(userid);
			
			L.i("userId : "+ phonebookPreference.getUserId());
			//T.showLong(context, "userId:"+phonebookPreference.getUserId()+"userName: "+phonebookPreference.getUserName());
		}
	}
	
	
	/**
	 * 解析广播的消息
	 * **/
	private void parseMessage(Message msg) {
		Gson gson = PhoneBookApplication.getInstance().getGson();
		L.i("gson ====" + msg.toString());
		String tag = msg.getTag();
		String userId = msg.getUser_id();
		int headId = msg.getHead_id();
		if (!TextUtils.isEmpty(tag) && (tag.equals(RESPONSE) || tag.equals(PHONEBOOK))) {// 如果是带有tag的消息
			if (userId.equals(PhoneBookApplication.getInstance().getPreference().getUserId()))
				return;
			User u = new User(userId, msg.getChannel_id(), msg.getNick(), headId, 0,"",msg.getAvatar_name());
			PhoneBookApplication.getInstance().getUserDB().addUser(u);// 存入或更新好友
			for (EventHandler handler : ehList)
				handler.onNewFriend(u);
			if (!tag.equals(RESPONSE)) {
				L.i("response start");
				Message item = new Message(System.currentTimeMillis(), "hi", PushMessageReceiver.RESPONSE ,PhoneBookApplication.getInstance().getCurrentUserAvatarName());
				new SendMsgAsyncTask(gson.toJson(item), userId).send();// 同时也回一条消息给对方1
				L.i("response end");
			}
		} else {// 聊天普通消息，
			if (PhoneBookApplication.getInstance().phoneBookPreference.getMsgSound())// 如果用户开启播放声音
				PhoneBookApplication.getInstance().getMediaPlayer().start();
			if (ehList.size() > 0) {// 有监听的时候，传递下去
				for (int i = 0; i < ehList.size(); i++){
					((EventHandler) ehList.get(i)).onMessage(msg);
					L.i("receiver Listener :"+ehList.get(i));
				}
			}  else {
				// 通知栏提醒，保存数据库
				// show notify
				showNotify(msg);
				/*MessageItem item = new MessageItem(MessageItem.MESSAGE_TYPE_TEXT, msg.getNick(), System.currentTimeMillis(), msg.getMessage(), headId, true, 1);
				RecentItem recentItem = new RecentItem(userId, headId, msg.getNick(), msg.getMessage(), 0, System.currentTimeMillis());
				PushApplication.getInstance().getMessageDB().saveMsg(userId, item);
				PushApplication.getInstance().getRecentDB().saveRecent(recentItem);*/
				MessageItem item = new MessageItem(MessageItem.MESSAGE_TYPE_TEXT, msg.getNick(), System.currentTimeMillis(), 
						msg.getMessage(), 0, true, 0 ,msg.getAvatar_name());
				if(msg.getTag().equals(Constant.ONETOONE)){
					PhoneBookApplication.getInstance().getMessageDB().saveMsg(msg.getUser_id(), item);
				}else{
					PhoneBookApplication.getInstance().getMessageDB().saveMsg(msg.getTag(), item);
				}
			}
		}
	}
	
	/**
	 * 消息来临显示通知栏
	 * **/
	@SuppressWarnings("deprecation")
	private void showNotify(Message message) {
		mNewNum++;
		// 更新通知栏
		PhoneBookApplication application = PhoneBookApplication.getInstance();
		CharSequence tickerText = message.getNick() + ":" + message.getMessage();
		/*int icon = R.drawable.phonebook;
		
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.FLAG_NO_CLEAR;
		// 设置默认声音
		// notification.defaults |= Notification.DEFAULT_SOUND;
		// 设定震动(需加VIBRATE权限)
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.contentView = null;*/
		Intent intent = new Intent(application, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(application, 0, intent, 0);
		/*notification.setLatestEventInfo(PhoneBookApplication.getInstance(), application.getPreference().getUserName() + " (" + mNewNum + "条新消息)", tickerText, contentIntent);*/
		
		
		// 下面是4.0通知栏api
		Bitmap bitmap = application.getAvatarByUserInfoExceptMe(getBitMap(message));
		if(bitmap != null){
			bitmap = PhotoUtil.toRoundCorner(bitmap, 15);
		}else{
			bitmap = BitmapFactory.decodeResource(application.getResources(), R.drawable.phonebook);
		}
		String contentTitle = null;
		if(message.getTag().equals(Constant.ONETOONE)){
			contentTitle = message.getNick();
		}else{
			contentTitle = message.getTag();
		}
		Notification.Builder mNotificationBuilder = new Notification.Builder(application)
		        .setTicker(tickerText)
		        .setContentTitle(contentTitle)
				.setContentText(tickerText)
				.setLargeIcon(bitmap)
				.setSmallIcon(R.drawable.phonebook)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(contentIntent);
		Notification n = mNotificationBuilder.getNotification();
		n.flags |= Notification.FLAG_NO_CLEAR;
		//n.defaults |= Notification.DEFAULT_VIBRATE;

		
		application.getNotificationManager().notify(NOTIFY_ID, n);// 通知一下才会生效哦
	}
	
	/**
	 * 根据消息决定通知显示方式
	 * **/
    private String getBitMap(Message msg){
    	String tag = msg.getTag();
    	if(tag.equals(Constant.ONETOONE)){
    		return msg.getAvatar_name();
    	}else{
    		return tag;
    	}
    }
}
