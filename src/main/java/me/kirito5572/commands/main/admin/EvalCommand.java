package me.kirito5572.commands.main.admin;

import groovy.lang.GroovyShell;
import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EvalCommand implements ICommand {
    private final @NotNull GroovyShell engine;
    private final @NotNull String imports;
    public EvalCommand() {
        this.engine = new GroovyShell();
        this.imports = """
                import java.io.*;
                import java.lang.*;
                import java.util.*;
                import java.util.Arrays.*;
                import java.util.concurrent.*;
                import java.sql.*;
                import net.dv8tion.jda.api.*;
                import net.dv8tion.jda.api.entities.*;
                import net.dv8tion.jda.api.managers.*;
                import net.dv8tion.jda.api.utils.*;
                import me.duncte123.botcommons.messaging.*;
                import net.dv8tion.jda.api.exceptions.*;
                import net.dv8tion.jda.api.audio.*;
                import net.dv8tion.jda.api.events.*;
                import net.dv8tion.jda.api.events.channel.*;
                import net.dv8tion.jda.api.events.channel.update.*;
                import net.dv8tion.jda.api.events.emoji.update.*;
                import net.dv8tion.jda.api.events.emoji.*;
                import net.dv8tion.jda.api.events.guild.update.*;
                import net.dv8tion.jda.api.events.guild.voice.*;
                import net.dv8tion.jda.api.events.guild.member.*;
                import net.dv8tion.jda.api.events.guild.invite.*;
                import net.dv8tion.jda.api.events.guild.override.*;
                import net.dv8tion.jda.api.events.guild.*;
                import net.dv8tion.jda.api.events.http.*;
                import net.dv8tion.jda.api.events.message.*;
                import net.dv8tion.jda.api.events.message.react.*;
                import net.dv8tion.jda.api.events.role.*;
                import net.dv8tion.jda.api.events.role.update.*;
                import net.dv8tion.jda.api.events.self.*;
                import net.dv8tion.jda.api.events.role.update.*;
                import net.dv8tion.jda.api.events.*;
                import net.dv8tion.jda.api.managers.*;
                import me.duncte123.botcommons.*;
                import me.duncte123.botcommons.text.*;
                import me.duncte123.botcommons.commands.*;
                import me.duncte123.botcommons.config.*;
                import me.duncte123.botcommons.messaging.*;
                import me.duncte123.botcommons.web.*;
                import com.google.gson.*;
                import me.kirito5572.commands.*;
                import me.kirito5572.commands.logger.*;
                import me.kirito5572.commands.main.*;
                import me.kirito5572.commands.music.*;
                import me.kirito5572.listeners.main.*;
                import me.kirito5572.listeners.logger.*;
                import me.kirito5572.listeners.*;
                import me.kirito5572.objects.*;
                import me.kirito5572.*;
                """;
    }
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        if(!Objects.requireNonNull(event.getMember()).getId().equals("284508374924787713")) {
            return;
        }
        String command = event.getOption("실행코드", OptionMapping::getAsString);
        if (command == null) {
            event.reply("입력된 명령어 없음").setEphemeral(true).queue();
            return;
        }
        if(command.contains("jda.shutdown()")) {
            event.reply("봇이 종료됩니다.").setEphemeral(true).queue();
            System.exit(0);
        }
        try {
            engine.setProperty("event", event);
            engine.setProperty("channel", event.getChannel());
            engine.setProperty("jda", event.getJDA());
            engine.setProperty("guild", event.getGuild());
            engine.setProperty("member", event.getMember());

            String script = imports + command.split("\\s+", 2)[0];
            Object out = engine.evaluate(script);

            event.reply(out == null ? "에러 없이 실행이 완료되었습니다." : out.toString()).setEphemeral(true).queue();
        } catch (Exception e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
            e.fillInStackTrace();
        }
    }

    @NotNull
    @Override
    public String getHelp() {
        return "개발용 실행형 명령어";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "eval";
    }

}
