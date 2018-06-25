import com.sun.jndi.toolkit.url.Uri;
import com.sun.net.httpserver.*;
import org.omg.CORBA.Request;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServerTest {
    public static void main(String[] args) throws Exception {
        int port = 9999;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static String nonRewritableHeaders[] = { "Transfer-Encoding"};
    static List<String> nonRewritableHeadersList = Arrays.asList(nonRewritableHeaders);

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                URL url = new URL(exchange.getRequestURI().toString());

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(exchange.getRequestMethod());
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);
                exchange.getRequestHeaders().forEach((k, v) -> {
                    if(!nonRewritableHeadersList.contains(k)) {
                        connection.setRequestProperty(k, String.join(",", v));
                    }
                });
                connection.setRequestProperty("Via","HTTP/1.1");

                byte[] data = new byte[16384];  int end;
                ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream();
                while((end = exchange.getRequestBody().read(data,0,data.length)) != -1){
                    requestBuffer.write(data,0,end);
                }
                requestBuffer.flush();
                connection.getOutputStream().write(requestBuffer.toByteArray());
                connection.connect();

                //

                InputStream is =null; byte[] dataResponse = new byte[16384];
                if(connection.getResponseCode() >= 400){
                    is = connection.getErrorStream();
                }else{
                    is = connection.getInputStream();
                }
                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                while((end = is.read(dataResponse,0, dataResponse.length)) != -1){
                    responseBuffer.write(dataResponse,0,end);
                }
                responseBuffer.flush();
                byte[] response = responseBuffer.toByteArray();

                connection.getHeaderFields().forEach((k, v) -> {
                    if(k != null && !nonRewritableHeadersList.contains(k)) {
                        exchange.getResponseHeaders().set(k, String.join(",", v));
                    }
                });
                int code = connection.getResponseCode();
                exchange.sendResponseHeaders(connection.getResponseCode(), response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }


}

