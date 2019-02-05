package me.libraryaddict.threaddump;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by libraryaddict on 4/02/2019.
 */
@Getter
public class DumpThread extends Thread {
    private final JavaPlugin plugin;
    private final DumpSettings settings;
    // The last time players were online
    @Setter
    private long lastTimePlayersOnline = 0;
    // The last time the thread dump was created
    @Setter
    private long lastThreadDumpTime = 0;
    @Setter
    private boolean previousCheckFailed = false;

    public DumpThread(JavaPlugin plugin, DumpSettings settings) {
        super("ThreadDump Monitor");

        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public void run() {
        // Sleep for the initial startup delay
        try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(getSettings().getMinutesStartupPeriod()));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Set the last time players were online to now
        setLastTimePlayersOnline(System.currentTimeMillis());

        // Start a new thread, report that monitoring is now enabled
        new Thread("ThreadDump Monitoring Status") {
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getPlugin().getLogger().info("Started monitoring");
                    }
                }.runTask(getPlugin());
            }
        }.start();

        // Run in an infinite loop as we're not calling back to the main thread
        while (true) {
            // Create the task here which will be accessed
            FutureTask<Boolean> task = new FutureTask<>(() -> !Bukkit.getOnlinePlayers().isEmpty());

            // Start a thread incase just running the BukkitRunnable would encounter a freeze
            new Thread("ThreadDump Monitoring Check") {
                public void run() {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Run the task to check if players are online
                            task.run();
                        }
                    }.runTask(getPlugin());
                }
            }.start();

            try {
                // Wait 20 seconds for resolve, otherwise throw error
                boolean playersOnline = task.get(getSettings().getSecondsWaitForBukkit(), TimeUnit.SECONDS);

                doPlayersOnline(playersOnline);
            }
            catch (Exception e) {
                // Error was thrown, assumed due to timeout

                // If it can dump threads
                if (isTimeToDumpAgain()) {
                    // Dump threads
                    System.out.println("[ThreadDump] Server isn't responding, dumping threads!");
                    dumpThreadsToFile();
                }
            }

            try {
                // Wait 10 seconds
                Thread.sleep(TimeUnit.SECONDS.toMillis(getSettings().getSecondsPluginCheck()));
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void doPlayersOnline(boolean playersOnline) {
        // If players are online
        if (playersOnline) {
            // Set the last time players were online to current time, or in future if the number is larger
            setLastTimePlayersOnline(Math.max(System.currentTimeMillis(), getLastTimePlayersOnline()));

            // Server isn't frozen, set to false
            setPreviousCheckFailed(false);
        } else if (getLastTimePlayersOnline() + TimeUnit.MINUTES.toMillis(getSettings().getMinutesNoPlayers()) <
                System.currentTimeMillis()) {
            // If the last time players were online is 2 minutes in the past

            // If it can dump threads
            if (isTimeToDumpAgain()) {
                // Dump threads
                System.out.println(
                        String.format("[ThreadDump] Server has been empty of players for %s minutes, dumping threads!",
                                getSettings().getMinutesNoPlayers()));
                dumpThreadsToFile();
            }
        }
    }

    /**
     * If server hasn't stated it is frozen or it doesn't care, and has never dumped threads before, or 10 minutes
     * has elapsed from the
     * last thread dump
     */
    private boolean isTimeToDumpAgain() {
        return (!getSettings().isSituationChangeOnly() || !isPreviousCheckFailed()) && (getLastThreadDumpTime() == 0 ||
                getLastThreadDumpTime() + TimeUnit.MINUTES.toMillis(getSettings().getMinutesBetweenThreadDumps()) <
                        System.currentTimeMillis());
    }

    private void dumpThreadsToFile() {
        setPreviousCheckFailed(true);

        // Set the last thread dump time
        setLastThreadDumpTime(System.currentTimeMillis());

        // Create the thread dump
        StringBuilder threadDump = createThreadDump();

        // The file the dump will be written to
        File file = getSettings().getLogFile();

        // If parent folders do not exist, create them
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        // Write thread dump to file
        try {
            FileUtils.writeStringToFile(file, threadDump.toString(), "UTF-8");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("[ThreadDump] Thread Dump written to file! Located: " + file.getAbsolutePath());
    }

    /**
     * Code taken from spigotmc
     */
    private StringBuilder createThreadDump() {
        StringBuilder string = new StringBuilder();
        string.append("------------------------------\n");
        //
        string.append("Entire Thread Dump:\n");
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);

        for (ThreadInfo thread : threads) {
            writeThreadToStringBuilder(thread, string);
        }

        string.append("------------------------------\n");

        return string;
    }

    /**
     * Code taken from spigotmc
     */
    private void writeThreadToStringBuilder(ThreadInfo thread, StringBuilder string) {
        string.append("------------------------------\n");
        //
        string.append("Current Thread: " + thread.getThreadName() + "\n");
        string.append("\tPID: " + thread.getThreadId() + " | Suspended: " + thread.isSuspended() + " | Native: " +
                thread.isInNative() + " | State: " + thread.getThreadState() + "\n");
        if (thread.getLockedMonitors().length != 0) {
            string.append("\tThread is waiting on monitor(s):\n");
            for (MonitorInfo monitor : thread.getLockedMonitors()) {
                string.append("\t\tLocked on:" + monitor.getLockedStackFrame() + "\n");
            }
        }
        string.append("\tStack:\n");
        //
        for (StackTraceElement stack : thread.getStackTrace()) {
            string.append("\t\t" + stack + "\n");
        }
    }
}
