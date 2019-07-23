package zhijianglab;


import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.Base64;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;



public class HttpRequest
{
    public String doGet(String url, String uname, String pwd)throws IOException
    {
        //System.out.println(url);
        String result = "";

        BufferedReader in = null;

        String usrinfo = uname + ":" + pwd;

        //System.out.println(url);

        URL realurl = new URL(url);

        HttpURLConnection conn = (HttpURLConnection)realurl.openConnection();

        String encodeedpwd = Base64.getEncoder().encodeToString((usrinfo).getBytes());

        conn.setRequestProperty("Authorization", "Basic " + encodeedpwd);
        conn.setConnectTimeout(6000*10);
        conn.setReadTimeout(6000*10);
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        //System.out.println(conn.getResponseCode());



        in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));


        String line;
        while ((line = in.readLine()) != null)
        {
            result += line;
        }


        if (in!=null)
        {
            in.close();
        }

        //System.out.println(result);

        return result;
    }

    public String doPut(String url, String uname, String pwd, String params)throws IOException
    {
        System.out.println(url);
        URL realurl = new URL(url);
        StringBuffer sbuffer = null;
        HttpURLConnection conn = (HttpURLConnection)realurl.openConnection();

        String usrinfo = uname + ":" + pwd;

        String encodeedpwd = Base64.getEncoder().encodeToString((usrinfo).getBytes());

        conn.setRequestProperty("Authorization", "Basic " + encodeedpwd);
        conn.setConnectTimeout(6000*10);
        conn.setReadTimeout(6000*10);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", " application/json");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Content-Length", params.getBytes().length + "");

        conn.connect();

        OutputStream out = conn.getOutputStream();

        out.write(params.getBytes());
        out.flush();
        out.close();

        if (conn.getResponseCode() == 200)
        {
            InputStreamReader inputStream =new InputStreamReader(conn.getInputStream());
            BufferedReader reader = new BufferedReader(inputStream);
            String lines;
            sbuffer= new StringBuffer("");
            while ((lines = reader.readLine()) != null)
            {
                lines = new String(lines.getBytes(), "utf-8");
                sbuffer.append(lines);
            }
            reader.close();
        }
        else
        {
            System.out.println("fail conn " + conn.getResponseCode());
        }
        conn.disconnect();
        return sbuffer.toString();
    }

}
