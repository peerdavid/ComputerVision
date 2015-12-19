package tirol.peer.david.computervision;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    /*
     * Link open cv c++ libs
     */
    static {
        // If you use opencv 2.4, System.loadLibrary("opencv_java")
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initOpenActivityBtn(R.id.btnImage, ImageActivity.class);
        initOpenActivityBtn(R.id.btnVideo, VideoActivity.class);
        initOpenActivityBtn(R.id.btnFeature, FeatureActivity.class);
    }


    private void initOpenActivityBtn(int btnId, final Class<?> activity){
        Button btnFeature = (Button) findViewById(btnId);
        btnFeature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getBaseContext(), activity);
                startActivity(i);
            }
        });
    }
}
