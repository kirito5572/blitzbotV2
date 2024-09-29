package me.kirito5572.commands.main;

import com.sun.management.OperatingSystemMXBean;
import me.duncte123.botcommons.messaging.EmbedUtils;
import me.kirito5572.App;
import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.util.Date;

import static oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class BotInfoCommand implements ICommand {
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        Date nowDate = new Date();
        long temp = nowDate.getTime() - App.getDate().getTime();
        Date upTime = new Date(temp);
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor cpu = hardwareAbstractionLayer.getProcessor();
        ProcessorIdentifier cpuInfo = cpu.getProcessorIdentifier();

        String day, hour, min, sec;
        try {
            double uptime = (double)operatingSystem.getSystemUptime();
            day = String.valueOf((int)uptime / 60 / 60 / 24);
            hour = String.valueOf((int)uptime / 60 / 60 % 24);
            min = String.valueOf((int)uptime / 60 % 60);
            sec = String.valueOf((int)uptime % 60);
        } catch (Exception e) {
            e.fillInStackTrace();
            return;
        }
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle(event.getJDA().getSelfUser().getName() + "에 대한 정보")
                .addField("봇 버젼", App.getVersion(), true)
                .addField("게이트웨이 핑", event.getJDA().getGatewayPing() + "ms", true)
                .addField("램 사용량", ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024) + "MB", true)
                .addField("빌드 시간", App.getBuild_time(), false)
                .addField("빌드 JDK 버젼", App.getBuild_jdk(), false)
                .addField("CPU 쓰레드", String.valueOf(Runtime.getRuntime().availableProcessors()), true)
                .addField("CPU 사용량", "프로그램: " + String.format("%.2f", osBean.getProcessCpuLoad() * 100.0D) +
                        "% / 전체: " + String.format("%.2f", osBean.getCpuLoad() * 100.0D) + "%", true)
                .addField("업타임", String.format("%s일 %s시간 %s분 %s초",
                        upTime.getTime()/86400000%100, upTime.getTime()/3600000%24, upTime.getTime()/60000%60, upTime.getTime()/1000%60), false)
                .addField("CPU 정보 ", cpuInfo.getName() + "@" + cpuInfo.getVendorFreq() / 1000000L + "MHz", false)
                .addField("OS 정보", operatingSystem.getManufacturer() + " " + operatingSystem.getVersionInfo().getVersion() + " Build" + operatingSystem.getVersionInfo().getBuildNumber(), true)
                .addField("OS 업타임",day + "일 " + hour + "시간 " + min + "분 " + sec + "초", true);

        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    @Override
    public @NotNull String getHelp() {
        return "봇에 대한 정보를 출력합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "봇정보";
    }
}
