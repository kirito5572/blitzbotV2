package me.kirito5572.objects.main;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public interface ICommand {

    void handle(@NotNull SlashCommandInteractionEvent event);

    String getHelp();

    String getInvoke();

}
