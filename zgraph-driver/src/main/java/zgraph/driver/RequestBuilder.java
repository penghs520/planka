package zgraph.driver;

import zgraph.driver.proto.request.Request;
import zgraph.driver.utils.NanoID;

public class RequestBuilder {

    /**
     * 创建带有随机请求ID的请求构建器
     *
     * @return 请求构建器
     */
    public static Request.Builder create() {
        return Request.newBuilder()
                .setRequestId(NanoID.random(10));
    }
}
