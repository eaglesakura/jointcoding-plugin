package como.eaglesakura.helloworld;

import com.eaglesakura.jc.annotation.JCClass;
import com.eaglesakura.jc.annotation.JCMethod;

@JCClass(cppNamespace = "example")
public interface ExampleProtocol {

    @JCMethod
    byte byteMethod();

    @JCMethod
    short shortMethod();

    @JCMethod
    int intMethod();

    @JCMethod
    long longMethod();

    @JCMethod
    String stringMethod();
}
