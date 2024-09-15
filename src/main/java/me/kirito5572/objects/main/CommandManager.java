package me.kirito5572.objects.main;

import me.kirito5572.commands.main.BotInfoCommand;
import me.kirito5572.commands.main.PingCommand;
import me.kirito5572.commands.main.admin.EvalCommand;
import me.kirito5572.commands.main.moderator.*;
import me.kirito5572.commands.music.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** @noinspection ALL*/
public class CommandManager {
    private final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final MySqlConnector mySqlConnector;
    private final GoogleAPI googleAPI;
    private final JDA jda;
    private final FilterSystem filterSystem;
    private final Map<String, SlashCommandData> commands = new HashMap<>();
    private final Map<String, SlashCommandData> musicCommands = new HashMap<>();
    private final Map<String, ICommand> commandHandle = new HashMap<>();

    public CommandManager(JDA jda, MySqlConnector mySqlConnector, GoogleAPI googleAPI, FilterSystem filterSystem) {
        this.jda = jda;
        this.mySqlConnector = mySqlConnector;
        this.googleAPI = googleAPI;
        this.filterSystem = filterSystem;
        addCommand();
        updateCommand();

    }

    /**
     * addCommand for prefix command system(prefix = !)
     * @param command command data, more: {@link ICommand}
     */

    private void addCommand() {
        ICommand command;

        command = new EvalCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.STRING, "args", "인수"));
        this.commandHandle.put(command.getInvoke(), command);

        command = new BanCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                .addOption(OptionType.USER, "유저", "밴 할 유저", true)
                .addOption(OptionType.STRING, "사유", "밴 하는 사유"));
        this.commandHandle.put(command.getInvoke(), command);

        command = new FilterWordAddCommand(filterSystem);
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                .addOption(OptionType.STRING, "단어", "추가할 단어", true));
        this.commandHandle.put(command.getInvoke(), command);

        command = new FilterWordRemoveCommand(filterSystem);
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                .addOption(OptionType.STRING, "단어", "삭제할 단어", true));
        this.commandHandle.put(command.getInvoke(), command);

        command = new KickCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS))
                .addOption(OptionType.USER, "유저", "킥 할 유저", true)
                .addOption(OptionType.STRING, "사유", "킥 하는 사유"));
        this.commandHandle.put(command.getInvoke(), command);

        command = new MessageBulkDeleteCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                .addOption(OptionType.INTEGER, "수량", "삭제할 수량", true));
        this.commandHandle.put(command.getInvoke(), command);

        command = new PingCommand(mySqlConnector);
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp()));
        this.commandHandle.put(command.getInvoke(), command);

        command = new BotInfoCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp()));
        this.commandHandle.put(command.getInvoke(), command);

        command = new MessagePinCommand(mySqlConnector);
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                .addOption(OptionType.STRING, "메세지", "삭제할 수량", true)
                .addOption(OptionType.ATTACHMENT, "파일", "첨부할 파일"));
        this.commandHandle.put(command.getInvoke(), command);

        command = new UserInfoCommand();
        this.commands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
                .addOption(OptionType.USER, "유저", "검색 할 유저명"));
        this.commandHandle.put(command.getInvoke(), command);

        {
            command = new JoinCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new LeaveCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new NowPlayingCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new PauseCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new PlayCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true)
                    .addOption(OptionType.STRING, "url", "재생할 음악의 URL", true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new QueueCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true)
                    .addOption(OptionType.INTEGER, "재생목록회차", "최대 20개씩 나눠서 출력, 출력할 회차를 입력, 미입력시 제일 앞부터 출력"));
            this.commandHandle.put(command.getInvoke(), command);

            command = new QueueDetectCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true)
                    .addOption(OptionType.INTEGER, "삭제할_수량", "재생목록에서 삭제할 수량, 미입력시 전체 초기화"));
            this.commandHandle.put(command.getInvoke(), command);

            command = new QueueMixCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new SearchCommand(googleAPI);
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true)
                    .addOption(OptionType.STRING, "검색", "검색할 내용", true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new SkipCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new StopClearCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new StopCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true));
            this.commandHandle.put(command.getInvoke(), command);

            command = new VolumeCommand();
            this.musicCommands.put(command.getInvoke(), Commands.slash(command.getInvoke(), command.getHelp())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_CONNECT))
                    .setGuildOnly(true)
                    .addOption(OptionType.INTEGER, "볼륨", "10~100사이로 입력", true));
            this.commandHandle.put(command.getInvoke(), command);
        }
    }

    private void updateCommand() {
        logger.info("명령어 로딩중");
        this.jda.updateCommands().addCommands(commands.values()).complete();
        for(Guild guild : this.jda.getGuilds()) {
            updateCommand(guild.getId());
        }
        logger.info("명령어 셋팅 완료");
    }

    public void updateCommand(String guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        Map<String, SlashCommandData> data = new HashMap<>();
        if(guild != null) {
            guild.updateCommands().addCommands(data.values()).queue();
        }
    }

    /**
     * When input a command, the part to be linked to the command
     * use with slash command system({@link net.dv8tion.jda.api.events.interaction.SlashCommandEvent})
     * @param event {@link net.dv8tion.jda.api.events.interaction.SlashCommandEvent}
     */
    public void handleCommand(SlashCommandInteractionEvent command) {
        if(this.commandHandle.containsKey(command.getName())) {
            this.commandHandle.get(command.getName()).handle(command);
        } else {
            command.getChannel().sendMessage("해당 명령어를 사용할 수 없습니다.").queue();
        }
    }
}
