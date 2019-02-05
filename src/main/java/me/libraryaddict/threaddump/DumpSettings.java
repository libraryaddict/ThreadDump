package me.libraryaddict.threaddump;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Created by libraryaddict on 5/02/2019.
 */
@Getter
public class DumpSettings {
    private String fileLocation;
    private boolean situationChangeOnly;
    private int minutesNoPlayers;
    private int minutesBetweenThreadDumps;
    private int secondsPluginCheck;
    private int secondsWaitForBukkit;
    private int minutesStartupPeriod;

    public DumpSettings(FileConfiguration configuration) {
        situationChangeOnly = configuration.getBoolean("SituationChangeOnly");
        minutesNoPlayers = configuration.getInt("MinutesNoPlayers");
        minutesBetweenThreadDumps = configuration.getInt("MinutesBetweenThreadDumps");
        secondsPluginCheck = configuration.getInt("SecondsBetweenChecks");
        secondsWaitForBukkit = configuration.getInt("SecondsWaitForBukkit");
        fileLocation = configuration.getString("DumpFileLocation");
        minutesStartupPeriod = configuration.getInt("MinutesStartupPeriod");
    }

    public File getLogFile() {
        return new File(fileLocation);
    }
}
