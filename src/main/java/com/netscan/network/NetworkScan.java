package com.netscan.network;

import java.io.Serializable;
import java.util.ArrayList;

import com.netscan.App;
import com.netscan.host.HostInfo;
import com.netscan.host.HostStatus;
import com.netscan.utility.ObjectCloner;
import org.nmap4j.Nmap4j;
import org.nmap4j.core.nmap.NMapExecutionException;
import org.nmap4j.core.nmap.NMapInitializationException;
import org.nmap4j.data.nmaprun.Host;

/**
 * NetworkScan -- Scansione della rete
 *
 * @author Alex Della Bruna
 */
public class NetworkScan implements Serializable {
    private NetworkConf net_conf;

    /**
     * Costruttore base
     * 
     * @param net_conf Configurazione della rete
     */
    public NetworkScan(NetworkConf net_conf) {
        this.net_conf = (NetworkConf) ObjectCloner.objDeepCopy(net_conf);
    }

    /**
     * Scansione base della rete(rilevamento degli host 100 porte piu' frequenti per
     * protocollo)
     * 
     * @return LinkedList<HostInfo> Lista di host rilevati
     */
    public ArrayList<HostInfo> basicScan() {
        Nmap4j nmap = new Nmap4j(App.windowsMode?"C:\\Program Files (x86)\\Nmap":"/usr");
        nmap.includeHosts(
                net_conf.getSubnetMask() != 0 ? net_conf.getIp() + "/" + net_conf.getSubnetMask() : net_conf.getIp());
        nmap.addFlags("-O -F");
        try {
            nmap.execute();
        } catch (NMapInitializationException | NMapExecutionException e) {
            System.err.println("Errore nel avvio di Nmap!");
        }
        ArrayList<HostInfo> hInfo = new ArrayList<>();
        if (!nmap.hasError()) {
            for (int i = 0; i < nmap.getResult().getHosts().size(); i++) {
                String os = null;
                Host h = nmap.getResult().getHosts().get(i);
                if (h.getOs().getOsMatches().size() != 0) {
                    os = h.getOs().getOsMatches().get(0).getName();
                }
                hInfo.add(new HostInfo(os, h.getAddresses(), h.getPorts().getPorts(),HostStatus.UNKNOWN));
            }
        } else {
            System.err.println("Error:" + nmap.getExecutionResults().getErrors());
        }
        return hInfo;
    }
}
