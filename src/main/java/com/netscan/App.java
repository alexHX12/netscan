package com.netscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.netscan.db.DBConnection;
import com.netscan.host.HostInfo;
import com.netscan.host.HostStatus;
import com.netscan.json.JSONData;
import com.netscan.network.NetworkConf;
import com.netscan.network.NetworkScan;

/**
 * App -- corpo del programma
 *
 * @author Alex Della Bruna
 */
public class App {
    private static int nScan=1;
    private static Boolean isRunning = false;
    public static boolean windowsMode=false;//true-->Windows false-->Unix

    /**
     * Esecuzione main launcher e controllo eccezioni
     * 
     * @param args Argomenti linea di comando
     */
    public static void main(String[] args){
        AppArgs appArgs=new AppArgs();
        AppArgs.jCommander=JCommander.newBuilder()
            .addObject(appArgs)
            .build();
        AppArgs.jCommander.setUsageFormatter(appArgs);
        try {
            AppArgs.jCommander.parse(args);
            if(AppArgs.help){
                AppArgs.jCommander.usage();
            }else{
                mainLauncher(args);
            }
        } catch (ParameterException e) {
            AppArgs.jCommander.usage();
        }catch(InvalidParameterException e2){
            System.err.println("Errore nei parametri!");
            System.err.println(e2.getMessage());
        }catch(Exception e3){
            System.err.println("Errore run-time!");
            System.err.println(e3.getMessage());
        }
    }

    /*
    * Verifica OS(Windows/Unix)
    */
    private static void checkOSMode(){
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            App.windowsMode=true;
        }else{
            App.windowsMode=false;
        }
    }

    /**
    * Verifica condizioni iniziali(root) su Windows
    * @return Ritorna true se l'utente possiede i privilegi di amministratore, false altrimenti 
    */
    private static boolean checkWindowsAdminRights(){
        boolean ris=false;
        String groups[] = (new com.sun.security.auth.module.NTSystem()).getGroupIDs();
        for (String group : groups) {
            if (group.equals("S-1-5-32-544")){//Administrator Group Id
                ris=true;
            }
        }
        return ris;
    }

    /**
     * Esecuzione del programma(switch modalità)
     * 
     * @param args Argomenti linea di comando
     * @throws Exception Eccezioni generali
     */
    private static void mainLauncher(String[] args) throws Exception{
        boolean hasAdminRights=false;
        checkOSMode();
        if(App.windowsMode){
            hasAdminRights=checkWindowsAdminRights();
        }else{
            Process p = Runtime.getRuntime().exec("id -u");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int user_id = Integer.parseInt(br.readLine());
            hasAdminRights=user_id==0;
        }
        if (!hasAdminRights) {
            throw new IllegalAccessException("Il programma necessita dei privilegi di "+(App.windowsMode?"amministratore":"root")+" per funzionare correttamente!");
        }
        DBConnection.connect();
        if(args.length!=0){
            cmdMode();
        }else{
            interactiveMode();
        }
    }

    /**
     * Esecuzione del programma in modalità console
     */
    private static void cmdMode(){
        //Controllo ip rete
        if(AppArgs.networkIP.equals("")){
            throw new InvalidParameterException("Nessun IP/sm specificato!");
        }
        Pattern pattern = Pattern.compile(
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])(?:\\.[01]?\\d\\d?|\\.2[0-4]\\d|\\.25[0-5]){3}(?:\\/[0-9]|\\/[1-2]\\d|\\/3[0-2])$");
        Matcher matcher = pattern.matcher(AppArgs.networkIP);
        if(!matcher.matches()){
            throw new InvalidParameterException("IP/sm non valido!");
        }
        NetworkConf net_conf = new NetworkConf(AppArgs.networkIP);
        NetworkScan net_scan = new NetworkScan(net_conf);
        //Controllo operazione
        if(!AppArgs.lastSeen&&!AppArgs.basicScan&&!AppArgs.realTimeScan&&!AppArgs.deleteData){
            AppArgs.basicScan=true;//Se non vengono specificate opzioni presuppone basicScan
        }
        boolean[] arrOp={AppArgs.lastSeen,AppArgs.basicScan,AppArgs.realTimeScan,AppArgs.deleteData};
        boolean oneTrue=false,moreTrue=false;
        for(boolean b:arrOp){
            if(b&&!oneTrue){
                oneTrue=true;
            }else if(b&&oneTrue){
                moreTrue=true;
            }
        }
        if(moreTrue){
            throw new InvalidParameterException("Operazioni non compatibili!");
        }
        if(AppArgs.lastSeen){
            System.out.println("Ultimi host rilevati:");
            ArrayList<HostInfo> lastSeenHost = DBConnection.getLastSeenHost(net_conf);
            for (HostInfo h : lastSeenHost) {
                System.out.println(h);
            }
        }else if(AppArgs.basicScan){
            App.execBasicScan(net_conf, net_scan,"result");
        }else if(AppArgs.realTimeScan){
            App.realTimeScan(net_conf, net_scan);
        }else if(AppArgs.deleteData){
            DBConnection.deleteNetworkData(net_conf);
            System.out.println("Dati della rete eliminati correttamente");
        }
    }

    /**
     * Esecuzione del programma in modalità interattiva
     */
    private static void interactiveMode(){
        Scanner s = new Scanner(System.in);
        String network;
        int sel = 0;
        boolean esci = false;
        Pattern pattern;
        Matcher matcher;
        do {
            clearScreen();
            System.out.print("Benvenuto\nInserisci ip rete nel formato ip/sm\n>");
            pattern = Pattern.compile(
                    "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])(?:\\.[01]?\\d\\d?|\\.2[0-4]\\d|\\.25[0-5]){3}(?:\\/[0-9]|\\/[1-2]\\d|\\/3[0-2])$");
            network = s.nextLine();
            matcher = pattern.matcher(network);
        } while (!matcher.matches());
        NetworkConf net_conf = new NetworkConf(network);
        NetworkScan net_scan = new NetworkScan(net_conf);
        do {
            do {
                clearScreen();
                System.out.print(
                        "---------------------------------------------------------\n" +
                                "|Network Scanner version 1.0\t\t\t\t|\n" +
                                "|-------------------------------------------------------|\n" +
                                "|\t\t\t\t\t\t\t|\n" +
                                "|\tStai operando sulla rete: " + 
                                (net_conf.getSubnetMask() == 0 ? network + " " : network) + "\t|\n" +
                                (net_conf.getSubnetMask() == 0 ? "|\t\t---Single Host Mode---\t\t\t|\n" : "") +
                                "|\t\t\t\t\t\t\t|\n" +
                                "|-------------------------------------------------------|\n" +
                                "|\t\t\t\t\t\t\t|\n" +
                                "|\tScegli operazione:\t\t\t\t|\n" +
                                "|\t\t\t\t\t\t\t|\n" +
                                "|\t1-Visualizza ultimi dispositivi rilevati\t|\n" +
                                "|\t2-Esegui scansione\t\t\t\t|\n" +
                                "|\t3-Esegui scansione real-time\t\t\t|\n" +
                                "|\t4-Elimina dati rete\t\t\t\t|\n" +
                                "|\t5-Altre opzioni\t\t\t\t\t|\n"+
                                "|\t6-Esci\t\t\t\t\t\t|\n" +
                                "|\t\t\t\t\t\t\t|\n" +
                                "---------------------------------------------------------\n" +
                                ">");
                try {
                    sel = Integer.parseInt(s.nextLine());
                } catch (NumberFormatException e) {
                    System.err.println("Errore nell'input!Riprova");
                    sel = 0;
                }
            } while (sel < 1 || sel > 6);
            clearScreen();
            switch (sel) {
                case 1:
                    System.out.println("Ultimi host rilevati:");
                    ArrayList<HostInfo> lastSeenHost = DBConnection.getLastSeenHost(net_conf);
                    for (HostInfo h : lastSeenHost) {
                        System.out.println(h);
                    }
                    break;
                case 2:
                    App.execBasicScan(net_conf, net_scan,"result");
                    break;
                case 3:
                    App.realTimeScan(net_conf, net_scan);
                    break;
                case 4:
                    DBConnection.deleteNetworkData(net_conf);
                    System.out.println("Dati della rete eliminati correttamente");
                    break;
                case 5:
                    String sel2;
                    do{
                        clearScreen();
                        System.out.print(
                            "---------------------------------------------------------\n" +
                                    "|Output JSON:"+(AppArgs.JSONOutputEnable?"Abilitato\t\t\t\t\t":"Non abilitato\t\t\t\t")+"|\n" +
                                    "|-------------------------------------------------------|\n\n"+
                                    "Premi invio per "+(AppArgs.JSONOutputEnable?"disabilitare":"abilitare")+"\n"+
                                    "Premi q per uscire\n"+
                                    ">");
                        sel2=s.nextLine();
                        if(sel2.isEmpty()){
                            AppArgs.JSONOutputEnable=!AppArgs.JSONOutputEnable;
                        }
                    }while(!sel2.equals("q"));
                    break;
                case 6:
                    esci = true;
                    break;
            }
            if (sel == 1 || sel == 2 || sel == 4) {
                System.out.println("Premi q per tornare al menu' principale...");
                while (!s.nextLine().equals("q"));
            }
        } while (!esci);
        s.close();
    }

    /**
     * Esecuzione di scansione di base della rete
     * 
     * @param net_conf Configurazione della rete
     * @param net_scan Istanza di scansione della rete
     */
    public static void execBasicScan(NetworkConf net_conf, NetworkScan net_scan,String jsonOutputFileName) {
        System.out.println("Scansione n° "+nScan+" in corso...");
        ArrayList<HostInfo> hInfo = net_scan.basicScan();
        clearScreen();
        System.out.println("Scansione n° "+nScan+"\nHost attualmente attivi:");
        // Salvataggio DB
        ArrayList<HostInfo> lHost = DBConnection.saveHostList(net_conf, hInfo);
        // Stampa host nuovi
        for (HostInfo h : lHost) {
            if(h.getStatus()==HostStatus.NEW){
                System.out.println("-----Nuovo dispositivo!-----");
            }else if(h.getStatus()==HostStatus.UPDATED){
                System.out.println("-----Dispositivo aggiornato!-----");
            }else if(h.getStatus()==HostStatus.DELETED){
                System.out.println("-----Dispositivo eliminato!-----");
            }
            System.out.println(h);
        }
        if(AppArgs.JSONOutputEnable){
            JSONData.saveHostList(lHost,jsonOutputFileName);
        }
        nScan++;
    }

    /**
     * Esecuzione di scansione real-time della rete(temporizzata finchè l'utente non
     * sceglie di uscire)
     * 
     * @param net_conf Configurazione della rete
     * @param net_scan Istanza di scansione della rete
     */
    public static void realTimeScan(NetworkConf net_conf, NetworkScan net_scan) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                synchronized (isRunning) {
                    if(!isRunning){
                        isRunning=true;
                    }
                    App.execBasicScan(net_conf, net_scan,"result_"+nScan);
                    System.out.println(
                            "La lista dei dispositivi attivi viene aggiornata ogni 2 minuti\nPremi q per uscire\n");    
                    isRunning=false;
                }
            }
        }, 0, 2, TimeUnit.MINUTES);
        Scanner s = new Scanner(System.in);
        while (!s.nextLine().equals("q"));
        scheduledExecutorService.shutdown();
        System.out.println("Terminazione in corso,attendi...");
        synchronized (isRunning) {
            try {
                while(isRunning){
                    Thread.sleep(1000);//Meccanismo wait-notify inconcludente causa loop notify in scheduled function call
                }
            } catch (InterruptedException e) {
                System.err.println("Thread interrotto!");
            }
        }
    }

    private static void clearScreen(){
        try {
            if(App.windowsMode){
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            }else{
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Errore nell'esecuzione del processo!");
        }
    }
}
