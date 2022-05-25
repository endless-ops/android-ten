package cn.dreamchase.android.ten.wxapi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.greenrobot.eventbus.EventBus;

import cn.dreamchase.android.ten.util.Constant;
import cn.dreamchase.android.ten.entry.WeiXin;

/**
 * -用来接收登录授权以及分享时微信的回调信息
 */
public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {
    private IWXAPI iwxapi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iwxapi = WXAPIFactory.createWXAPI(this, Constant.WECHAT_APPID,true);
        iwxapi.registerApp(Constant.WECHAT_APPID);
        iwxapi.handleIntent(getIntent(),this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        iwxapi.handleIntent(getIntent(),this);
        Log.i("ansen","WXEntryActivity onNewIntent");
    }

    @Override
    public void onReq(BaseReq baseReq) {
        Log.i("ansen","WXEntryActivity onReq");
    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX) { //分享
            WeiXin weiXin = new WeiXin(2,baseResp.errCode,"");
            EventBus.getDefault().post(weiXin);
        }else if (baseResp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            // 登录
            SendAuth.Resp authResp = (SendAuth.Resp) baseResp;
            WeiXin weiXin = new WeiXin(1,baseResp.errCode,authResp.code);
            EventBus.getDefault().post(weiXin);
        }
        finish();
    }
}
