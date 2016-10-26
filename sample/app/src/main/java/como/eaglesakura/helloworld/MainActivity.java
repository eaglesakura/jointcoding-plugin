package como.eaglesakura.helloworld;

import com.eaglesakura.jc.annotation.JCClass;
import com.eaglesakura.jc.annotation.JCMethod;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

@JCClass(cppNamespace = "example")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @JCMethod
    public String hello() {
        return "Hello World";
    }
}
