import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cedarsoftware.util.io.JsonWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

public class Server {

    private static int port = 9999;
    private static URL pathURL;
    private static List<DomainStats> domainStatsList;
    private static List<String> blackList;
    public static void main(String[] args) throws Exception
    {
        loadBlackList();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port: " + port);
    }

    private static void loadBlackList() throws IOException
    {
        Path filePath = Paths.get("G:\\IntelliJ\\ProxyServer\\src\\main\\resources\\blackList.txt");
        blackList = Files.readAllLines(filePath);
    }

    private static void readStats() {

        String csvFile = "G:\\IntelliJ\\ProxyServer\\src\\main\\resources\\stats.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            domainStatsList.clear();
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] stat = line.split(cvsSplitBy);
                DomainStats domainStats = new DomainStats(stat[0], Long.parseLong(stat[1]), Long.parseLong(stat[2]), Long.parseLong(stat[3]));
                domainStatsList.add(domainStats);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void updateCSV(String host, long inputSize, long outputSize) throws IOException {

        //host, visits++, inputData, outputData
        File inputFile = new File("G:\\IntelliJ\\ProxyServer\\src\\main\\resources\\stats.csv");

        // Read existing file
        CSVReader reader = new CSVReader(new FileReader(inputFile), ',');
        List<String[]> csvBody = reader.readAll();
        boolean exists = false;
        for(String[] s : csvBody)
        {
            if (s[0].equals(host))
            {
                s[1]= String.valueOf(Long.parseLong(s[1])+1);
                s[2]= String.valueOf(Long.parseLong(s[2])+inputSize);
                s[3]= String.valueOf(Long.parseLong(s[3])+outputSize);
                exists=true;
                break;
            }
        }
        if(!exists)
        {
            String[] newElement = new String[4];
            newElement[0] = host;
            newElement[1] = "1";
            newElement[2] = String.valueOf(inputSize);
            newElement[3] = String.valueOf(outputSize);
            csvBody.add(newElement);
        }
        reader.close();

        // Write to CSV file which is open
        CSVWriter writer = new CSVWriter(new FileWriter(inputFile), ',');
        writer.writeAll(csvBody);
        writer.flush();
        writer.close();
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            Headers reqHeaders = exchange.getRequestHeaders();
            try
            {
                String host = reqHeaders.get("Host").get(0);
                if (blackList.contains(host)){
                    exchange.sendResponseHeaders(403, -1);
                    OutputStream os = exchange.getResponseBody();
                    os.close();
                    updateCSV(host, 0, 0);
                }
                else {
                    long inputDataSize;
                    long outputDataSize;
                    String path = exchange.getRequestURI().getPath();
                    pathURL = new URL(exchange.getRequestURI().toString());
                    HttpURLConnection con = (HttpURLConnection) pathURL.openConnection();
                    con.setRequestProperty("Via", "HTTP/1.1");
                    con.setInstanceFollowRedirects(false);
                    con.setDoOutput(true);
                    con.setRequestMethod(exchange.getRequestMethod());
                    for (String key : reqHeaders.keySet()) {
                        if (key != null) {
                            if (reqHeaders.get(key) != null) {
                                con.setRequestProperty(key, reqHeaders.get(key).get(0));
                            }

                        }
                    }

                    byte[] data = new byte[64000];
                    int end;
                    ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream();
                    while ((end = exchange.getRequestBody().read(data, 0, data.length)) != -1) {
                        requestBuffer.write(data, 0, end);
                    }
                    requestBuffer.flush();
                    outputDataSize=requestBuffer.toByteArray().length;
                    con.getOutputStream().write(requestBuffer.toByteArray());
                    con.connect();
                    int responseCode = 404;
                    responseCode = con.getResponseCode();
                    InputStream is =null; byte[] dataResponse = new byte[16384];

                    if (responseCode >= 400) {
                        exchange.sendResponseHeaders(responseCode, -1);
                        OutputStream os = exchange.getResponseBody();
                        os.close();
                    } else {
                        is = con.getInputStream();
                        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                        while((end = is.read(dataResponse,0, dataResponse.length)) != -1){
                            responseBuffer.write(dataResponse,0,end);
                        }
                        responseBuffer.flush();
                        byte[] response = responseBuffer.toByteArray();

                        Map<String, List<String>> myMap = con.getHeaderFields();

                        for (String key : myMap.keySet()) {
                            if (key != null) {
                                if (myMap.get(key) != null) {
                                    exchange.getResponseHeaders().set(key, myMap.get(key).get(0));
                                }
                            }

                        }
                        inputDataSize = response.length;
                        exchange.sendResponseHeaders(responseCode, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                        //host, (visits++), inputData, outputData
                        updateCSV(host, inputDataSize, outputDataSize);
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

}