package com.netscan.host;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import com.netscan.utility.ObjectCloner;
import org.nmap4j.data.host.Address;
import org.nmap4j.data.host.ports.Port;
import org.nmap4j.data.nmaprun.host.ports.port.Service;
import org.nmap4j.data.nmaprun.host.ports.port.State;

/**
 * HostInfo -- Descrizione degli host
 *
 * @author Alex Della Bruna
 */
public class HostInfo {
    private String os;
    private ArrayList<Address> addresses = new ArrayList<>();
    private ArrayList<Port> ports = new ArrayList<>();
    private HostStatus status;

    /**
     * Costruttore base
     * 
     * @param os        Os
     * @param addresses Lista di indirizzi
     * @param ports     Lista di porte
     * @param status    Stato della rilevazione
     */
    public HostInfo(String os, ArrayList<Address> addresses, ArrayList<Port> ports, HostStatus status) {
        this.os = (os != null ? (String) ObjectCloner.objDeepCopy(os) : os);
        for (Address a : addresses) {
            this.addresses.add(a);
        }
        addLocalHostMac();
        for (Port p : ports) {
            this.ports.add(p);
        }
        this.status = (HostStatus) ObjectCloner.objDeepCopy(status);
    }

    /**
     * Costruttore per copia
     * 
     * @param h Istanza di HostInfo
     * @implNote Address e Port non supportano serializzazione(metodo lento)
     */
    public HostInfo(HostInfo h) {
        this.os = (h.os != null ? (String) ObjectCloner.objDeepCopy(h.os) : h.os);
        for (Address a : h.addresses) {
            Address a_tmp=new Address();
            a_tmp.setAddr(a.getAddr()==null?null:(String) ObjectCloner.objDeepCopy(a.getAddr()));
            a_tmp.setAddrtype(a.getAddrtype()==null?null:(String) ObjectCloner.objDeepCopy(a.getAddrtype()));
            a_tmp.setVendor(a.getVendor()==null?null:(String) ObjectCloner.objDeepCopy(a.getVendor()));
            this.addresses.add(a_tmp);
        }
        addLocalHostMac();
        for (Port p : h.ports) {
            Port p_tmp=new Port();
            p_tmp.setPortId(p.getPortId());
            p_tmp.setProtocol((String) ObjectCloner.objDeepCopy(p.getProtocol()));
            Service s_tmp=new Service();
            s_tmp.setConf(p.getService().getConf()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getConf()));
            s_tmp.setExtrainfo(p.getService().getExtrainfo()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getExtrainfo()));
            s_tmp.setMethod(p.getService().getMethod()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getMethod()));
            s_tmp.setName(p.getService().getName()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getName()));
            s_tmp.setOsType(p.getService().getOsType()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getOsType()));
            s_tmp.setProduct(p.getService().getProduct()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getProduct()));
            s_tmp.setVersion(p.getService().getVersion()==null?null:(String) ObjectCloner.objDeepCopy(p.getService().getVersion()));
            p_tmp.setService(s_tmp);
            State st_tmp=new State();
            st_tmp.setReason(p.getState().getReason()==null?null:(String)ObjectCloner.objDeepCopy(p.getState().getReason()));
            st_tmp.setReason_ttl(p.getState().getReason_ttl());
            st_tmp.setState((String) ObjectCloner.objDeepCopy(p.getState().getState()));
            p_tmp.setState(st_tmp);
            this.ports.add(p_tmp);
        }
        this.status = (HostStatus) ObjectCloner.objDeepCopy(h.status);
    }

    private void addLocalHostMac(){
        if (getMAC() == null) {
            try {
                //Controllo se l'ip corrisponde al launcher(aggiunta MAC mancante)
                if (getIPV4().equals(InetAddress.getLocalHost().getHostAddress())) {
                    Address a_tmp = new Address();
                    byte[] a_mac_tmp;
                    String macAddress=null;
                    try {
                        a_mac_tmp = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
                        String[] hex = new String[a_mac_tmp.length];
                        for (int i = 0; i < a_mac_tmp.length; i++) {
                            hex[i] = String.format("%02X", a_mac_tmp[i]);//Formatta con almeno 2 cifre 0 se non sono abbastanza hex
                        }
                        macAddress = String.join(":", hex);
                    } catch (SocketException e) {
                        System.err.println("Errore nelle operazioni socket!");
                    }
                    a_tmp.setAddrtype("mac");
                    a_tmp.setAddr(macAddress);
                    this.addresses.add(a_tmp);
                }
            } catch (UnknownHostException e) {
                System.err.println("Host sconosciuto!");
            }
        }
    }

    /**
     * Ritorna Os
     * 
     * @return String os
     */
    public String getOS() {
        return os != null ? (String) ObjectCloner.objDeepCopy(os) : os;
    }

    /**
     * Ritorna l'indirizzo del tipo richiesto
     * 
     * @param address_type Tipo di indirizzo
     * @return String Indirizzo del tipo richiesto
     */
    private String getAddress(String address_type) {
        String res = null;
        for (Address a : addresses) {
            if (a.getAddrtype().equals(address_type)) {
                res = a.getAddr();
            }
        }
        return res != null ? (String) ObjectCloner.objDeepCopy(res) : res;
    }

    /**
     * Ritorna indirizzo IPV4
     * 
     * @return String Indirizzo IPV4
     */
    public String getIPV4() {
        return getAddress("ipv4");
    }

    /**
     * Ritorna indirizzo MAC
     * 
     * @return String indirizzo MAC
     */
    public String getMAC() {
        return getAddress("mac");
    }

    /**
     * Ritorna lista di porte
     * 
     * @return ArrayList<Port> Lista di porte
     */
    public ArrayList<Port> getPorts() {
        ArrayList<Port> res = new ArrayList<>();
        for (Port p : ports) {
            res.add(p);
        }
        return res;
    }

    /**
     * Ritorna stato della rilevazione
     * 
     * @return HostStatus stato della rilevazione
     */
    public HostStatus getStatus() {
        return (HostStatus) ObjectCloner.objDeepCopy(status);
    }

    /**
     * Setta lo stato della rilevazione
     */
    public void setStatus(HostStatus status) {
        this.status = (HostStatus) ObjectCloner.objDeepCopy(status);
    }

    /**
     * Verifica eguaglianza tra due istanze
     * 
     * @param h Altra istanza di HostInfo
     * @return boolean Vero se le istanze sono eguali falso altrimenti
     */
    public boolean equals(HostInfo h) {
        boolean ris = true;
        if (this.os != h.os || this.addresses.size() != h.addresses.size()) {
            ris = false;
        } else {
            for (int i = 0; i < h.addresses.size(); i++) {
                if (!this.addresses.get(i).getAddrtype().equals(h.addresses.get(i).getAddrtype())
                        || !this.addresses.get(i).getAddr().equals(h.addresses.get(i).getAddr())) {
                    ris = false;
                }
            }
        }
        return ris;
    }

    /**
     * Ritorna stringa descrittiva dell'istanza
     * 
     * @return String Stringa di descrizione dell'istanza
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("-------------------------------------\n");
        s.append("OS:\t").append(this.os != null ? this.os : "Non definito").append("\n");
        s.append("IPV4:\t").append(getIPV4()).append("\n");
        s.append("MAC:\t").append(getMAC()).append("\n");
        s.append("\nPorte aperte:\n");
        if (ports.size() == 0) {
            s.append("Nessuna porta aperta!\n");
        } else {
            for (Port p : ports) {
                s.append(p.getPortId()).append("\t").append(p.getProtocol()).append("\t")
                        .append(p.getService().getName()).append("\n");
            }
        }
        s.append("-------------------------------------\n");
        return s.toString();
    }
}
