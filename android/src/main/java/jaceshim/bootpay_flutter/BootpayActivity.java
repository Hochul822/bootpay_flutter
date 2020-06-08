package jaceshim.bootpay_flutter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import jaceshim.bootpay_flutter.models.PayParam;
import kr.co.bootpay.Bootpay;
import kr.co.bootpay.BootpayAnalytics;
import kr.co.bootpay.BootpayBuilder;
import kr.co.bootpay.BootpayFlutterActivity;
import kr.co.bootpay.enums.Method;
import kr.co.bootpay.listener.CancelListener;
import kr.co.bootpay.listener.CloseListener;
import kr.co.bootpay.listener.ConfirmListener;
import kr.co.bootpay.listener.DoneListener;
import kr.co.bootpay.listener.ErrorListener;
import kr.co.bootpay.listener.ReadyListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jaceshim.bootpay_flutter.Constans.PAY_PARAM_KEY;
import static jaceshim.bootpay_flutter.Constans.PAY_RESULT_DATA_KEY;

/**
 * 결제화면 Activity
 *
 * @author jaceshim
 */
public class BootpayActivity extends BootpayFlutterActivity {
    //private final String TAG = this.getClass().getSimpleName();
    private final String TAG = "bootpay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "payParam in intent : " + this.getIntent().getStringExtra(PAY_PARAM_KEY));
        final PayParam params = new Gson().fromJson(this.getIntent().getStringExtra(PAY_PARAM_KEY), PayParam.class);
        Log.d(TAG, "payParam : " + params.toString());

        bootpayRequest(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

     @Override
     public void onBackPressed() {
         Map<String, String> data = new HashMap<>();
         // 결제완료전 back버튼 터치시 사용자 결제취소로 처리.
         data.put("action", "BootpayCancel");
         bindResult(new Gson().toJson(data));
         // back버튼 터치시 결제를 중지 상태로 처리한다. 밑에 super.onBackPressed() 결과값 설정보다 먼저 호출하면 결과값 설정이 안되니 주의요망!
         super.onBackPressed();
     }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    private void bootpayRequest(PayParam params) {
        try {

            String bootpayId = params.getApplicationId();
            Log.d(TAG, "bootpayId : " + bootpayId);
            BootpayAnalytics.init(this, bootpayId);

            Log.d(TAG, "Bootpay 결제요청 파라미터 : " + params);
            Log.d(TAG, "Bootpay 결제요청 파라미터 : " + params.getPrice());
            Log.d(TAG, "Bootpay method 1 : " + params.getMethod());
            Log.d(TAG, "Bootpay method 2 : " + getPaymentMethod(params.getMethod()));


            int paymentAmount = Integer.parseInt(Objects.requireNonNull(params.getPrice()));

            List<String> methods = new ArrayList<String>();

            final BootpayBuilder bootpayBuilder = Bootpay.init(this)
                    .setContext(this)
                    .setApplicationId(bootpayId) // 해당 프로젝트(안드로이드)의 application id 값
                    .setPG(params.getPg()) // 결제할 PG 사
                    // .setMethod(getPaymentMethod(params.getMethod())) // 결제수단
                    .setMethod(Method.BANK) // 결제수단
                    // .setMethods(methods)
                    .setName(params.getName()) // 결제할 상품명
                    .setOrderId(params.getOrderId()) // 결제 고유번호
                    .setPrice(paymentAmount) // 결제할 금액
                    .setItems(params.getItems())
                    .setBootUser(params.getUserInfo())
                    .setBootExtra(params.getExtra());

            // .setParams(params.getParams())
            // .isShowAgree(params.isShowAgreeWindow());

            final String accountExpireAt = params.getAccountExpireAt();

            if (accountExpireAt != null && !accountExpireAt.isEmpty()) {
                bootpayBuilder.setAccountExpireAt(accountExpireAt);
            }


            Log.i("bootpay", "start bootpay");


            // 결제완료시 호출, 아이템 지급 등 데이터 동기화 로직을 수행합니다
            bootpayBuilder.onConfirm(this)
                    .onDone(message -> Log.d("bootpay", "done " + message))
                    .onReady(message -> Log.d("bootpay", "ready " + message))
                    .onCancel(message -> Log.d("bootpay", "cancel " + message))
                    .onError(this)
                    .onClose(message -> closeBootpay(message))
                    .request();

            Log.i("bootpay", "start bootpay after");

        } catch (Exception e) {
            Log.d("bootpay  error", e.getMessage());
            bindResult(e.getMessage());
        }

    }

    private Method getPaymentMethod(String paymentMethod) {
        Log.d(TAG, "Bootpay 결제요청 method - str : " + paymentMethod);
        Log.d(TAG, "Bootpay 결제요청 method - enum upper: " + Method.valueOf(paymentMethod.toUpperCase()));

        // return Method.PHONE;

        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return Method.SELECT;
        }
//        return Method.PHONE;

        return Method.valueOf(paymentMethod.toUpperCase());
    }

    @Override
    public void onError(String message) {
        Log.i("bootpay", "on error " + message);
        bindResult(message);
        this.finish();
    }

    @Override
    public void onCancel(String message) {
        Log.i("bootpay", "on cacnel " + message);
        bindResult(message);
        this.finish();
    }

    @Override
    public void onClose(String message) {
        Log.i("bootpay", "on close " + message);
        this.finish();
    }

    void closeBootpay(String message) {
        Log.i("bootpay", "on close " + message);
        this.finish();
    }

    @Override
    public void onReady(String message) {
        Log.i("bootpay", "on ready " + message);
    }

    @Override
    public void onConfirm(String message) {
        Log.i("bootpay", "on confirm " + message);
        Bootpay.confirm(message);
    }

    @Override
    public void onDone(String message) {
        Log.i("bootpay", "on done " + message);
        bindResult(message);
        this.finish();
    }

    void bindResult(String message) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(PAY_RESULT_DATA_KEY, message);

        // result code는 의미 없음
        setResult(900, resultIntent);
        this.finish();
    }
}