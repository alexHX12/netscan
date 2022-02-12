package com.netscan.json;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import com.netscan.host.HostInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nmap4j.data.host.ports.Port;

public class JSONData {
    public static void saveHostList(ArrayList<HostInfo> host,String jsonOutputFileName){
        JSONObject jObj=new JSONObject();
        JSONArray jArr=new JSONArray();
        for(HostInfo h:host){
            JSONObject jHost=new JSONObject();
            jHost.put("os", h.getOS());
            JSONArray jHostArr=new JSONArray();
            jHostArr.put(h.getIPV4());
            jHostArr.put(h.getMAC());
            jHost.put("addresses", jHostArr);
            jHostArr=new JSONArray();
            ArrayList<Port> pList=h.getPorts();
            for(Port p:pList){
                JSONObject jHostPort=new JSONObject();
                jHostPort.put("id", p.getPortId());
                jHostPort.put("protocol", p.getProtocol());
                jHostPort.put("service_name", p.getService().getName());
                jHostArr.put(jHostPort);
            }
            jHost.put("ports", jHostArr);
            jHost.put("status", h.getStatus());
            jArr.put(jHost);
        }
        jObj.put("hosts", jArr);
        try {
            PrintWriter p=new PrintWriter("./"+jsonOutputFileName+".json");
            p.println(jObj.toString(4));
            p.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nell'apertura del file!");
        }
    }
}
