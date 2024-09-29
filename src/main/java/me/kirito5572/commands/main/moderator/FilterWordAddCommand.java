package me.kirito5572.commands.main.moderator;

import me.kirito5572.objects.main.FilterSystem;
import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;

public class FilterWordAddCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(FilterWordAddCommand.class);
    private final FilterSystem filterSystem;

    public FilterWordAddCommand(FilterSystem filterSystem) {
        this.filterSystem = filterSystem;
    }
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        if (Objects.requireNonNull(event.getGuild()).getId().equals("826704284003205160")) {
            OptionMapping opt = event.getOption("단어");
            if(opt == null) {
                return;
            }
            boolean isSuccess = filterSystem.commandAuthorityCheck(event);
            if(isSuccess) {
                filterSystem.wordUpdate(false, true, new String[]{opt.getAsString()});
                event.reply("단어 추가가 완료되었습니다.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public @NotNull String getHelp() {
        return "(관리자 전용) 필터링 단어 목록에서 단어를 추가합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "단어추가";
    }
}
