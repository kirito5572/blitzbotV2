package me.kirito5572.listener;

import me.duncte123.botcommons.messaging.EmbedUtils;
import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.sticker.Sticker;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateTagsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;


public class LogListener extends ListenerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(LogListener.class);
    private final MySqlConnector mySqlConnector;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

    public LogListener(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
        try {
            mySqlConnector.reConnection();
        } catch (SQLException e) {
            logger.error("SQL reConnection FAIL");
            e.fillInStackTrace();
        }

    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if (!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        boolean isFile = false;

        Message message = event.getMessage();
        if (event.getAuthor().isBot()) {
            return;
        }
        if (message.isWebhookMessage()) {
            return;
        }
        List<Message.Attachment> files = message.getAttachments();
        if (!files.isEmpty()) {
            isFile = true;
        }

        try {
            mySqlConnector.Insert_Query("INSERT INTO blitz_bot.ChattingDataTable (messageId, userId, messageRaw, isFile) VALUES (?, ?, ?, ?)",
                    new int[]{mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.BOOLEAN},
                    new String[]{message.getId(), message.getAuthor().getId(), message.getContentRaw(), String.valueOf(isFile)});
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        if (isFile) {
            int i = 0;
            for (Message.Attachment attachment : files) {
                if (attachment.isImage() || attachment.isVideo()) {
                    i++;
                    String extension = attachment.getFileExtension();
                    String FileName = attachment.getFileName();
                    File file;
                    if(extension != null && FileName.contains(extension)) {
                        file = new File(attachment.getFileName());
                    } else {
                        file = new File(attachment.getFileName() + "." + attachment.getFileExtension());
                    }
                    try {
                        attachment.getProxy().downloadToFile(file);
                    } catch (CancellationException e) {
                        logger.error("로그 파일 다운로드에 실패했습니다.");
                        logger.warn(e.getMessage());
                        return;
                    }
                    try {
                        S3UploadObject(file, message.getId() + "_" + i);
                    } catch (SdkClientException e) {
                        logger.error(e.getMessage());
                        e.fillInStackTrace();
                    }
                    boolean isFileDeleted = file.delete();
                    if (!isFileDeleted) {
                        logger.warn("파일 삭제에 실패하였습니다. 재시도 중입니다.");
                        isFileDeleted = file.delete();
                        if (!isFileDeleted) {
                            logger.error("파일 삭제에 실패하였습니다.");
                        } else {
                            logger.info("파일 삭제에 성공했습니다.");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        if(event.getAuthor().isBot()) {
            return;
        }
        if(event.getMessage().isWebhookMessage()) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date time = new Date();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ChattingDataTable WHERE messageId=?",
                new int[] {mySqlConnector.STRING},
                new String[] {event.getMessageId()})){
            Member member = event.getMember();
            assert member != null;
            embedBuilder.setTitle("수정된 메세지")
                    .setColor(Color.ORANGE)
                    .setDescription("메세지 수정: " + event.getChannel().getAsMention() + "\n[메세지 이동](" + event.getMessage().getJumpUrl() + ")")
                    .addField("작성 채널", event.getChannel().getAsMention(), false);
            if(resultSet.next()) {
                String pastData = resultSet.getString("messageRaw");
                String nowData = event.getMessage().getContentRaw();
                if(pastData != null) {
                    MessageBuilder(embedBuilder, pastData, "수정전 내용", null);
                    MessageBuilder(embedBuilder, nowData,"수정후 내용", event.getMessageId());
                }
            } else {
                embedBuilder.addField("수정전 내용", "정보 없음", false);
                MessageBuilder(embedBuilder, event.getMessage().getContentRaw(),"수정후 내용", null);
            }
            embedBuilder.addField("수정 시간", timeFormat.format(time), false)
                    .setFooter(member.getEffectiveName() + "(" + member.getEffectiveName() + ")", member.getUser().getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendMessageEmbeds(embedBuilder.build()).queue();

            mySqlConnector.Insert_Query("UPDATE blitz_bot.ChattingDataTable SET messageRaw=? WHERE messageId=?",
                    new int[] {mySqlConnector.STRING, mySqlConnector.STRING},
                    new String[] {event.getMessage().getContentRaw(), event.getMessageId()});
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date time = new Date();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ChattingDataTable WHERE messageId=?;",
                new int[] {mySqlConnector.STRING}, new String[] {event.getMessageId()})) {
            embedBuilder.setTitle("삭제된 메세지")
                    .setColor(Color.RED);
            boolean isFile = false;
            if(resultSet.next()) {
                isFile = resultSet.getBoolean("isFile");
                Member member = event.getGuild().getMemberById(resultSet.getString("userId"));
                if(member != null) {
                    embedBuilder.setFooter(member.getEffectiveName() + "(" + member.getEffectiveName() + ")", member.getUser().getAvatarUrl());
                } else {
                    embedBuilder.setFooter("알수 없는 유저");
                }
                if(isFile) {
                    embedBuilder.appendDescription("이미지가 포함된 게시글");
                }
                embedBuilder.addField("작성 채널", event.getChannel().getAsMention(), false);
                String messageRaw = resultSet.getString("messageRaw");
                if(resultSet.getString("messageRaw").isEmpty()) {
                    embedBuilder.addField("삭제된 내용", "내용이 없이 사진만 있는 메세지", false);
                } else {
                    MessageBuilder(embedBuilder, messageRaw,"삭제된 내용", event.getMessageId());
                }
            } else {
                embedBuilder.addField("데이터 없음", "데이터 없음", false);
            }
            File file = null;
            embedBuilder.addField("삭제 시간", timeFormat.format(time), false);
            if(isFile) {
                 file = S3DownloadObject(event.getMessageId() + "_" + 1);
                Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendFiles(FileUpload.fromData(file)).queue();
            }
            Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendMessageEmbeds(embedBuilder.build()).queue();
            mySqlConnector.Insert_Query("DELETE FROM blitz_bot.ChattingDataTable WHERE messageId=?", new int[] {mySqlConnector.STRING}, new String[] {event.getMessageId()});
            if(file !=null) {
                if (file.exists()) {
                    if (!file.delete()) {
                        logger.error("파일 삭제 실패");
                    }
                }
            }

        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy/MM/dd a hh:mm:ss");
        embedBuilder.setTitle("유저 입장")
                .setColor(new Color(50, 200, 50))
                .setDescription(event.getMember().getAsMention() + "유저가 서버에 들어왔습니다.")
                .addField("유저명", event.getMember().getEffectiveName() + "(" + event.getMember().getUser().getName() + ") ", false)
                .addField("입장 시간", simpleDateFormat.format(date), false)
                .addField("유저 가입일", event.getMember().getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                .setFooter(event.getMember().getId(), event.getMember().getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date date = new Date();
        User user = event.getUser();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy/MM/dd a hh:mm:ss");
        embedBuilder.setTitle("유저 퇴장")
                .setDescription(event.getUser().getAsMention() + "유저가 서버에서 나갔습니다.")
                .setColor(Color.RED)
                .addField("유저명", user.getEffectiveName() + "(" + user.getName() + ") ", false)
                .addField("유저 가입일", user.getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                .addField("퇴장 시간", simpleDateFormat.format(date), false)
                .setFooter(user.getId(), user.getAvatarUrl());

        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        List<Role> roleList = event.getRoles();
        StringBuilder roleData = new StringBuilder();
        for (Role role : roleList) {
            roleData.append(role.getAsMention());
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed()
                .setTitle("유저 역할 추가")
                .setColor(Color.GREEN)
                .setDescription("대상 유저:" + member.getAsMention())
                .addField("유저명", member.getEffectiveName() + "(" + member.getUser().getName() + ") ", false)
                .addField("추가된 역할", roleData.toString(), false)
                .addField("시간", timeFormat.format(date), false)
                .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Member member = event.getMember();
        Date date = new Date();
        List<Role> roleList = event.getRoles();
        StringBuilder roleData = new StringBuilder();
        for (Role role : roleList) {
            roleData.append(role.getAsMention());
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed()
                .setTitle("유저 역할 삭제")
                .setColor(Color.RED)
                .setDescription("대상 유저:" + member.getAsMention())
                .addField("유저명", member.getEffectiveName() + "(" + member.getUser().getName() + ") ", false)
                .addField("추가된 역할", roleData.toString(), false)
                .addField("시간", timeFormat.format(date), false)
                .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        Date date = new Date();
        String nickName = event.getOldNickname();
        if (nickName == null) {
            nickName = "없음";
        }
        String newNickName = event.getNewNickname();
        if (newNickName == null) {
            newNickName = "없음";
        }

        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("유저 닉네임 변경")
                .setDescription("대상 유저:" + event.getMember().getAsMention()).setColor(Color.GREEN)
                .addField("이전 이름", nickName, false)
                .addField("현재 이름", newNickName, false)
                .addField("시간", timeFormat.format(date), false);

        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        User user = event.getUser();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 차단 실행")
                .setColor(new Color(200, 50, 50))
                .setDescription(timeFormat.format(date))
                .addField("유저명", user.getName(), false)
                .setFooter(user.getId(), user.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        User user = event.getUser();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 차단 해제")
                .setColor(new Color(50, 200, 50))
                .setDescription(timeFormat.format(date))
                .addField("유저명", user.getName(), false)
                .setFooter(user.getId(), user.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        AudioChannelUnion channelJoined = event.getChannelJoined();
        AudioChannelUnion channelLeft= event.getChannelLeft();

        if(channelJoined != null && channelLeft != null) {
            //채널 이동
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 이동")
                    .setColor(new Color(255, 255, 0))
                    .setDescription(timeFormat.format(date))
                    .addField("이전 채널명", channelLeft.getAsMention(), false)
                    .addField("현재 채널명", channelJoined.getAsMention(), false)
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();

        } else if(channelJoined != null) {
            //채널 입장
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 입장")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("채널명", channelJoined.getAsMention(), false)
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();

        } else if(channelLeft != null) {
            //채널 퇴장
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 퇴장")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("채널명", channelLeft.getAsMention(), false)
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

    @Override
    public void onGuildVoiceGuildMute(@NotNull GuildVoiceGuildMuteEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        if(event.isGuildMuted())
            embedBuilder.setTitle("유저 강제 뮤트")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        else
            embedBuilder.setTitle("유저 강제 뮤트 해제")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildVoiceGuildDeafen(@NotNull GuildVoiceGuildDeafenEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        if(event.isGuildDeafened())
            embedBuilder.setTitle("유저 보이스 서버 음소거")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("유저명",member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        else
            embedBuilder.setTitle("유저 보이스 서버 음소거 해제")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(timeFormat.format(date))
                    .addField("유저명", member.getEffectiveName() + "(" + member.getAsMention() + ")", false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildInviteCreate(@NotNull GuildInviteCreateEvent event) {
        Invite invite = event.getInvite();
        String value = invite.getUrl();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 초대 링크 생성")
                .setColor(Color.GREEN)
                .addField("생성자", (invite.getInviter() != null) ? invite.getInviter().getName() : "알수 없음", false)
                .addField("생성된 URL", value, false)
                .addField("최대 사용 횟수", (invite.getMaxUses() == 0) ? "무제한" : invite.getMaxUses() + "회", false)
                .addField("최대 사용 횟수", (invite.getMaxAge() == 0) ? "무제한" : invite.getMaxUses() + "초", false)
                .addField("변경 시간", timeFormat.format(invite.getTimeCreated()), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onGuildInviteDelete(@NotNull GuildInviteDeleteEvent event) {
        Date date = new Date();
        String value = "discord.gg/" + event.getCode();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 초대 링크 삭제")
                .setColor(Color.RED)
                .addField("삭제된 URL", value, false)
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onGuildStickerAdded(@NotNull GuildStickerAddedEvent event) {
        Guild guild = event.getGuild();
        Date date = new Date();
        Sticker sticker = event.getSticker();
        InputStream input = null;
        try(InputStream value = sticker.getIcon().download().join()) {
            input = value;
        } catch (IOException ignored) {
        }
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 스티커 추가")
                .setColor(Color.RED)
                .addField("변경 시간", timeFormat.format(date), false);
            try {
                Objects.requireNonNull(guild.getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();
                if(input != null) Objects.requireNonNull(guild.getTextChannelById("1284794417407987714")).sendMessage("추가된 스티커").addFiles(FileUpload.fromData(input, sticker.getId())).queue();
            } catch (Exception e) {
                e.fillInStackTrace();
                logger.warn("onGuildStickerAdded WARN");
            }
        try {
            if(input != null) input.close();
        } catch (IOException ignored) {
        }

    }

    @Override
    public void onGuildStickerRemoved(@NotNull GuildStickerRemovedEvent event) {
        Guild guild = event.getGuild();
        Date date = new Date();
        Sticker sticker = event.getSticker();
        InputStream input = null;
        try(InputStream value = sticker.getIcon().download().join()) {
            input = value;
        } catch (IOException ignored) {
        }
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 스티커 삭제")
                .setColor(Color.RED)
                .addField("변경 시간", timeFormat.format(date), false);
        try {
            Objects.requireNonNull(guild.getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();
            if(input != null) Objects.requireNonNull(guild.getTextChannelById("1284794417407987714")).sendMessage("삭제된 스티커").addFiles(FileUpload.fromData(input, sticker.getId())).queue();

        } catch (Exception e) {
            e.fillInStackTrace();
            logger.warn("onGuildStickerRemoved WARN");
        }
        try {
            if(input != null) input.close();
        } catch (IOException ignored) {
        }

    }

    @Override
    public void onGuildStickerUpdateName(@NotNull GuildStickerUpdateNameEvent event) {
        Date date = new Date();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 스티커 이름 변경")
                .setColor(Color.RED)
                .addField("변경 전 이름", oldValue, false)
                .addField("변경 후 이름", newValue, false)
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();

    }

    @Override
    public void onGuildStickerUpdateTags(@NotNull GuildStickerUpdateTagsEvent event) {
        Date date = new Date();
        Set<String> oldValue = event.getOldValue();
        Set<String> newValue = event.getNewValue();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 스티커 태그 변경")
                .setColor(Color.RED)
                .addField("변경 전 태그", oldValue.toString(), false)
                .addField("변경 후 태그", newValue.toString(), false)
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();

    }

    @Override
    public void onEmojiAdded(@NotNull EmojiAddedEvent event) {
        Emoji emote = event.getEmoji();
        Date date = new Date();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 이모지 추가")
                .setColor(Color.GREEN)
                .addField("이모지명", emote.getName(), false)
                .setDescription("[이모지 보기](" + emote.getAsReactionCode() + ")")
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();

    }

    @Override
    public void onEmojiRemoved(@NotNull EmojiRemovedEvent event) {
        Emoji emote = event.getEmoji();
        Date date = new Date();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 이모지 제거").setColor(Color.GREEN)
                .addField("이모지명", emote.getName(), false)
                .setDescription("[이모지 보기](" + emote.getAsReactionCode() + ")")
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();

    }

    @Override
    public void onEmojiUpdateName(@NotNull EmojiUpdateNameEvent event) {
        Emoji emote = event.getEmoji();
        Date date = new Date();
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("서버 이모지 업데이트").setColor(Color.GREEN)
                .addField("이전 이모지명", event.getOldName(), false)
                .addField("변경된 이모지명", event.getNewName(), false)
                .setDescription("[이모지 보기](" + emote.getAsReactionCode() + ")")
                .addField("변경 시간", timeFormat.format(date), false);
        Objects.requireNonNull(event.getGuild().getTextChannelById("1284794417407987714")).sendMessageEmbeds(builder.build()).queue();

    }

    private static final Region clientRegion = Region.AP_NORTHEAST_2;
    private static final String bucketName = "blitzbot-logger";


    /**
     * upload file to bot s3 cloud
     *
     * @param file the {@link File} to upload
     * @param messageId message id of the file to be uploaded
     */

    private void S3UploadObject(@NotNull File file, @NotNull String messageId) {
        try(S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(clientRegion)
                .build()){
            Map<String, String> metadata = new HashMap<>();
            metadata.put("extension", FilenameUtils.getExtension(file.getName()));

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(messageId)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putObjectRequest, file.toPath());
        }
    }

    /**
     * download {@link File} to bot s3 cloud
     *
     * @param messageId Message id of the file to be downloaded
     *
     * @return download {@link File} or null(If the file does not exist)
     */

    private File S3DownloadObject(@NotNull String messageId) {
        try(S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(clientRegion)
                .build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(messageId)
                    .build();
            Path path;
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .key(messageId)
                    .bucket(bucketName)
                    .build();
            String type = s3Client.headObject(objectRequest).contentType();
            path = Paths.get(messageId + "." + type.split("/")[1]);
            s3Client.getObject(getObjectRequest, path);
            return path.toFile();
        }
    }

    /**
     * Message data splitter(if Message data's length > 1000, split it)
     *
     * @param embedBuilder the EmbedBuilder which is use addField with split data
     * @param data the message string to be split
     * @param messageId the messageId which is modifying message's id
     *                  input null if the message which is before modification is null
     * @param name the name
     */

    private void MessageBuilder(@NotNull EmbedBuilder embedBuilder, @NotNull String data, @NotNull String name, @Nullable String messageId) {
        if(1000 < data.length() && data.length() <= 2000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000), false);
        } else if(2000 < data.length() && data.length() <= 3000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000, 2000), false)
                    .addField(name + "-3", data.substring(2000), false);
        } else if(3000 < data.length() && data.length() <= 4000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000, 2000), false)
                    .addField(name + "-3", data.substring(2000, 3000), false)
                    .addField(name + "-4", data.substring(3000), false);
        } else {
            embedBuilder.addField(name, data, false);
        }

        if(messageId != null) {
            embedBuilder.addField("메세지 ID", messageId, false);
        }

    }
}
