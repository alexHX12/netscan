package com.netscan.network;

import java.io.Serializable;
import com.netscan.utility.ObjectCloner;

/**
 * NetworkConf -- Configurazione della rete
 *
 * @author Alex Della Bruna
 */
public class NetworkConf implements Serializable {
    private String ip;
    private int subnet_mask;

    /**
     * Costruttore base
     * 
     * @param address Indirizzo della rete(ip/subnet mask)
     */
    public NetworkConf(String address) {
        String[] tmp = address.split("/");
        this.ip = tmp[0];
        if (tmp.length != 1) {
            this.subnet_mask = Integer.parseInt(tmp[1]);
        } else {
            this.subnet_mask = 0;
        }
    }

    /**
     * Costruttore ip,subnet mask
     * 
     * @param ip          Ip della rete
     * @param subnet_mask Subnet mask della rete
     */
    public NetworkConf(String ip, int subnet_mask) {
        this(ip + "/" + subnet_mask);
    }

    /**
     * Ritorna ip della rete
     * 
     * @return String Ip della rete
     */
    public String getIp() {
        return (String) ObjectCloner.objDeepCopy(ip);
    }

    /**
     * Ritorna subnet mask della rete
     * 
     * @return int subnet mask della rete
     */
    public int getSubnetMask() {
        return subnet_mask;
    }
}
