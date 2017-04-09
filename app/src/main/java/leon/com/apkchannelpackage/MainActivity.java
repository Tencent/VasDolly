package leon.com.apkchannelpackage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.leon.channel.helper.ChannelReaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    TextView mTextView;
g
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.channel);
        String channel = ChannelReaderUtil.getChannel(getApplicationContext());
        mTextView.setText(channel);

        byte buffer[] = "ltlovezh".getBytes();
        for (int i = 0; i < buffer.length; i++) {
            Log.i(TAG, Integer.toHexString(Integer.valueOf(Byte.valueOf(buffer[i]).toString())));
        }

        Log.i(TAG, "------");

        ByteBuffer byteBuffer = ByteBuffer.wrap("ltlovezh".getBytes());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte[] value = byteBuffer.array();
        for (int i = 0; i < value.length; i++) {
            Log.i(TAG, Integer.toHexString(Integer.valueOf(Byte.valueOf(value[i]).toString())));
        }
        Log.i(TAG, "------");

        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        value = byteBuffer.array();
        for (int i = 0; i < value.length; i++) {
            Log.i(TAG, Integer.toHexString(Integer.valueOf(Byte.valueOf(value[i]).toString())));
        }


    }
}
