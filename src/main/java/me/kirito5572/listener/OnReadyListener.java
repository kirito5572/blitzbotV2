package me.kirito5572.listener;

import me.kirito5572.App;
import me.kirito5572.objects.main.GoogleAPI;
import me.kirito5572.objects.main.MySqlConnector;
import me.kirito5572.objects.main.OptionData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.AttachmentProxy;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class OnReadyListener extends ListenerAdapter {
    private final MySqlConnector mySqlConnector;
    private final GoogleAPI googleAPI;
    private int i = 0;
    private final Logger logger = LoggerFactory.getLogger(OnReadyListener.class);

    public OnReadyListener(MySqlConnector mySqlConnector, GoogleAPI googleAPI) {
        this.mySqlConnector = mySqlConnector;
        this.googleAPI = googleAPI;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        startUpGetData();
        if(App.appMode == App.APP_ALPHA) {
            Objects.requireNonNull(event.getJDA().getPresence()).setActivity(Activity.watching("알파 테스트에 오신것을 환영합니다."));
        } else if(App.appMode == App.APP_BETA) {
            Objects.requireNonNull(event.getJDA().getPresence()).setActivity(Activity.watching("베타 테스트에 오신것을 환영합니다."));
        } else if(App.appMode == App.APP_STABLE) {
            autoActivityChangeModule(event);
        }
        logger.info("자동화 처리부 시작 완료");
    }

    /**
     * Automatically change activity of bot
     * @param event {@link net.dv8tion.jda.api.events.session.ReadyEvent}
     */

    private void autoActivityChangeModule(@NotNull ReadyEvent event) {
        JDA jda = event.getJDA();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                switch (i) {
                    case 0,2,4 -> Objects.requireNonNull(jda.getPresence()).setActivity(Activity.watching("월드오브탱크블리츠 공식 한국어 디스코드"));
                    case 1 -> Objects.requireNonNull(jda.getPresence()).setActivity(Activity.listening(App.getVersion()));
                    case 3 -> Objects.requireNonNull(jda.getPresence()).setActivity(Activity.playing("버그/개선 사항은 DM 부탁드립니다."));
                    case 5 -> Objects.requireNonNull(jda.getPresence()).setActivity(Activity.streaming("kirito5572#5572 제작","https://github.com/kirito5572"));
                }
                i++;
                if (i > 5) {
                    i = 0;
                }
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 5000);
    }

    private void autoTranslationDetector(ReadyEvent event) {
        final String inputGuildId = "665581943382999048";
        final String outputGuildId = "826704284003205160";

        final String[] inputChannels = new String[] {
                "1039543294306287687", "827542008112480297", "1039543108888711178"
        };
        @SuppressWarnings("unused") final String[] outputChannelsDebugs = new String[] {
                "671515746119188492", "671515746119188492", "671515746119188492"
        };

        final String[] outputChannels = new String[] {
                "827040899174236171", "827040924722397216", "827040988488925185"
        };

        //TODO 디텍터 link 시키고 디버깅해보기
        Guild inputGuild = event.getJDA().getGuildById(inputGuildId);
        Guild outputGuild = event.getJDA().getGuildById(outputGuildId);

        if(inputGuild == null) {
            return;
        }
        if(outputGuild == null) {
            return;
        }
        GuildMessageChannel inputNoticeChannel = inputGuild.getTextChannelById(inputChannels[0]);
        GuildMessageChannel inputGameNewsChannel = inputGuild.getTextChannelById(inputChannels[1]);
        GuildMessageChannel inputWorkOnProgressChannel = inputGuild.getTextChannelById(inputChannels[2]);
        if(inputNoticeChannel == null || inputGameNewsChannel == null || inputWorkOnProgressChannel == null) {
            return;
        }

        GuildMessageChannel outputNoticeChannel = outputGuild.getTextChannelById(outputChannels[0]);
        GuildMessageChannel outputGameNewsChannel = outputGuild.getTextChannelById(outputChannels[1]);
        GuildMessageChannel outputWorkOnProgressChannel = outputGuild.getTextChannelById(outputChannels[2]);
        if(outputNoticeChannel == null || outputGameNewsChannel == null || outputWorkOnProgressChannel == null) {
            return;
        }

        try {
            messageCheckingModule(inputNoticeChannel, outputNoticeChannel);
            messageCheckingModule(inputGameNewsChannel, outputGameNewsChannel);
            messageCheckingModule(inputWorkOnProgressChannel, outputWorkOnProgressChannel);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean isLastMessageModule(String channelId, String messageId) {
        boolean value = false;
        try(ResultSet rs = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.isLastMessage WHERE channelId = ?", new int[]{mySqlConnector.STRING}, new String[] {channelId})) {
            String lastedMessageId =rs.getString("messageId");
            if(lastedMessageId.equals(messageId)) {
                value = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    private void messageCheckingModule(GuildMessageChannel input, GuildMessageChannel output) throws ExecutionException, InterruptedException {
        if(!isLastMessageModule(input.getId(), input.getLatestMessageId())) {
            Message message = input.getIterableHistory()
                    .takeAsync(1)
                    .get().get(0);
            String inputMessage = message.getContentDisplay();
            List<Message.Attachment> attachments = message.getAttachments();
            List<FileUpload> downloadFile = new ArrayList<>();
            for (Message.Attachment attachment : attachments) {
                AttachmentProxy proxy = attachment.getProxy();
                if (attachment.isImage() || attachment.isVideo()) {
                    downloadFile.add(FileUpload.fromData(proxy.download().get(), attachment.getFileName() + "." + attachment.getFileExtension()));
                }
            }

            MessageCreateAction messageCreateAction = null;
            if(inputMessage.length() > 1) {
                String outputMessage = googleAPI.googleTranslateModule(inputMessage, input.getGuild());
                messageCreateAction = output.sendMessage(input.getName() + "\n" + outputMessage);
                messageCreateAction = messageCreateAction.addFiles(downloadFile);
            } else {
                messageCreateAction = output.sendFiles(downloadFile);
            }
            message.delete().queue();
        }
    }

    private void startUpGetData() {
        List<String> complainData = new ArrayList<>();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ComplainBan;", new int[]{}, new String[]{})) {
            while (resultSet.next()) {
                complainData.add(resultSet.getString("userId"));
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        OptionData.setComplainBanUserList(complainData);
    }


}
