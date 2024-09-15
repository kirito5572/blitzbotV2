package me.kirito5572.listener;

import me.kirito5572.objects.main.FilterSystem;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FilterListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(FilterListener.class);
    private final FilterSystem filterSystem;

    public FilterListener(FilterSystem filterSystem) {
        this.filterSystem = filterSystem;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        boolean isFilter = filterSystem.filterRefresh();
        if(!isFilter) {
            logger.error("필터 기능부 비 활성화됨");
        }
        boolean isWhiteFilter = filterSystem.whiteFilterRefresh();
        if(!isWhiteFilter) {
            logger.error("명령어 처리 기능부 준비 완료, 화이트리스트 비활성화 됨");
        }
        if(isFilter && isWhiteFilter) {
            logger.info(" 필터 기능부 준비 완료");
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()) {
            return;
        }
        Member member = event.getMember();
        if(member == null) {
            return;
        }
        if(member.getId().equals(event.getJDA().getSelfUser().getId())){
            return;
        }
        if(event.getMessage().isWebhookMessage()) {
            return;
        }
        if(member.getUser().isBot()) {
            return;
        }
        filter_data(member, event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(!event.isFromGuild()) {
            return;
        }
        Member member = event.getMember();
        assert member != null;
        if(member.getId().equals(event.getJDA().getSelfUser().getId())){
            return;
        }
        if(event.getMessage().isWebhookMessage()) {
            return;
        }
        filter_data(member, event.getMessage());
    }

    /**
     *
     * Thread execution to check prohibited words
     * @param member the member who need check he/her message
     * @param message the message which is checked
     *
     */

    private void filter_data(@NotNull Member member, @NotNull Message message) {
        Guild guild = member.getGuild();
        if (guild.getId().equals("826704284003205160")) {
            try {
                String message_raw = message.getContentRaw();
                if (message_raw.length() > 1) {
                    try(ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4 )) {
                        executor.execute(() -> filter(Objects.requireNonNull(member), message));
                    }
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }
    }

    /**
     * The part that checks prohibited words by executing as a thread
     * call from filter_data()
     *
     * @param member the member who need check he/her message
     * @param message the message which is checked
     *
     */

    private void filter(@NotNull Member member, @NotNull Message message) {
        String rawMessage = message.getContentRaw();
        if (member.getUser().isBot()) {
            return;
        }
        rawMessage = rawMessage.trim().replaceAll("\\s+", " ");
        String match = "[^\uAC00-\uD7A30-9a-zA-Z ]";
        rawMessage = rawMessage.replaceAll(match, "");
        String MessageFormatting = rawMessage;
        boolean filter = false;
        boolean filter_continue = false;
        for (String data : filterSystem.getFilterList()) {
            if (MessageFormatting.contains(data)) {
                for(String[] a : filterSystem.getWhiteFilterList()) {
                    if(data.equals(a[0])) {
                        if(MessageFormatting.contains(a[1])) {
                            filter_continue = true;
                            break;
                        }
                    }
                }
                if(filter_continue) {
                    filter_continue = false;
                    continue;
                }
                filter = true;
                rawMessage = rawMessage.replaceAll(data, "(삭제됨)");
            }
        }
        if(filter) {
            if (member.getPermissions().contains(Permission.ADMINISTRATOR) |
                    member.getPermissions().contains(Permission.MESSAGE_MANAGE) |
                    member.getPermissions().contains(Permission.KICK_MEMBERS) |
                    member.getPermissions().contains(Permission.BAN_MEMBERS) |
                    member.getPermissions().contains(Permission.MANAGE_PERMISSIONS) |
                    member.getPermissions().contains(Permission.MANAGE_CHANNEL) |
                    member.getPermissions().contains(Permission.MANAGE_SERVER)) {
                logger.info("특정 등급 이상 권한 부여자가 필터링에 걸리는 단어를 사용하였으나 통과되었습니다.");
                return;
            }
            GuildMessageChannel textchannel = message.getGuildChannel();
            message.getGuildChannel().deleteMessageById(message.getId()).queue();
            textchannel.sendMessage(rawMessage).queue();
            textchannel.sendMessage(member.getAsMention() + ", 금지어 사용에 주의하여주십시오.").queue(message1 -> message1.delete().queueAfter(10, TimeUnit.SECONDS));
        }
    }
}
