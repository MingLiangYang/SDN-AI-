package org.jbh.flowcontroller.impl.defender;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileWriter
{
    private String path = null;
    private BufferedWriter out = null;

    public FileWriter(String path)
    {
        this.path = path;
        try{
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void writeLine(String content)
    {
        try {
            out.write(content + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}