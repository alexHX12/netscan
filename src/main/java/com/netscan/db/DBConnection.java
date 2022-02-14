package com.netscan.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import com.netscan.host.HostInfo;
import com.netscan.host.HostStatus;
import com.netscan.network.NetworkConf;
import org.nmap4j.data.host.Address;
import org.nmap4j.data.host.ports.Port;
import org.nmap4j.data.nmaprun.host.ports.port.Service;

/**
 * DBConnection -- Interazioni col database
 *
 * @author Alex Della Bruna
 */
public class DBConnection {
    public static Connection connection;

    /**
     * Connessione al database
     */
    public static void connect() {
        String url = "jdbc:mysql://localhost:3306/host_detected";
        String username = "root";
        String password = "";
        try {
            DBConnection.connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossibile connettersi al database!", e);
        }
    }

    /**
     * Ottenimento degli host rilevati dall'ultima scansione
     * 
     * @param network Configurazione della rete
     * @return LinkedList<HostInfo> Lista di host
     */
    public static ArrayList<HostInfo> getLastSeenHost(NetworkConf network) {
        ArrayList<HostInfo> lHost = new ArrayList<>();
        try {
            Statement s = connection.createStatement();
            ResultSet res = s.executeQuery("SELECT * FROM host WHERE network_ip='" + network.getIp()
                    + "' AND network_subnet_mask='" + network.getSubnetMask() + "'");
            while (res.next()) {
                ArrayList<Address> a_list = new ArrayList<>();
                Address a_tmp = new Address();
                a_tmp.setAddrtype("ipv4");
                a_tmp.setAddr(res.getString("ipv4"));
                a_list.add(a_tmp);
                a_tmp = new Address();
                a_tmp.setAddrtype("mac");
                a_tmp.setAddr(res.getString("mac"));
                a_list.add(a_tmp);
                Statement s2 = connection.createStatement();
                ResultSet res_p = s2.executeQuery("SELECT * FROM host_port WHERE host_id='" + res.getInt("id") + "'");
                ArrayList<Port> p_list = new ArrayList<>();
                while (res_p.next()) {
                    Port p_tmp = new Port();
                    p_tmp.setPortId(res_p.getInt("port_id"));
                    p_tmp.setProtocol(res_p.getString("port_protocol"));
                    Statement s3 = connection.createStatement();
                    ResultSet res_p_s = s3
                            .executeQuery("SELECT * FROM port WHERE id='" + res_p.getInt("port_id") + "'");
                    res_p_s.next();
                    Service s_tmp = new Service();
                    s_tmp.setName(res_p_s.getString("service_name"));
                    p_tmp.setService(s_tmp);
                    p_list.add(p_tmp);
                }
                lHost.add(
                        new HostInfo(!res.getString("os").equals("null") ? res.getString("os") : null, a_list, p_list,HostStatus.valueOf(res.getString("status"))));
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'esecuzione della query!");
            System.err.println(e.getMessage());
        }
        return lHost;
    }

    /**
     * Salva gli host rilevati nel database e ritorna lista dei nuovi host inseriti
     * 
     * @param network Configurazione della rete
     * @param hInfo   Lista di host
     * @return LinkedList<HostInfo> Lista di host con stato settato
     */
    public static ArrayList<HostInfo> saveHostList(NetworkConf network, ArrayList<HostInfo> hInfo) {
        ArrayList<HostInfo> lHost = new ArrayList<>();
        ArrayList<Integer> lHostId=new ArrayList<>();
        boolean isOneNew=false;
        try {
            Statement s = connection.createStatement();
            // Controllo network
            ResultSet res = s.executeQuery("SELECT * FROM network WHERE ip='" + network.getIp() + "' AND subnet_mask='"
                    + network.getSubnetMask() + "'");
            if (!res.isBeforeFirst()) {// Se il network non esiste
                s.execute("INSERT INTO network VALUES('" + network.getIp() + "'," + network.getSubnetMask() + ")");
            }
            // Controllo dati
            int host_id=0;
            for (HostInfo h : hInfo) {
                res = s.executeQuery("SELECT * FROM host WHERE mac='" + h.getMAC() + "' AND network_ip='"
                        + network.getIp() + "' AND network_subnet_mask='" + network.getSubnetMask() + "'");
                if (!res.isBeforeFirst()) {// Se l'host non esiste
                    h.setStatus(HostStatus.NEW);
                    isOneNew=true;
                    s.execute("INSERT INTO host VALUES(null,'" + h.getOS() + "','" + h.getIPV4() + "','" + h.getMAC()
                                + "','"+h.getStatus()+ "','" + network.getIp() + "'," + network.getSubnetMask() +")");
                    Statement s2=connection.createStatement();
                    ResultSet res2=s2.executeQuery("SELECT id FROM host WHERE os='"+h.getOS()+"' AND ipv4='"+h.getIPV4()+"' AND mac='"+h.getMAC()+"' AND status='"+
                    h.getStatus()+"' AND network_ip='"+network.getIp()+"' AND network_subnet_mask="+network.getSubnetMask());
                    res2.next();
                    host_id=res2.getInt("id");
                    lHostId.add(host_id);
                } else {
                    res.next();
                    host_id=res.getInt("id");
                    //Verifica se l'host corrisponde esattamente
                    res=s.executeQuery("SELECT * FROM host WHERE mac='" + h.getMAC() + "' AND os='"+h.getOS()+"' AND ipv4='"+h.getIPV4()+"' AND network_ip='"
                    + network.getIp() + "' AND network_subnet_mask='" + network.getSubnetMask() + "'");
                    lHostId.add(host_id);
                    if(!res.isBeforeFirst()){//Se non corrisponde esattamente
                        h.setStatus(HostStatus.UPDATED);
                        s.execute("UPDATE host SET os='" + h.getOS() + "',ipv4='" + h.getIPV4() + "',status='"+h.getStatus()+"' WHERE id="
                                + host_id);
                    }else{//Se corrisponde esattamente
                        h.setStatus(HostStatus.NOT_CHANGED);
                        s.execute("UPDATE host SET os='" + h.getOS() + "',ipv4='" + h.getIPV4() + "',status='"+h.getStatus()+"' WHERE id="
                                + host_id);
                    }
                }
                ArrayList<Port> p_tmp = h.getPorts();
                //Mappa temporanea di confronto
                HashMap<Long,String> lPort=new HashMap<>();
                res= s.executeQuery("SELECT port_id,port_protocol FROM host_port WHERE host_id='"+host_id+"'");
                while (res.next()) {
                    lPort.put((long) res.getInt("port_id"), res.getString("port_protocol"));
                }
                // Controllo port
                for (Port p : p_tmp) {
                    res = s.executeQuery("SELECT * FROM port WHERE id='" + p.getPortId() + "' AND protocol='"
                            + p.getProtocol() + "' AND service_name='" + p.getService().getName() + "'");
                    if (!res.isBeforeFirst()) {// Se la porta non esiste
                        s.execute("INSERT INTO port VALUES('" + p.getPortId() + "','" + p.getProtocol() + "','"
                                + p.getService().getName() + "')");
                    }
                }
                // Controllo host_port
                //Elimino vecchie associazioni
                s.execute("DELETE FROM host_port WHERE host_id='" + host_id + "'");
                //Da qui nessuna associazione
                HashMap<Long,String> lPortCurrent=new HashMap<>();
                for (Port p : p_tmp) {
                    lPortCurrent.put(p.getPortId(), p.getProtocol());
                    s.execute("INSERT INTO host_port VALUES('" + host_id + "','" + p.getPortId() + "','"
                            + p.getProtocol() + "')");
                }
                if(h.getStatus()!=HostStatus.NEW&&!lPortCurrent.equals(lPort)){
                    h.setStatus(HostStatus.UPDATED);//Se diverse
                }
                //Aggiunta dell'host
                lHost.add(new HostInfo(h));
            }
            if(lHostId.size()!=0){
                DBConnection.setDeletedHostState(network,lHostId);
            }else if(!isOneNew){
                s.execute("UPDATE host SET status='DELETED' WHERE network_ip='" + network.getIp()
                + "' AND network_subnet_mask=" + network.getSubnetMask());
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'esecuzione della query!");
            System.err.println(e.getMessage());
        }
        return lHost;
    }

    /**
     * Elimina i dati della rete corrente
     * 
     * @param network Configurazione della rete
     */
    public static void deleteNetworkData(NetworkConf network) {
        try {
            Statement s = connection.createStatement();
            // Controllo network
            ResultSet res = s.executeQuery("SELECT id FROM host WHERE network_ip='" + network.getIp()
                    + "' AND network_subnet_mask='" + network.getSubnetMask() + "'");
            if (res.isBeforeFirst()) {// Se l'entry esiste
                Statement s2 = connection.createStatement();
                while (res.next()) {
                    s2.execute("DELETE FROM host_port WHERE host_id='" + res.getInt("id") + "'");
                    s2.execute("DELETE FROM host WHERE id='" + res.getInt("id") + "'");
                }
            }
            s.execute("DELETE FROM network WHERE ip='" + network.getIp() + "' AND subnet_mask='"
                    + network.getSubnetMask() + "'");
        } catch (SQLException e) {
            System.err.println("Errore nell'esecuzione della query!");
            System.err.println(e.getMessage());
        }
    }

    /**
    * Setta lo stato eliminato agli host non rilevati
    * 
    * @param network Configurazione della rete
    * @param lHostId Lista di id degli host rilevati
    */
    public static void setDeletedHostState(NetworkConf network,ArrayList<Integer> lHostId){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<lHostId.size();i++){
            if((i+1)!=lHostId.size()){
                sb.append(Integer.toString(lHostId.get(i))).append(",");
            }else{
                sb.append(Integer.toString(lHostId.get(i)));
            }
        }
        String hostIdStr=sb.toString();
        try {
            Statement s = connection.createStatement();
            ResultSet res = s.executeQuery("SELECT id FROM host WHERE network_ip='" + network.getIp()
                    + "' AND network_subnet_mask='" + network.getSubnetMask() + "' AND id NOT IN ("+hostIdStr+")");
            while(res.next()) {// Se l'entry esiste
                Statement s2 = connection.createStatement();
                s2.execute("UPDATE host SET status='DELETED' WHERE id="+ res.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'esecuzione della query!");
            System.err.println(e.getMessage());
        }
    }
}
