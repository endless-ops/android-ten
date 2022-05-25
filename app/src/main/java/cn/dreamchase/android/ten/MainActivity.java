package cn.dreamchase.android.ten;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayOutputStream;

import cn.dreamchase.android.ten.util.Constant;
import cn.dreamchase.android.ten.entry.WeiXin;
import cn.dreamchase.android.ten.entry.WeiXinPay;
import cn.dreamchase.android.ten.entry.WeiXinToken;
import cn.dreamchase.android.ten.entry.WeiXinUserInfo;
import cn.dreamchase.android.ten.net.HTTPCaller;
import cn.dreamchase.android.ten.net.RequestDataCallback;

/**
 * -微信登录、分享与支付
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private IWXAPI iwxapi;

    private TextView nickName;
    private TextView age;

    private static final int IMAGE_SIZE = 32768; // 微信分享图片大小限制


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this); // 注册
        iwxapi = WXAPIFactory.createWXAPI(this, Constant.WECHAT_APPID, true);
        iwxapi.registerApp(Constant.WECHAT_APPID);

        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_share_friend_circle).setOnClickListener(this);
        findViewById(R.id.btn_share_friend).setOnClickListener(this);
        findViewById(R.id.btn_pay).setOnClickListener(this);

        nickName = findViewById(R.id.tv_nickname);
        age = findViewById(R.id.tv_age);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                // 微信登录
                login();
                break;
            case R.id.btn_share_friend_circle:
                // 微信分享到朋友圈
                share(true);
                break;
            case R.id.btn_share_friend:
                // 分享给好友
                share(false);
                break;
            case R.id.btn_pay:
                // 先去服务器获取支付信息，返回一个WeiXinPay对象，然后调用Pay方法
                showToast("微信支付需要服务器支持");
                break;
        }
    }

    @Subscribe
    public void onEventMainThread(WeiXin weiXin) {
        Log.i("ansen", "收到eventbus请求type：" + weiXin.getType());

        if (weiXin.getType() == 1) {
            getAccessToken(weiXin.getCode());
        } else if (weiXin.getType() == 2) {
            switch (weiXin.getErrCode()) {
                case BaseResp
                        .ErrCode.ERR_OK:
                    Log.i("ansen", "微信登录成功。。。。");
                    break;

                case BaseResp
                        .ErrCode.ERR_USER_CANCEL:
                    Log.i("ansen", "微信分享取消。。。。");
                    break;

                case BaseResp
                        .ErrCode.ERR_AUTH_DENIED:
                    Log.i("ansen", "微信分享拒绝。。。。");
                    break;
            }
        } else if (weiXin.getType() == 3) {
            if (weiXin.getErrCode() == BaseResp.ErrCode.ERR_OK) {
                Log.i("ansen", "微信支付成功....");
            }
        }
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this); // 取消注册
    }

    /**
     * -微信登录
     */
    public void login() {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = String.valueOf(System.currentTimeMillis());
        iwxapi.sendReq(req);
    }

    public void getAccessToken(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + Constant.WECHAT_APPID
                + "&secret=" + Constant.WECHAT_SECRET + "&code=" + code
                + "&grant_type=authorization_code";

        HTTPCaller.getInstance().get(WeiXinToken.class, url, null,
                new RequestDataCallback<WeiXinToken>() {
                    @Override
                    public void dataCallback(WeiXinToken obj) {
                        if (obj.getErrcode() == 0) {
                            // 请求成功
                            getWeiXinUserInfo(obj);
                        } else {
                            // 请求失败
                            showToast(obj.getErrmsg());
                        }
                    }
                });
    }


    public void getWeiXinUserInfo(WeiXinToken weiXinToken) {
        String url = "https://api.weixin.qq.com/sns/userinfo?access_token=" + weiXinToken.getAccess_token()
                + "&openid=" + weiXinToken.getOpenid();

        HTTPCaller.getInstance().get(WeiXinUserInfo.class, url, null, new RequestDataCallback<WeiXinUserInfo>() {
            @Override
            public void dataCallback(WeiXinUserInfo obj) {

                nickName.setText("昵称:" + obj.getNickname());
                age.setText("年龄:" + obj.getAge());
            }
        });
    }


    public void share(boolean friendsCircle) {
        WXWebpageObject webpageObject = new WXWebpageObject();
        webpageObject.webpageUrl = "www.baidu.com";
        WXMediaMessage msg = new WXMediaMessage(webpageObject);
        msg.title = "分享标题";
        msg.description = "分享描述";
        msg.thumbData = getThumbData(); // 封面图片byte数组

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = friendsCircle ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    /**
     * 获取分享封面byte数组 我们这边取的是软件启动icon
     *
     * @return
     */
    public byte[] getThumbData() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher, options);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        int quality = 100;
        while (output.toByteArray().length > IMAGE_SIZE && quality != 10) {
            output.reset(); // 清空baos
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);// 这里压缩options%，把压缩后的数据存放到baos中
            quality -= 10;
        }
        bitmap.recycle();
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    public void pay(WeiXinPay weiXinPay) {
        PayReq payReq = new PayReq();

        payReq.appId = Constant.WECHAT_APPID;
        payReq.nonceStr = weiXinPay.getNoncestr();
        payReq.packageValue = weiXinPay.getPackage_value();
        payReq.sign = weiXinPay.getSign();
        payReq.partnerId = weiXinPay.getPartnerid();
        payReq.prepayId = weiXinPay.getPrepayid();
        payReq.timeStamp = weiXinPay.getTimestamp();

        iwxapi.registerApp(Constant.WECHAT_APPID);
        iwxapi.sendReq(payReq);

    }


}