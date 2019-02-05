package me.libraryaddict.threaddump;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by libraryaddict on 4/02/2019.
 */
public class DumpPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Init settings
        DumpSettings settings = new DumpSettings(getConfig());

        // Create ThreadDump thread and start
        new DumpThread(this, settings).start();

        getLogger().info(String.format("Monitoring will begin in %s minutes", settings.getMinutesStartupPeriod()));
    }
}
