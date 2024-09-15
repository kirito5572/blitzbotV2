package me.kirito5572.listener;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmoteClickGiveRoleListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(EmoteClickGiveRoleListener.class);
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160"))
            return;
        if(!event.getMessageId().equals("1045683239173496872"))
            return;
        Role role = event.getGuild().getRoleById("1045683935872552980");
        if(role == null)
            return;
        User user = event.getUser();
        if(user == null)
            return;
        event.getGuild().addRoleToMember(UserSnowflake.fromId(user.getId()), role).queue();
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160"))
            return;
        if(!event.getMessageId().equals("1045683239173496872"))
            return;
        Role role = event.getGuild().getRoleById("1045683935872552980");
        if(role == null)
            return;
        User user = event.getUser();
        if(user == null)
            return;
        event.getGuild().removeRoleFromMember(UserSnowflake.fromId(user.getId()), role).queue();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("규칙 동의 처리부 시작 완료");
    }
}
