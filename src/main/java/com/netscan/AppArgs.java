package com.netscan;

import java.util.List;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.internal.Lists;

public class AppArgs implements IUsageFormatter {
    public static JCommander jCommander;

    @Parameter(names = { "-n", "--network-ip" }, description = "Network ip/sm[if not specified go to interactive mode]")
    public static String networkIP = "";

    @Parameter(names = { "-l", "--last-seen" }, description = "Show last seen hosts(previous scan)")
    public static boolean lastSeen = false;

    @Parameter(names = { "-b","--basic-scan" }, description = "Basic network scan[default if no operation where specified]")
    public static boolean basicScan = false;

    @Parameter(names = { "-r", "--real-time-scan" }, description = "Real time network scan")
    public static boolean realTimeScan = false;

    @Parameter(names = { "-d", "--delete-data" }, description = "Delete network data")
    public static boolean deleteData = false;

    @Parameter(names = { "--json" }, description = "Enable JSON output")
    public static boolean JSONOutputEnable = false;

    @Parameter(names = { "-h", "--help" }, description = "Show usage info", help = true)
    public static boolean help;

    @Override
    public void usage(StringBuilder out) {
        if (jCommander.getDescriptions() == null) {
            jCommander.createDescriptions();
        }
        // Create a list of the parameters
        List<ParameterDescription> params = Lists.newArrayList();
        params.addAll(jCommander.getFields().values());
        params.sort(jCommander.getParameterDescriptionComparator());
        // Append all the parameter names
        if (params.size() > 0) {
            out.append(
                    "Usage: netscan.jar --network-ip <ip/sm> { --last-seen | --basic-scan | --real-time-scan | --delete-data } [ --json ]\n");
            out.append("Options:\n");
            for (ParameterDescription pd : params) {
                out.append(pd.getNames()).append("\n").append(pd.getDescription()).append("\n\n");
            }
        }
    }

    @Override
    public void usage(StringBuilder out, String indent) {
    }

    @Override
    public void usage(String commandName) {
    }

    @Override
    public void usage(String commandName, StringBuilder out) {
    }

    @Override
    public void usage(String commandName, StringBuilder out, String indent) {
    }

    @Override
    public String getCommandDescription(String commandName) {
        return null;
    }
}
