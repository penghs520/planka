package planka.graph.driver;

import planka.graph.driver.proto.request.Request;
import planka.graph.driver.utils.NanoID;

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
